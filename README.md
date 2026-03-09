# Cloud-Based Ticket Reservation Application

The Cloud-Based Ticket Reservation Application is a ticket booking system developed for SOEN 345. It allows users to browse events such as movies, concerts, travel, and sports, reserve tickets, cancel bookings, and receive digital confirmations via email or SMS.

The system supports both customers and event administrators. Customers can search and filter events, while administrators can manage the events by adding, editing, or cancelling them.

Built in Java and designed for cloud deployment, the application supports concurrent users, prevents overbooking, and provides a simple and user-friendly experience.

## Prerequisites

- Android Studio (latest stable)
- JDK 17 (recommended for Gradle/CI parity)
- Android SDK + emulator image (API 34+ recommended)
- Firebase project access (for Auth + Firestore)
- Resend account + API key

## 2. Configure `.env` for email confirmations

From project root:

```bash
cp .env.example .env
```

Set values in `.env`:

```env
RESEND_API_KEY=re_xxxxxxxxxxxxxxxxxxxxxxxxx
RESEND_FROM_EMAIL=<onboarding@resend.dev>
```

## 3. Run tests

```bash
./gradlew testDebugUnitTest
```

## 4. Run app on emulator

1. Open project in Android Studio.
2. Open **Device Manager** and start an emulator.
3. Click **Run** on `app` configuration.

## 6. Useful commands

```bash
./gradlew clean
./gradlew lintDebug
./gradlew assembleDebug
```

## Team

| Name                    | ID        | GitHub Username |
|-------------------------|-----------|----------------|
| Abdulah Ghulam Ali      | 40281857  | [@CallMeAbdu](https://github.com/CallMeAbdu) |
| Andy Cai                | 40282940  | [@NDC0DE](https://github.com/NDC0DE) |
| Huu Khoa Kevin Tran     | 40283037  | [@hkevint](https://github.com/hkevint) |
| Sofia Cimon             | 40282210  | [@sofiacimon](https://github.com/sofiacimon) |
| Thi Hong Mai Nguyen     | 40248343  | [@miiyao7](https://github.com/miiyao7) |
