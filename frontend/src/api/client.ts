import axios from 'axios';

// Relative base URL - in dev, Vite's proxy (see vite.config.ts) forwards
// /api/* to the Spring Boot backend. In production, the frontend is expected
// to be served from the same origin as the backend (or reverse-proxied), so
// a relative path works there too without configuration.
export const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});
