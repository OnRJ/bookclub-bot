import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/**/*.test.{ts,tsx}', 'src/main.tsx', 'src/types/**'],
      reporter: ['text', 'html', 'json-summary'],
    },
  },
  server: {
    host: '0.0.0.0',
    allowedHosts: [
      'flash-engaging-membrane-today.trycloudflare.com',
    ],
  },
})
