# Nudge

A simple reminder and alarm web app with an optional AI reminder creator powered by Google Gemini.

## Run locally on Windows

### 1. Install prerequisites

- JDK 21 or newer
- Node.js 18 or newer
- A Google Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey)

### 2. Add your own Gemini API key

From the project root, copy `.env.example` and rename the copy to `.env`:

```powershell
copy .env.example .env
```

Open `.env` and replace the placeholder:

```env
GEMINI_API_KEY=your_own_gemini_api_key
GEMINI_MODEL=gemini-3.6-flash
```

**Never commit `.env` or share your API key.** The `.gitignore` file already excludes it.

### 3. Start the backend

Double-click:

```text
runbackend.bat
```

The Spring Boot backend runs at `http://localhost:8080`.

### 4. Start the frontend

Double-click:

```text
runfrontend.bat
```

Open the Vite URL shown in the terminal, usually `http://localhost:5173`.

The first frontend run automatically installs npm dependencies.

## GitHub setup

```powershell
git init
git add .
git commit -m "Initial Nudge project"
git branch -M main
git remote add origin YOUR_GITHUB_REPOSITORY_URL
git push -u origin main
```

Each person who clones the repository must create their own local `.env` file and use their own Gemini API key.

## Notes

- Gemini usage is subject to Google's current API limits and quotas.
- The backend loads `.env` when started through `runbackend.bat`.
- `start-backend.bat` and `start-frontend.bat` are kept as compatibility aliases.
