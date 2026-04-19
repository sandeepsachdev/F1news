/* F1 Hub — Service Worker */

const CACHE_NAME = 'f1hub-v1';
const CACHE_ASSETS = ['/', '/css/style.css', '/js/app.js'];

self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(CACHE_NAME).then(cache => cache.addAll(CACHE_ASSETS))
    );
    self.skipWaiting();
});

self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(keys =>
            Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))
        )
    );
    self.clients.claim();
});

self.addEventListener('fetch', event => {
    if (event.request.method !== 'GET') return;
    event.respondWith(
        caches.match(event.request).then(cached => cached || fetch(event.request))
    );
});

self.addEventListener('push', event => {
    let data = { title: 'F1 Hub', body: 'Race day is coming!', url: '/', icon: '' };
    try {
        data = { ...data, ...JSON.parse(event.data.text()) };
    } catch (e) {
        data.body = event.data.text();
    }

    event.waitUntil(
        self.registration.showNotification(data.title, {
            body: data.body,
            icon: data.icon || '/favicon.ico',
            badge: '/favicon.ico',
            tag: 'f1-race-alert',
            renotify: true,
            data: { url: data.url },
            actions: [
                { action: 'open', title: 'View F1 Hub' },
                { action: 'dismiss', title: 'Dismiss' }
            ]
        })
    );
});

self.addEventListener('notificationclick', event => {
    event.notification.close();
    if (event.action === 'dismiss') return;

    const url = event.notification.data?.url || '/';
    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clientList => {
            for (const client of clientList) {
                if (client.url === url && 'focus' in client) return client.focus();
            }
            if (clients.openWindow) return clients.openWindow(url);
        })
    );
});
