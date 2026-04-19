# F1 Hub 🏎️

A Spring Boot web app that reports on Formula 1 news, recent race results, driver standings, and predictions for upcoming races — with web push notifications 1 day before every race.

## Prompts

These are the prompts used to generate this project with [Claude Code](https://claude.ai/code):

---

**Prompt 1**

> Build a spring boot app which reports on formula 1 news. Including recent results and predictions for upcoming races. Allow the user to receive web notifications 1 day in advance of a upcoming Race. The app should run on render and needs a dockerfile.

**Prompt 2**

> Add prompts to readme

---

## Features

- **Live F1 Data** — race schedule, recent results, and driver standings via the [Jolpica F1 API](https://api.jolpi.ca/ergast/f1/)
- **Race Predictions** — algorithm combining 60% championship standings + 40% recent form (last 3 races)
- **Web Push Notifications** — subscribe in-browser; a daily scheduler sends a push notification 1 day before each race
- **Countdown Timer** — live countdown to the next race
- **REST API** — JSON endpoints for schedule, results, standings, and predictions
- **Render-ready** — multi-stage Dockerfile and `render.yaml` included

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.2, Spring WebFlux (WebClient) |
| Frontend | Thymeleaf, vanilla JS, Service Worker |
| Push Notifications | VAPID via `nl.martijndwars:web-push` + BouncyCastle |
| Database | H2 (default) / PostgreSQL (via env var) |
| Deployment | Docker (multi-stage), Render |

## Project Structure

```
src/main/java/com/f1news/
├── config/         AppConfig.java          # WebClient, VAPID PushService beans
├── controller/     HomeController.java      # Renders the Thymeleaf page
│                   ApiController.java       # REST: /api/f1/*
│                   NotificationController.java  # REST: /api/notifications/*
├── model/          PushSubscriptionEntity.java  # JPA entity (stored subscriptions)
│   └── dto/        RaceDto, RaceResultDto, DriverStandingDto,
│                   RacePredictionDto, PushSubscriptionRequest
├── repository/     PushSubscriptionRepository.java
└── service/        F1DataService.java       # All Jolpica API calls
                    PredictionService.java   # Prediction algorithm
                    WebPushService.java      # Subscribe / send push
                    RaceNotificationScheduler.java  # Daily cron @ 09:00 UTC

src/main/resources/
├── templates/index.html        # Single-page Thymeleaf UI
└── static/
    ├── css/style.css
    └── js/app.js               # Countdown, notification subscription
        js/sw.js                # Service worker (push + offline cache)
```

## Running Locally

```bash
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). The app uses H2 in-memory by default — no database setup needed.

> **Note:** VAPID keys are auto-generated at startup if not set (ephemeral — subscriptions won't survive a restart). For persistence, set the env vars below.

## REST API

| Method | Path | Description |
|---|---|---|
| GET | `/api/f1/schedule` | Full season schedule |
| GET | `/api/f1/next-race` | Next upcoming race |
| GET | `/api/f1/results?count=5` | Recent race results |
| GET | `/api/f1/standings/drivers` | Driver championship |
| GET | `/api/f1/standings/constructors` | Constructor championship |
| GET | `/api/f1/predictions` | Predicted top 10 for next race |
| GET | `/api/notifications/vapid-public-key` | VAPID public key |
| POST | `/api/notifications/subscribe` | Register a push subscription |
| POST | `/api/notifications/unsubscribe` | Remove a push subscription |

## Deploying to Render

1. Push to GitHub and connect the repo in Render
2. Render will auto-detect the `Dockerfile`; use `render.yaml` for one-click deploy
3. Set the following environment variables in the Render dashboard:

| Variable | Description |
|---|---|
| `VAPID_PUBLIC_KEY` | Base64url-encoded VAPID public key |
| `VAPID_PRIVATE_KEY` | Base64url-encoded VAPID private key |
| `VAPID_SUBJECT` | `mailto:you@example.com` |

Generate a VAPID key pair at [vapidkeys.com](https://vapidkeys.com/).

### Optional — PostgreSQL (persistent subscriptions)

Add a Render Postgres service, then set:

| Variable | Value |
|---|---|
| `DATABASE_URL` | Render connection string |
| `DB_DRIVER` | `org.postgresql.Driver` |
| `JPA_DIALECT` | `org.hibernate.dialect.PostgreSQLDialect` |
| `H2_CONSOLE` | `false` |

## Data Source

Race data is provided by the [Jolpica F1 API](https://api.jolpi.ca/ergast/f1/) — the community-maintained successor to the Ergast F1 API.
