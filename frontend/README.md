# Stock Research App — Frontend

React 18 / Vite / TypeScript / Material UI (v6) / React Query / React Router.
Dark-mode-first, per the product spec.

## Running it

Requires the backend running on `http://localhost:8080` (see `backend/README.md`).

```bash
cd frontend
npm install
npm run dev
```

Opens at `http://localhost:5173`. In dev, Vite proxies all `/api/*` requests
to the backend (see `vite.config.ts`), so no CORS configuration or hardcoded
hostnames are needed in the frontend code itself.

## Building for production

```bash
npm run build
```

Outputs a static bundle to `dist/`. Serve it with any static file server, or
reverse-proxy `/api/*` to the Spring Boot backend and serve `dist/` for
everything else from the same origin.

Verified: both `npm run dev` and `npm run build` were run against a fresh
`npm install` during development of this app and complete with zero errors.

## Structure

```
src/
├── api/          Typed API client functions (one file per backend domain),
│                  all built on a single shared Axios instance (api/client.ts)
├── types/         TypeScript interfaces mirroring the backend DTOs exactly -
│                  keep these in sync manually if backend DTOs change
├── hooks/         React Query hooks wrapping the API layer (useDashboard,
│                  useCompany, useWatchlist, useMisc for events/sectors/
│                  settings/discovery)
├── pages/         One component per route/module:
│                    DashboardPage, StockExplorerPage, WatchlistPage,
│                    EventCenterPage, SectorDashboardPage, SectorDetailPage,
│                    SettingsPage, CompanyDetailPage
├── components/    Shared UI pieces used across pages:
│                    AppLayout (nav shell), ScoreBadge, ScoreChangeChip,
│                    WhyInterestingCard, CompanyListItem, EventListItem,
│                    QueryStateBoundary (loading/error handling)
├── theme/         MUI dark/light theme definitions (dark is default)
├── router/        React Router route table
└── constants.ts   Shared enums/lists mirrored from the backend
                    (tracked sectors, event types)
```

## Notes on MUI Grid

This app uses MUI v6.5's newer `Grid` API (`<Grid size={{ xs: 6, sm: 3 }}>`)
via the `@mui/material/Grid2` import path, aliased to `Grid` in each file
that uses it. The plain `@mui/material` `Grid` export is still the legacy
v1 API (`item xs={6}`) for backwards compatibility, so importing from
`Grid2` specifically is required for the `size` prop syntax used throughout
this codebase.

## Notes on the path alias

`@/*` resolves to `src/*` and is configured in **two** places that must stay
in sync: `tsconfig.app.json` (`compilerOptions.paths`, for type-checking) and
`vite.config.ts` (`resolve.alias`, for the actual bundler). TypeScript's
`paths` option only affects type-checking — it does not rewrite import
paths — so Vite/Rollup needs its own alias or the production build will fail
to resolve `@/...` imports even though `tsc` reports no errors.

## What's NOT included yet

- Financial charts (spec mentions "Financial Charts" on the company page;
  `recharts` is included as a dependency and ready to use, but no chart
  component has been built yet since the backend doesn't yet store
  historical time-series fundamentals - only point-in-time snapshots plus
  score history)
- Code-splitting / lazy loading (the production bundle is a single ~650KB
  JS chunk - fine for a personal single-user tool, but worth splitting by
  route with `React.lazy()` if it grows)
