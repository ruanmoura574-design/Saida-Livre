const C="saidalivre-v18",FILES=["./","index.html","manifest.webmanifest","privacidade.html","icon-192.png","icon-512.png"];
self.addEventListener("install",e=>{e.waitUntil(caches.open(C).then(c=>c.addAll(FILES)));self.skipWaiting()});
self.addEventListener("activate",e=>{e.waitUntil(caches.keys().then(k=>Promise.all(k.filter(x=>x!==C).map(x=>caches.delete(x)))));self.clients.claim()});
// páginas: busca sempre a versão nova na internet; só usa a cópia salva se estiver sem internet
self.addEventListener("fetch",e=>{if(e.request.method!=="GET"||e.request.url.includes("/corrida/"))return;const nav=e.request.mode==="navigate"||e.request.url.endsWith(".html");
 if(nav){e.respondWith(fetch(e.request,{cache:"no-store"}).then(r=>{const cl=r.clone();caches.open(C).then(c=>c.put(e.request,cl));return r}).catch(()=>caches.match(e.request).then(r=>r||caches.match("index.html"))));return}
 e.respondWith(caches.match(e.request).then(r=>r||fetch(e.request).then(res=>{if(res.ok&&(e.request.url.startsWith(self.location.origin)||e.request.url.includes("fonts.g"))){const cl=res.clone();caches.open(C).then(c=>c.put(e.request,cl))}return res})))});
