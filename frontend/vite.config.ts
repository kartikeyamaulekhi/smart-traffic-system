import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Dev server on 5173 (matches the gateway's default CORS allowlist), proxying
// API + auth traffic to the gateway on 8080 so the browser never sees CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/auth': 'http://localhost:8080',
    },
  },
});