package com.nudge.security;

import com.nudge.entity.User;
import com.nudge.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Teaches Spring Security how to look up a Nudge user by email.
 *
 * <p>It is used by {@code DaoAuthenticationProvider} during login: Spring loads
 * the stored BCrypt hash through this class and compares the submitted password
 * against it, so the comparison logic never has to be hand-written.</p>
 */
@Service
public class NudgeUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public NudgeUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account for email " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
