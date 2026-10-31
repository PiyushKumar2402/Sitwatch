# SiteWatch: Website Availability and Performance Monitoring

SiteWatch lets users register websites and checks them automatically. It records whether each site is
up, how fast it responds, and when it goes down, then shows the results on a live dashboard.
The project is also a complete **DevOps pipeline**: Git, GitHub, Maven, JUnit, Jenkins (Freestyle and
Pipeline), GitHub webhooks, Docker and Docker Compose.

## Features

| Module | What it does |
|---|---|
| User authentication | Register and sign in; passwords stored as BCrypt hashes; every user sees only their own sites |
| Website management | Add, edit, pause/resume and delete sites; choose the check interval (30 s to 24 h) |
| Health checker | HTTP GET, status-code check, timeout and connection-error detection |
| Performance monitoring | Response time per check, 24 h average, live response-time chart |
| Alert management | Incident opens when a site goes down and closes when it recovers; log alert; optional email |
| Uptime analytics | Uptime % for 24 h and 7 days, total downtime, daily report for the last 7 days |
| Monitoring history | Every check is stored with timestamp, status code, response time and error; 30-day retention |

## How the monitoring works

```
@Scheduled tick (every 5 s)
   -> finds enabled sites whose own interval has elapsed
   -> runs each check in parallel on a thread pool (a site is never checked twice at once)
        HealthCheckService: HTTP GET, measure time, classify status code (2xx/3xx = up)
        MonitoringService : save CheckResult, update site status,
                            open an Incident on first failure / close it on recovery, raise an alert
   -> dashboard reads the results from MySQL and refreshes every 15 s
```

The HTTP request runs outside the database transaction, so a slow website never blocks the database.

## Quick start

### Option A: Docker Compose (recommended)

```bash
docker compose up -d --build
```

Open http://localhost:8080, create an account and add a site. Compose starts three containers:
`db` (MySQL 8.4), `app` (SiteWatch) and `demo-site` (a tiny nginx website for the downtime demo).

### Option B: run locally with Maven (no database needed, uses in-memory H2)

```bash
mvn spring-boot:run          # needs JDK 17+ and Maven
```

Data is lost on restart in this mode. Use Option A for persistent data.

## The five demonstrations

1. **Add three websites.** Add `https://www.google.com`, `https://www.amazon.in` and `http://demo-site`.
   Within seconds each row shows Online, a response time and a heartbeat strip of recent checks.
2. **Detect an unavailable website.** Run `docker compose stop demo-site`. After the next check the row turns
   red, an incident appears with the reason, and the log shows `ALERT: ... is DOWN`. Run
   `docker compose start demo-site` and the incident closes with its downtime duration.
   (You can also add `https://this-site-does-not-exist-12345.com` to show a DNS failure.)
3. **Response time and uptime reports.** Open a site: uptime 24 h / 7 d, average response, downtime total,
   the response-time chart, the daily report and the check history.
4. **Push to GitHub and watch Jenkins build it.** See the CI/CD section below.
5. **Show the containers.** `docker compose ps` shows `db`, `app` and `demo-site`, with health status.

## Tests

```bash
mvn test
```

| Test | What it proves |
|---|---|
| `StatusClassifierTest` | Which HTTP codes count as up or down |
| `UptimeCalculatorTest` | Uptime percentage and downtime duration, including clipped and ongoing incidents |
| `UrlValidatorTest` | URL normalisation and blocking of localhost / private IPs (SSRF protection) |
| `ReportServiceTest` | Daily report grouping |
| `MonitoringSchedulerTest` | A site is checked only when its own interval has elapsed |
| `HealthCheckServiceTest` | Real HTTP checks against a local test server: 200, 404, 500, redirect, timeout, refused connection |
| `MonitoringFlowTest` | Whole core flow: site up, then down (one incident opens), then recovered (incident closes) |
| `WebFlowTest` | Register, login, CSRF, add site, users cannot see each other's sites, SSRF refusal |

No test depends on the internet or on real websites.

## CI/CD with Git, GitHub, Maven, Jenkins and Docker

### 1. Git and GitHub

```bash
git init
git add .
git commit -m "Initial commit: SiteWatch"
git branch -M main
git remote add origin https://github.com/<your-username>/sitewatch.git
git push -u origin main
git checkout -b develop && git push -u origin develop      # long-lived development branch
```

Suggested flow: work on `feature/...` branches, merge into `develop`, and merge `develop` into `main`
when ready. Only `main` triggers the Deploy stage.

### 2. Start Jenkins (with Maven, Docker and Compose already inside)

