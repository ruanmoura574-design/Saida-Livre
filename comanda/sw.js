// Comanda Online: sempre busca a versão mais nova na internet;
// sem internet, abre a última versão guardada no celular.
const CACHE = "comanda-online";
const PRE = ["./", "./index.html", "./manifest.webmanifest", "./icons/icon-192.png", "./icons/icon-512.png"];
self.addEventListener("install", e => { self.skipWaiting(); e.waitUntil(caches.open(CACHE).then(c => c.addAll(PRE)).catch(() => {})); });
self.addEventListener("activate", e => e.waitUntil(self.clients.claim()));
const cacheable = u => u.origin === self.location.origin || (u.hostname === "www.gstatic.com" && u.pathname.startsWith("/firebasejs/")) || u.hostname === "fonts.googleapis.com" || u.hostname === "fonts.gstatic.com";
self.addEventListener("fetch", e => {
  const r = e.request; if (r.method !== "GET") return;
  const u = new URL(r.url); if (!cacheable(u)) return;
  e.respondWith(fetch(r).then(res => { if (res.ok || res.type === "opaque") { const cp = res.clone(); caches.open(CACHE).then(c => c.put(r, cp)); } return res; })
    .catch(() => caches.match(r).then(m => m || (r.mode === "navigate" ? caches.match("./index.html") : Response.error()))));
});
// toque na notificação: abre a comanda (ou traz ela para a frente)
self.addEventListener("notificationclick", e => {
  e.notification.close();
  const url = (e.notification.data && e.notification.data.url) || self.registration.scope;
  e.waitUntil(self.clients.matchAll({ type: "window", includeUncontrolled: true }).then(list => {
    for (const c of list) { if (c.url.startsWith(self.registration.scope) && "focus" in c) return c.focus(); }
    return self.clients.openWindow(url);
  }));
});
