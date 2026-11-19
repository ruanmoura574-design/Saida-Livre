// Comanda Online mudou para comandaonlinebr.github.io: este service worker se remove sozinho.
self.addEventListener("install",()=>self.skipWaiting());
self.addEventListener("activate",e=>e.waitUntil((async()=>{for(const k of await caches.keys())await caches.delete(k);await self.registration.unregister();for(const c of await self.clients.matchAll())c.navigate(c.url)})()));
