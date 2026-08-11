# Stock Research App — Backend

Spring Boot 3 / Java 21 backend for the personal Indian stock research tool.

## What's included in this delivery

This is the **backend half** of a vertical-slice build: a complete, working
architecture demonstrated end-to-end with **3 seeded demo companies**
(RVNL – Railway, BEL – Defense, DEEPAKNTR – Chemicals) rather than the full
NSE/BSE universe. Data sources (price data, news, corporate announcements)
are **stub implementations** returning realistic canned data — see
"Swapping in real data sources" below for how to replace them.

## Running it

```bash
cd backend
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. On first boot, `DataSeeder` seeds
the 3 demo companies and runs the discovery pipeline twice (so there's a
"previous" score to diff against for the score-change feature), then the app
is ready to use.

Data persists in a local H2 file database at `backend/data/stockresearch.mv.db`.
Delete that file to reset to a fresh seed on next boot.

H2 console (for inspecting the DB directly) is available at
`http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/stockresearch`).

## Switching to PostgreSQL

Set environment variables before starting:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/stockresearch
export DB_DRIVER=org.postgresql.Driver
export DB_USER=your_user
export DB_PASSWORD=your_password
mvn spring-boot:run
```

## Configuring AI (Settings)

Once running, `PUT /api/settings` with a body like:

```json
{
  "aiProvider": "CLAUDE",
  "apiKey": "sk-ant-...",
  "modelName": "claude-sonnet-4-6",
  "temperature": 0.3,
  "maxTokens": 1500
}
```

Supported `aiProvider` values: `CLAUDE`, `OPENAI`, `GEMINI`, `OPENROUTER`.
(`LOCAL` is accepted by the schema for future use but has no client yet.)

Then `POST /api/companies/{companyId}/research/generate` to generate AI
research for a company (find `companyId` via `GET /api/companies/search?q=RVNL`).

## Key API endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/dashboard` | Dashboard module aggregate |
| GET | `/api/companies/search?q=` | Stock Explorer search |
| GET | `/api/companies/filter?...` | Stock Explorer filters |
| GET | `/api/companies/{id}` | Company detail page |
| GET | `/api/companies/{id}/score-change` | "Why did the score change?" |
| GET | `/api/companies/{id}/why-interesting` | "Why is this stock interesting?" card |
| POST | `/api/companies/{id}/research/generate` | Generate AI Research |
| GET | `/api/watchlist` | Watchlist |
| POST/DELETE | `/api/watchlist/{companyId}` | Add/remove watchlist item |
| GET | `/api/events` | Event Center (all, or `?type=LARGE_ORDER`) |
| GET | `/api/sectors` | Sector Dashboard (all 10 tracked sectors) |
| GET/PUT | `/api/settings` | Settings |
| POST | `/api/discovery/refresh` | Manually trigger the discovery pipeline |

## Architecture

```
domain/            JPA entities
repository/         Spring Data JPA repositories
dto/                API request/response objects (never expose entities directly)
service/
  datasource/       PriceDataSource, NewsSource, AnnouncementSource interfaces
                     + Stub*  implementations (swap these for real integrations)
  scoring/          ScoreRule interface + 17 independent rule implementations,
                     combined by ScoringEngine
  discovery/        DiscoveryPipeline (orchestrates Stages 1-10), 
                     FundamentalScreeningStage, EventClassifier
  ai/               AiClient interface + Claude/OpenAI/Gemini/OpenRouter clients,
                     AiClientRouter, PromptBuilder, ResearchService
  *Service.java     Application services backing each UI module
controller/         REST controllers (thin, delegate to services)
scheduler/          RefreshScheduler (background job, runs hourly, honors
                     the configured refresh interval from Settings)
config/             CorsConfig, DataSeeder
```

## Swapping in real data sources

Each stub (`StubPriceDataSource`, `StubNewsSource`, `StubAnnouncementSource`)
implements a plain interface (`PriceDataSource`, `NewsSource`,
`AnnouncementSource`). To go live:

1. Implement a new class, e.g. `YahooFinancePriceDataSource implements PriceDataSource`.
2. Annotate it `@Primary` (or use a Spring `@Profile`) so it takes precedence
   over the stub.
3. Nothing else in the codebase needs to change — `DiscoveryPipeline` and
   everything downstream only depend on the interface.

## Notes on the scoring engine

Every scoring dimension (`Revenue Growth`, `Debt Reduction`,
`Government Policy`, etc.) is an independent Spring `@Component` implementing
`ScoreRule`. To add a new dimension, add a new class in
`service/scoring/rules/` — `ScoringEngine` picks it up automatically via
Spring's dependency injection (no registration step needed). To change a
rule's weight or logic, edit only that one file.

## AI safety constraint

The system prompt in `PromptBuilder` explicitly forbids buy/sell language,
and `ResearchSummary` (the entity that stores AI output) has no field for a
recommendation — there's nowhere to put one even if a model ignored the
instruction. This mirrors the product requirement that the app "should never
recommend buying or selling."

## What's NOT included yet (by design, per this delivery's scope)

- Real Yahoo Finance / NSE / BSE / Google News integrations (stubs only)
- The full NSE+BSE company universe (only 3 seeded companies)
- Dynamic, DB-backed government-tailwind keyword list (currently a static
  set in `GovernmentTailwindScoreRule` — easy to move to a table later)
- Historical fundamental snapshots for true period-over-period margin
  comparison (currently `MarginExpansionScoreRule` scores absolute margin
  level; `ScoreSnapshot`'s design already supports adding this)
