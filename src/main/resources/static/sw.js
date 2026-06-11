// Basic service worker for PWA offline support
self.addEventListener('install', () => {
    self.skipWaiting();
});

self.addEventListener('fetch', (event) => {
    event.respondWith(
        fetch(event.request).catch(() => new Response('Offline', {status: 503}))
    );
});
