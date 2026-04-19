/* ── Countdown Timer ── */
(function () {
    const el = document.getElementById('countdown');
    if (!el) return;

    const dateStr = el.dataset.date;
    const timeStr = el.dataset.time && el.dataset.time !== 'TBC' ? el.dataset.time : '12:00:00Z';
    const target = new Date(dateStr + 'T' + timeStr);

    function pad(n) { return String(n).padStart(2, '0'); }

    function tick() {
        const diff = target - Date.now();
        if (diff <= 0) {
            document.getElementById('days').textContent = '00';
            document.getElementById('hours').textContent = '00';
            document.getElementById('mins').textContent = '00';
            document.getElementById('secs').textContent = '00';
            return;
        }
        const d = Math.floor(diff / 86400000);
        const h = Math.floor((diff % 86400000) / 3600000);
        const m = Math.floor((diff % 3600000) / 60000);
        const s = Math.floor((diff % 60000) / 1000);
        document.getElementById('days').textContent = pad(d);
        document.getElementById('hours').textContent = pad(h);
        document.getElementById('mins').textContent = pad(m);
        document.getElementById('secs').textContent = pad(s);
    }

    tick();
    setInterval(tick, 1000);
})();

/* ── Accordion: toggle race result panels ── */
function toggleResult(id) {
    const panel = document.getElementById(id);
    if (!panel) return;
    panel.classList.toggle('open');
}

/* ── Push Notifications ── */
function urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
    const raw = atob(base64);
    return Uint8Array.from([...raw].map(c => c.charCodeAt(0)));
}

function showStatus(msg, type) {
    const el = document.getElementById('notifyStatus');
    if (!el) return;
    el.textContent = msg;
    el.className = 'notify-status ' + type;
    el.classList.remove('hidden');
    if (type !== 'error') {
        setTimeout(() => el.classList.add('hidden'), 5000);
    }
}

function toggleNotifications() {
    if (!('Notification' in window) || !('serviceWorker' in navigator)) {
        showStatus('Push notifications are not supported by your browser.', 'error');
        return;
    }
    if (Notification.permission === 'granted') {
        checkExistingSubscription().then(sub => {
            if (sub) {
                unsubscribeFromPush(sub);
            } else {
                openModal();
            }
        });
    } else if (Notification.permission === 'denied') {
        showStatus('Notifications are blocked. Please enable them in your browser settings.', 'error');
    } else {
        openModal();
    }
}

function openModal() {
    document.getElementById('notifyModal').classList.remove('hidden');
    document.getElementById('modalOverlay').classList.remove('hidden');
}

function closeModal() {
    document.getElementById('notifyModal').classList.add('hidden');
    document.getElementById('modalOverlay').classList.add('hidden');
}

async function requestNotificationPermission() {
    const statusEl = document.getElementById('modalStatus');
    statusEl.textContent = 'Requesting permission...';

    const permission = await Notification.requestPermission();
    if (permission !== 'granted') {
        statusEl.textContent = 'Permission denied. You can enable notifications in browser settings.';
        return;
    }

    statusEl.textContent = 'Registering service worker...';
    try {
        await subscribeToRaceNotifications();
        closeModal();
        showStatus('You are now subscribed to race day notifications!', 'success');
        document.getElementById('notifyBtn').textContent = '🔕 Unsubscribe';
    } catch (err) {
        statusEl.textContent = 'Failed to subscribe: ' + err.message;
        console.error('Push subscription failed:', err);
    }
}

async function checkExistingSubscription() {
    if (!('serviceWorker' in navigator)) return null;
    const reg = await navigator.serviceWorker.getRegistration('/sw.js');
    if (!reg) return null;
    return reg.pushManager.getSubscription();
}

async function subscribeToRaceNotifications() {
    const reg = await navigator.serviceWorker.register('/js/sw.js');
    await navigator.serviceWorker.ready;

    let vapidKey = typeof VAPID_PUBLIC_KEY !== 'undefined' ? VAPID_PUBLIC_KEY : '';
    if (!vapidKey) {
        const res = await fetch('/api/notifications/vapid-public-key');
        const data = await res.json();
        vapidKey = data.publicKey;
    }

    const existing = await reg.pushManager.getSubscription();
    if (existing) await existing.unsubscribe();

    const subscription = await reg.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(vapidKey)
    });

    const subJson = subscription.toJSON();
    await fetch('/api/notifications/subscribe', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            endpoint: subJson.endpoint,
            keys: { p256dh: subJson.keys.p256dh, auth: subJson.keys.auth }
        })
    });
}

async function unsubscribeFromPush(subscription) {
    await fetch('/api/notifications/unsubscribe', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ endpoint: subscription.endpoint })
    });
    await subscription.unsubscribe();
    showStatus('Unsubscribed from race notifications.', 'info');
    document.getElementById('notifyBtn').textContent = '🔔 Notify Me';
}

/* ── Init: update button state on load ── */
(async function () {
    if (!('serviceWorker' in navigator) || !('Notification' in window)) return;
    if (Notification.permission === 'granted') {
        const sub = await checkExistingSubscription();
        if (sub) {
            document.getElementById('notifyBtn').textContent = '🔕 Unsubscribe';
        }
    }
})();
