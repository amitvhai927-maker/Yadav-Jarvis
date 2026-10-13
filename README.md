# Yadav AI — Android JARVIS starter

A Kotlin Android voice assistant starter project designed to be built from an Android phone using GitHub Actions.

## Included
- JARVIS-style dark UI
- Speech-to-text using Android SpeechRecognizer
- Text-to-speech replies
- Microphone-button activation
- Foreground-only wake phrase prototype: "Hey Jarvis"
- Local commands: open YouTube, WhatsApp, Chrome, Settings; tell time
- Optional AI answers through a Node.js backend
- GitHub Actions APK build workflow

## Important limitations
1. This is a starter/MVP, not the movie JARVIS. Android does not permit unrestricted control of every app or setting.
2. The wake-word feature uses repeated Android speech-recognition sessions while the app is visible. It is not a dedicated always-on, offline wake-word engine. SpeechRecognizer is not designed for continuous listening, can consume battery/data, and device recognition support varies. Turn wake listening off when not using it.
3. The AI backend URL is a placeholder until you deploy the backend. Without that, local commands and speech still work, but AI answers won't.
4. Keep your AI API key on the backend only. Never put it in Android code, GitHub, or the APK.
5. AI API usage may cost money. Set usage limits/budget alerts with your provider and do not share the key.

## Part A — Create the GitHub repository on your phone
1. Open https://github.com and sign in.
2. Create a repository named `yadav-ai-jarvis`. Public or private is okay.
3. Upload the contents of this ZIP to the repository root. On GitHub mobile web, use Add file / Upload files; you can also use a Git client app.
4. Ensure the `.github/workflows/android.yml` path is preserved.

## Part B — Deploy the backend (optional, for AI answers)
The backend folder is separate from the Android app.
1. Create a second GitHub repository named `yadav-ai-backend`.
2. Upload the contents of the `backend/` folder to that repository's root (not the folder itself).
3. Create a Node web service on a hosting provider that supports Node.js. Connect that repository.
4. Set build command to `npm install` and start command to `npm start`.
5. Add environment variables in the hosting dashboard:
   - `OPENAI_API_KEY` = your secret API key
   - `AI_MODEL` = `gpt-4.1-mini` (or a model available to your API account)
   - `APP_TOKEN` = a long random secret
6. Copy the deployed HTTPS URL, for example `https://your-service.example.com`.
7. In `app/src/main/java/com/yadav/ai/MainActivity.kt`, replace:
   `https://YOUR-BACKEND.example.com/chat`
   with your deployed URL plus `/chat`, and replace `CHANGE_ME_APP_TOKEN` with the same `APP_TOKEN` value.
8. Commit that change. For a public repository, putting a static app token in the app is only a basic speed bump, not strong authentication; the backend also rate-limits requests. For a private personal prototype, keep the repo private. For a public release, add real user authentication and stronger abuse controls.
9. Never add `OPENAI_API_KEY` to the Android project.

## Part C — Build APK using GitHub Actions
1. Open the repository on GitHub.
2. Select the Actions tab.
3. If prompted, enable workflows.
4. Open `Build Android APK` and tap `Run workflow`, or push/commit a change to trigger it.
5. Wait for the run to finish with a green check.
6. Open the completed workflow run, find Artifacts, and download `yadav-ai-debug-apk`.
7. Extract the ZIP and install `app-debug.apk`. Android may ask you to allow installation from your browser/files app. Only install APKs you built and trust.

## Part D — First run
1. Open Yadav AI and grant microphone permission.
2. Tap `TALK` and say “What time is it?” or “Open YouTube.”
3. Tap `WAKE WORD` to enable the foreground prototype. Say “Hey Jarvis, open YouTube” or “Hey Jarvis” followed by a second command.
4. For AI answers, deploy the backend and update the URL/token first.
5. Speech recognition may use the device's configured recognition service and may send audio to that service. Check your Android speech service/privacy settings.

## Folder structure
- `app/` Android app source
- `backend/` Node.js AI proxy backend
- `.github/workflows/android.yml` APK build workflow

## Troubleshooting
- `Speech recognition unavailable`: update/enable the Google speech service or install a compatible speech-recognition service; check microphone permission.
- `AI backend not configured`: set the deployed backend URL in MainActivity and rebuild.
- `401 from backend`: app token does not match `APP_TOKEN` on the server.
- `429`: backend rate limit reached.
- APK build fails: open the Actions log and inspect the first Gradle error; check the Gradle/Java versions and that every file path was uploaded correctly.