```bash
cd jenkins
docker compose up -d --build
docker compose exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Open http://localhost:8081, paste the password, install the suggested plugins, and also install
**GitHub** and **Pipeline** plugins if they are missing.

### 3. Jenkins Freestyle job (the "build the application" demo)

1. New Item, name `sitewatch-freestyle`, type **Freestyle project**.
2. Source Code Management: Git, your repository URL, branch `*/main`.
3. Build Triggers: **GitHub hook trigger for GITScm polling**.
4. Build Steps: Execute shell, `mvn -B clean package`.
5. Post-build Actions: Publish JUnit test result report `target/surefire-reports/*.xml`, and Archive
   `target/sitewatch.jar`.

### 4. Jenkins Pipeline job (full automation)

1. New Item, name `sitewatch-pipeline`, type **Pipeline**.
2. Pipeline: **Pipeline script from SCM**, Git, your repository URL, branch `*/main`, script path `Jenkinsfile`.
3. Build Triggers: **GitHub hook trigger for GITScm polling**.

Stages in the `Jenkinsfile`: Checkout, Compile, Test, Package, Docker Build, Push Image (optional), Deploy (main only).
For Push Image, add a Jenkins credential with ID `dockerhub` and tick the `PUSH_IMAGE` parameter.

### 5. GitHub webhook

GitHub repository, Settings, Webhooks, Add webhook:

* Payload URL: `http://<public-address-of-jenkins>/github-webhook/` (keep the trailing slash)
* Content type: `application/json`, event: **Just the push event**

Jenkins on your laptop is not reachable from GitHub. Expose it with a tunnel
(`ngrok http 8081`) and use the ngrok URL, or fall back to **Poll SCM** (`H/2 * * * *`) in the job.

### 6. The live CI/CD demo

1. Change something visible, for example the text on the dashboard.
2. `git commit -am "Change dashboard text" && git push`
3. Watch Jenkins start by itself: Checkout, Compile, Test, Package, Docker Build, Deploy.
4. Refresh http://localhost:8080 to see the change running in the new container.
5. **Prove the pipeline guards quality:** break a test (for example change `199` to `200` in
   `StatusClassifierTest`), push, and show the Test stage failing and the Deploy stage never running.

## Security notes (worth mentioning to your teacher)

* Passwords are hashed with BCrypt; CSRF protection is on for all forms; every query is scoped to the
  logged-in user, so users cannot open each other's sites.
* **SSRF protection:** a monitoring tool makes requests to user-supplied URLs, so attackers could point it at
  `localhost`, a private network or the cloud metadata address. SiteWatch rejects such targets when a site is
  added and again before every check, and it does not follow redirects. `docker-compose.yml` turns this
  protection off (`SITEWATCH_ALLOW_PRIVATE_HOSTS=true`) only so the demo container can be monitored.
  Set it to `false` for any public deployment.
* Known limits: a DNS record that changes between the validation and the request is not fully covered, and
  `ddl-auto=update` is used instead of database migrations (use Flyway for production).

## Configuration

Main settings are in `src/main/resources/application.properties` and can be overridden with environment
variables: check timeout, thread pool size, minimum interval, history retention days, and email alerts
(`SITEWATCH_EMAIL_ALERTS=true` plus `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`,
`SPRING_MAIL_PASSWORD`). Health endpoint for Docker: `/actuator/health`.

## Project structure

```
sitewatch/
├── Jenkinsfile                  pipeline as code
├── Dockerfile                   multi-stage build (Maven, then a small JRE image, non-root user)
├── docker-compose.yml           app + MySQL + demo site, with health checks
├── jenkins/                     Jenkins image (Maven + Docker + Compose) and its compose file
├── pom.xml
└── src
    ├── main/java/com/sitewatch
    │   ├── model/    AppUser, Website, CheckResult, Incident
    │   ├── repo/     Spring Data repositories
    │   ├── service/  HealthCheckService, MonitoringService, MonitoringScheduler, StatsService,
    │   │             ReportService, AlertService, UrlValidator, UptimeCalculator, StatusClassifier
    │   ├── web/      controllers + JSON API for the chart
    │   ├── dto/ config/ util/
    └── main/resources/templates, static/css, application.properties
```

## Technology

Java 17, Spring Boot 3.3 (Web, Security, Data JPA, Validation, Actuator, Mail), Thymeleaf, Bootstrap 5,
Chart.js, MySQL 8.4 (H2 for local runs and tests), Maven, JUnit 5, Git and GitHub, Jenkins, Docker and Docker Compose.

## Likely viva questions

* **Why is the HTTP call outside the transaction?** A 10 s timeout would otherwise hold a database connection.
* **How do you avoid checking a site twice?** An in-flight set of site IDs in `MonitoringScheduler`.
* **Why are redirects not followed?** A redirect could send the checker to an internal address and bypass the SSRF check.
* **Why a multi-stage Dockerfile?** The final image contains only the JRE and the JAR, not Maven or the source.
* **What does the webhook do?** It tells Jenkins the moment you push, so no polling is needed.
