# Cloud-Based Ticket Reservation Application

The Cloud-Based Ticket Reservation Application is a ticket booking system developed for SOEN 345. It allows users to browse events such as movies, concerts, travel, and sports, reserve tickets, cancel bookings, and receive digital confirmation emails.

The system supports both customers and event administrators. Customers can search and filter events, while administrators can manage the events by adding, editing, or cancelling them.

Built in Java and designed for cloud deployment, the application supports concurrent users, prevents overbooking, and provides a simple and user-friendly experience.

## Prerequisites

- Android Studio (latest stable)
- JDK 17 (recommended for Gradle/CI parity)
- Android SDK + emulator image (API 34+ recommended)
- Firebase project access (for Auth + Firestore)
- Gmail account with 2FA enabled and an App Password
- Python 3 (for local emulator mail relay)

## 1. Configure `.env` for relay + email

From project root:

```bash
cp .env.example .env
```

Set values in `.env`:

```env
EMAIL_USER=yourdemoaccount@gmail.com
EMAIL_APP_PASS=your16charapppassword
MAIL_RELAY_HOST=127.0.0.1
MAIL_RELAY_PORT=8080
MAIL_RELAY_BASE_URL=http://10.0.2.2:8080
```

## 2. Start the host mail relay

```bash
python3 tools/mail_relay_server.py
```

Keep this terminal running while using the app on emulator.

## 3. Verify relay is reachable

```bash
curl http://127.0.0.1:8080/health
```

Expected:
```json
{"status":"ok"}
```

## 4. Run app on emulator

1. Build + install:
   ```bash
   ./gradlew clean installDebug
   ```
2. Open project in Android Studio.
3. Open **Device Manager** and start an emulator.
4. Click **Run** on `app` configuration or run:
   ```bash
   adb shell am start -n com.soen345.project/.MainActivity
   ```

## 6. Troubleshooting

- `Cleartext HTTP traffic ... not permitted`: reinstall latest debug build (`./gradlew clean installDebug`).
- `Failed to relay booking confirmation`: relay not running or wrong `MAIL_RELAY_BASE_URL`.
- Relay returns `smtp_failed`: verify Gmail 2FA + App Password and no spaces in `EMAIL_APP_PASS`.
- Emulator only: always use `MAIL_RELAY_BASE_URL=http://10.0.2.2:8080`.

## 7. Useful commands

```bash
./gradlew clean
./gradlew lintDebug
./gradlew assembleDebug
./gradlew installDebug
./gradlew testDebugUnitTest
curl http://127.0.0.1:8080/health
```

## Team

| Name                    | ID        | GitHub Username |
|-------------------------|-----------|----------------|
| Abdulah Ghulam Ali      | 40281857  | [@CallMeAbdu](https://github.com/CallMeAbdu) |
| Andy Cai                | 40282940  | [@NDC0DE](https://github.com/NDC0DE) |
| Huu Khoa Kevin Tran     | 40283037  | [@hkevint](https://github.com/hkevint) |
| Sofia Cimon             | 40282210  | [@sofiacimon](https://github.com/sofiacimon) |
| Thi Hong Mai Nguyen     | 40248343  | [@miiyao7](https://github.com/miiyao7) |
