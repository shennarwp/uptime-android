# <img src="docs/uptime-icon.svg" alt="" width="40" height="40" /> Uptime Android

Native Android client for the existing self-hosted Uptime Monitor backend. The app is Kotlin and Jetpack Compose, with a small manual dependency container. It does not run checks on the phone: the server remains the source of monitoring data.

## Requirements

- Android Studio with support for Android Gradle Plugin 9.4.1.
- JDK 17 or a newer JDK supported by the installed Android Gradle Plugin.
- Android SDK Platform 37 and the SDK build tools installed through Android Studio.
- A connected Android device or emulator running Android 8.0 (API 26) or newer.
- No system Gradle installation is required; use the checked-in Gradle wrapper.

## Current scope

- Dashboard of targets, current status, recent history bars, and TLS certificate expiry data.
- API-token login against `POST /api/v1/auth/verify`.
- Add, edit, and delete targets through the existing API.
- Incident view for reviewing and marking server incidents as read.
- WorkManager polling about every 15 minutes with local notifications for down checks, recovery, and certificate expiry.
- Server URL and notification/worker settings stored locally. The API token is encrypted with an Android Keystore AES-GCM key.
- No Room, Hilt, Firebase, FCM, ntfy, React Native, PWA, offline database, or Play Store packaging.

## API contract

The client is configured for the Swagger contract currently in the sibling `uptime` repository:

| Method   | Path                  | Auth         |
|----------|-----------------------|--------------|
| `GET`    | `/api/v1/targets`     | no           |
| `POST`   | `/api/v1/auth/verify` | bearer token |
| `POST`   | `/api/v1/targets`     | bearer token |
| `PUT`    | `/api/v1/target/{id}` | bearer token |
| `DELETE` | `/api/v1/target/{id}` | bearer token |

The Settings screen accepts either the server origin (`https://monitor.example`) or a complete `/api/v1` URL and normalizes it. The backend currently parses six-field cron expressions (seconds first), including descriptors such as `@hourly`; the client validates that shape before submission.

## Open in Android Studio

1. Open this directory as a Gradle project.
2. Use a JDK supported by the installed Android Gradle Plugin and install the Android SDK matching `compileSdk` in `app/build.gradle.kts`.
3. Run the app, enter the HTTPS server URL in Settings, then verify the API token in Login.
4. Keep Android battery usage unrestricted if timely 15-minute polling matters. Android may still defer periodic work.

The token is excluded from Android Auto Backup. A biometric lock is deliberately left as a follow-up because the token encryption and the app lock are separate concerns.

## Notifications

The app schedules `AlertWorker` with WorkManager about every 15 minutes. Each run fetches the current targets and their checks from the server, then stores the run timestamp and the last known state locally. The first successful run establishes that baseline and does not notify about older failures.

On later runs, the worker looks at checks newer than the previous run and can post these local notifications:

- **Down** when a new check reports that a target is unavailable. The notification includes the HTTP status or error when the server provides one.
- **Recovered** when a target that was previously down is up again.
- **Certificate expiry** when a certificate has 30 or fewer days remaining. Reminders are bucketed so the worker does not send the same reminder repeatedly during one period; certificates with 10 or fewer days remaining are reminded daily, and expired certificates are reported separately.

Notifications use the `Uptime alerts` Android channel. Android 13 and newer require the app's notification permission; Android can also defer periodic work when battery usage is restricted, so unrestricted battery usage is recommended for timely alerts.

Each notification has a target and incident-type tag, allowing down, recovery, and certificate alerts for the same target to be handled independently. Tapping a notification opens the Incident view focused on that target and incident type. The **Mark as read** action dismisses the system notification immediately, then queues a background request that marks the matching server incident as read.

## Windows and Linux development

The repository keeps text files as LF line endings across Windows, WSL, and Linux. `.gitattributes` and `.editorconfig` set this for Git and supported editors; Windows batch scripts use CRLF. Use `gradlew.bat` on Windows or `./gradlew` on Linux/WSL. Each machine should generate its own ignored `local.properties` for the Android SDK path.

## Secret scanning

Enable the tracked Git hooks once per checkout:

```bash
./scripts/install-git-hooks.sh
```

The script uses [Lefthook](https://lefthook.dev/), a standalone native binary; no `package.json`, Node.js, or npm is required. Install Lefthook and Gitleaks in WSL, then run the script. The configured pre-commit and pre-push checks run Gitleaks, Android lint, and the JVM unit tests. Git for Windows clients such as Sublime Merge are supported: the tracked hooks automatically bridge into WSL, so commits and pushes from Windows use the same checks.

## Project layout

- `app/src/main/java/com/rwpiri/uptime/data/` - API models, Retrofit contract, encrypted token/settings stores, and repository.
- `app/src/main/java/com/rwpiri/uptime/MainActivity.kt` - Compose dashboard and edit flows.
- `app/src/main/java/com/rwpiri/uptime/AlertWorker.kt` - periodic alert evaluation and notification channel.
- `FIXME.md` - implementation and server-verification backlog.

## About

Uptime Android is an independently maintained mobile client for the self-hosted Uptime Monitor service. Monitoring and alert evaluation remain on the server; the Android app provides a dashboard, target management, and local notifications. See `FIXME.md` for remaining work and deployment assumptions.
