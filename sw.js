/* OFP Viewer — offline cache. The whole app is index.html. */
const V = 'ofp-viewer-v1';
const FILES = ['./', './index.html', './manifest.webmanifest', './icon-192.png', './icon-512.png'];

// Cache.addAll() is all-or-nothing: one flaky fetch among the five files here —
// exactly the weak-signal case this whole cache exists for — used to fail the
// entire install, which meant skipWaiting() never ran and the cache stayed
// permanently empty, so freshPage() below always missed and fell through to
// the network wait on every single launch, forever, not just once. Caching
// each file independently means a single miss doesn't cost the others.
self.addEventListener('install', e => {
  e.waitUntil(
    caches.open(V)
      .then(c => Promise.all(FILES.map(url => fetch(url).then(r => { if (r.ok) return c.put(url, r); }).catch(() => {}))))
      .then(() => self.skipWaiting())
  );
});
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys()
    .then(ks => Promise.all(ks.filter(k => k !== V).map(k => caches.delete(k))))
    .then(() => self.clients.claim()));
});

// Cache-first, not a race against the network: a pilot opening this on a plane
// or in a hangar with no signal is the normal case, not the fallback one, and
// a page that waits on the network first reads as broken exactly when it
// matters most. The cache answers immediately every time there is one; a
// fetch still runs alongside it to refresh the cache for the launch after
// this one. Offline never updates, same as before — the version you leave
// the ground with is the version you fly with, just without a wait to get it.
function freshPage(req){
  const revalidate = fetch(req).then(r => {
    caches.open(V).then(c => c.put('./index.html', r.clone())).catch(() => {});
    return r;
  }).catch(() => null);
  return caches.match('./index.html').then(hit => hit || revalidate.then(r => r || caches.match('./index.html')));
}

self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET') return;
  if (e.request.mode === 'navigate'){ e.respondWith(freshPage(e.request)); return; }
  e.respondWith(
    caches.match(e.request, { ignoreSearch: true })
      .then(hit => hit || fetch(e.request).then(r => {
        const copy = r.clone();
        caches.open(V).then(c => c.put(e.request, copy)).catch(() => {});
        return r;
      }).catch(() => caches.match('./index.html')))
  );
});
