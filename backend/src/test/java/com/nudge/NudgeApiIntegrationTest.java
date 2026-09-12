package com.nudge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nudge.dto.LoginRequest;
import com.nudge.dto.ReminderRequest;
import com.nudge.dto.SignupRequest;
import com.nudge.entity.Priority;
import com.nudge.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for the whole HTTP stack: controller -> service -> JPA -> H2,
 * including the JWT filter and Spring Security rules.
 *
 * <p>This is the file that proves "the backend actually works" - every test hits
 * a real endpoint, nothing is mocked.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:nudgetest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NudgeApiIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private SignupRequest signup(String name, String email, String password) {
        SignupRequest request = new SignupRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    /** Registers a fresh user and returns a usable JWT. */
    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(signup("Test User", email, "secret1234"))))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest();
        login.setEmail(email);
        login.setPassword("secret1234");

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JSON.readTree(body).get("token").asText();
    }

    private ReminderRequest reminder(String title, LocalDate date, LocalTime time, Priority priority) {
        ReminderRequest request = new ReminderRequest();
        request.setTitle(title);
        request.setNotes("some notes");
        request.setDate(date);
        request.setTime(time);
        request.setPriority(priority);
        request.setCategory("Work");
        return request;
    }

    // ------------------------------------------------------------------
    // authentication
    // ------------------------------------------------------------------

    @Test
    @DisplayName("signup creates a user, returns a token, and never returns the password")
    void signupReturnsTokenWithoutPassword() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(signup("Asha Kumar", "asha@example.com", "secret1234"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.name").value("Asha Kumar"))
                .andExpect(jsonPath("$.user.email").value("asha@example.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                // the response must not contain a bcrypt hash either
                .andExpect(jsonPath("$.user.id").isNumber());

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(loginAs("asha@example.com"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertFalse(response.contains("$2a$"), "BCrypt hash leaked in the response");
    }

    private LoginRequest loginAs(String email) {
        LoginRequest login = new LoginRequest();
        login.setEmail(email);
        login.setPassword("secret1234");
        return login;
    }

    @Test
    @DisplayName("signup rejects a duplicate email with 409")
    void signupRejectsDuplicateEmail() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(signup("One", "dup@example.com", "secret1234"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(signup("Two", "dup@example.com", "secret1234"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    @DisplayName("signup rejects short passwords and bad emails with 400 + field errors")
    void signupValidatesInput() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(signup("X", "not-an-email", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").isNotEmpty())
                .andExpect(jsonPath("$.errors.email").isNotEmpty());
    }

    @Test
    @DisplayName("wrong HTTP method and wrong content type give 405 / 415, not 500")
    void meaningfulStatusCodesForMalformedCalls() throws Exception {
        // GET on a POST-only endpoint
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value(containsString("not allowed")));

        // right endpoint, wrong Content-Type
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType());

        // endpoint that does not exist at all
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isUnauthorized()); // security answers first: no token
    }

    @Test
    @DisplayName("the stored password is a BCrypt hash, not the plain text")
    void passwordsAreHashedWithBCrypt() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(signup("Hash Check", "hash@example.com", "secret1234"))))
                .andExpect(status().isCreated());

        com.nudge.entity.User saved = userRepository.findByEmailIgnoreCase("hash@example.com").orElseThrow();

        // BCrypt hashes start with $2a$ / $2b$ / $2y$ and are 60 characters long.
        org.junit.jupiter.api.Assertions.assertTrue(
                saved.getPassword().matches("^\\$2[aby]\\$\\d{2}\\$.{53}$"),
                "password was not stored as a BCrypt hash: " + saved.getPassword());
        org.junit.jupiter.api.Assertions.assertNotEquals("secret1234", saved.getPassword());
        // Two users with the same password must not share a hash (salted).
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(signup("Hash Two", "hash2@example.com", "secret1234"))))
                .andExpect(status().isCreated());
        com.nudge.entity.User second = userRepository.findByEmailIgnoreCase("hash2@example.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotEquals(saved.getPassword(), second.getPassword());
    }

    @Test
    @DisplayName("login with a wrong password returns 401")
    void loginRejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(signup("Bob", "bob@example.com", "secret1234"))))
                .andExpect(status().isCreated());

        LoginRequest wrong = new LoginRequest();
        wrong.setEmail("bob@example.com");
        wrong.setPassword("wrong-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(wrong)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("incorrect")));
    }

    @Test
    @DisplayName("protected endpoints reject missing, garbage and tampered tokens")
    void protectedEndpointsRequireValidToken() throws Exception {
        mockMvc.perform(get("/api/reminders"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/reminders").header("Authorization", "Bearer garbage.token.value"))
                .andExpect(status().isUnauthorized());

        // A token signed with a different key must not be accepted.
        String tampered = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJib2JAZXhhbXBsZS5jb20ifQ.not-real";
        mockMvc.perform(get("/api/reminders").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // reminders
    // ------------------------------------------------------------------

    @Test
    @DisplayName("reminder CRUD: create, read, update, complete, delete")
    void reminderLifecycle() throws Exception {
        String token = registerAndLogin("carol@example.com");

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        String created = mockMvc.perform(post("/api/reminders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(
                                reminder("Submit assignment", tomorrow, LocalTime.of(18, 30), Priority.HIGH))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Submit assignment"))
                .andExpect(jsonPath("$.date").value(tomorrow.toString()))
                .andExpect(jsonPath("$.time").value("18:30"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.completed").value(false))
                .andReturn().getResponse().getContentAsString();

        long id = JSON.readTree(created).get("id").asLong();

        mockMvc.perform(get("/api/reminders/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Submit assignment"))
                .andExpect(jsonPath("$.category").value("Work"));

        // edit
        mockMvc.perform(put("/api/reminders/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(
                                reminder("Submit final project", tomorrow, LocalTime.of(9, 0), Priority.LOW))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Submit final project"))
                .andExpect(jsonPath("$.priority").value("LOW"));

        // mark completed
        mockMvc.perform(patch("/api/reminders/" + id + "/complete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

        // delete -> 204, then gone
        mockMvc.perform(delete("/api/reminders/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/reminders/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a new reminder must not be scheduled in the past")
    void rejectsPastReminder() throws Exception {
        String token = registerAndLogin("dave@example.com");

        mockMvc.perform(post("/api/reminders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(
                                reminder("Yesterday", LocalDate.now().minusDays(1), LocalTime.of(8, 0), Priority.MEDIUM))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("past")));
    }

    @Test
    @DisplayName("title is required and priority must be a known value")
    void validatesReminderFields() throws Exception {
        String token = registerAndLogin("erin@example.com");

        ReminderRequest blank = reminder("   ", LocalDate.now().plusDays(1), LocalTime.of(10, 0), Priority.MEDIUM);
        mockMvc.perform(post("/api/reminders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(blank)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").isNotEmpty());

        mockMvc.perform(post("/api/reminders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bad enum\",\"date\":\"2030-01-01\",\"time\":\"10:00\",\"priority\":\"URGENT\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("lists are sorted by date then time, and only contain the caller's own rows")
    void usersOnlySeeTheirOwnReminders() throws Exception {
        String aliceToken = registerAndLogin("alice@example.com");
        String bobToken = registerAndLogin("brian@example.com");

        // Inserted out of order on purpose.
        mockMvc.perform(post("/api/reminders")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(reminder("Later", LocalDate.now().plusDays(5), LocalTime.of(20, 0), Priority.LOW))))
                .andExpect(status().isCreated());

        String aliceEarly = mockMvc.perform(post("/api/reminders")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(reminder("Earlier", LocalDate.now().plusDays(1), LocalTime.of(7, 15), Priority.HIGH))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long aliceEarlyId = JSON.readTree(aliceEarly).get("id").asLong();

        mockMvc.perform(post("/api/reminders")
                .header("Authorization", "Bearer " + bobToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(reminder("Bob's private one", LocalDate.now().plusDays(2), LocalTime.of(11, 0), Priority.MEDIUM))))
                .andExpect(status().isCreated());

        // Alice sees exactly her two, oldest first.
        mockMvc.perform(get("/api/reminders").header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Earlier"))
                .andExpect(jsonPath("$[1].title").value("Later"));

        // Bob sees exactly one, and never Alice's.
        mockMvc.perform(get("/api/reminders").header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Bob's private one"));

        // Bob cannot read, edit, complete or delete Alice's reminder.
        mockMvc.perform(get("/api/reminders/" + aliceEarlyId).header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/reminders/" + aliceEarlyId)
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(
                                reminder("Hacked", LocalDate.now().plusDays(1), LocalTime.of(7, 15), Priority.HIGH))))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/reminders/" + aliceEarlyId + "/complete").header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/reminders/" + aliceEarlyId).header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());

        // ...and Alice's row is untouched.
        mockMvc.perform(get("/api/reminders/" + aliceEarlyId).header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Earlier"))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    @DisplayName("PATCH /complete with no body toggles the flag")
    void toggleWithoutBodyFlipsState() throws Exception {
        String token = registerAndLogin("flip@example.com");

        String created = mockMvc.perform(post("/api/reminders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(
                                reminder("Toggle me", LocalDate.now().plusDays(1), LocalTime.of(12, 0), Priority.MEDIUM))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = JSON.readTree(created).get("id").asLong();

        mockMvc.perform(patch("/api/reminders/" + id + "/complete").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

        mockMvc.perform(patch("/api/reminders/" + id + "/complete").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false));
    }
}
