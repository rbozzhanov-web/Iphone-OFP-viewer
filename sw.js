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
// fetch still runs alongside it — online or offline, it's always attempted,
// it just resolves to nothing when there's no signal — to check for a newer
// index.html. Most of the time there isn't one, and nothing further happens:
// the cache is refreshed for next time exactly as before. Only when the
// fetched bytes actually differ from what was already cached does the page
// get told an update is sitting there, so it can decide to pick it up now
// instead of waiting for the next full relaunch.
function freshPage(req){
  const revalidate = fetch(req).then(r => {
    if (!r.ok) return r;
    const forCache = r.clone();
    const forCompare = r.clone();
    caches.open(V).then(c => c.match('./index.html').then(prevHit =>
      Promise.all([prevHit ? prevHit.clone().text() : null, forCompare.text()]).then(([prevText, freshText]) =>
        c.put('./index.html', forCache).then(() => {
          if (prevText !== null && prevText !== freshText) notifyUpdateAvailable();
        })
      )
    )).catch(() => {});
    return r;
  }).catch(() => null);
  return caches.match('./index.html').then(hit => hit || revalidate.then(r => r || caches.match('./index.html')));
}

function notifyUpdateAvailable(){
  self.clients.matchAll({ type: 'window' }).then(clients => {
    clients.forEach(client => client.postMessage({ type: 'ofp-update-available' }));
  });
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
