import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // During development the page runs on :5173 and the Java API on :8080.
    // Browsers block a page from calling a different address (CORS), so Vite
    // forwards anything starting with /api to the Java app instead.
    // The browser only ever talks to :5173.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
