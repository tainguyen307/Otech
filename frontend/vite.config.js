import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  server: {
    host: true,
  },
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: 'autoUpdate',
      devOptions: {
        enabled: true,
        type: 'module',
      },
      includeAssets: ['favicon.ico', 'icon.svg', 'icon320.png', 'icon512.png'],
      manifest: {
        name: 'Otech Social Marketplace',
        short_name: 'Otech',
        description: 'A social marketplace for giving useful things a second life.',
        id: '/',
        theme_color: '#087cf5',
        background_color: '#f5f7fa',
        display: 'standalone',
        start_url: '/',
        scope: '/',
        orientation: 'portrait-primary',
        categories: ['shopping', 'social'],
        lang: 'en',
        icons: [
          {
            src: '/icon192.png',
            sizes: '192x192',
            type: 'image/png',
            purpose: 'any',
          },
          {
            src: '/icon512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
        ],
        screenshots: [
          {
            src: '/icon512.png',
            sizes: '512x512',
            type: 'image/png',
            form_factor: 'wide',
            label: 'Otech marketplace on desktop',
          },
          {
            src: '/icon320.png',
            sizes: '320x320',
            type: 'image/png',
            label: 'Otech marketplace on mobile',
          },
        ],
      },
      workbox: {
        cleanupOutdatedCaches: true,
        navigateFallback: '/index.html',
        runtimeCaching: [
          {
            urlPattern: /^https:\/\/(images\.unsplash\.com|i\.pravatar\.cc)\//,
            handler: 'CacheFirst',
            options: {
              cacheName: 'otech-images',
              expiration: {
                maxEntries: 60,
                maxAgeSeconds: 60 * 60 * 24 * 30,
              },
              cacheableResponse: { statuses: [0, 200] },
            },
          },
          {
            urlPattern: /^https:\/\/fonts\.(googleapis|gstatic)\.com\//,
            handler: 'CacheFirst',
            options: {
              cacheName: 'otech-fonts',
              expiration: {
                maxEntries: 20,
                maxAgeSeconds: 60 * 60 * 24 * 365,
              },
              cacheableResponse: { statuses: [0, 200] },
            },
          },
        ],
      },
    }),
  ],
})
