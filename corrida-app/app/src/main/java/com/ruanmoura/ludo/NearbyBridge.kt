package com.ruanmoura.ludo

import android.app.Activity
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/** Liga os celulares por Bluetooth / Wi-Fi Direct (Google Nearby Connections), sem internet. */
class NearbyBridge(private val act: Activity, private val web: WebView) {
    private val client = Nearby.getConnectionsClient(act)
    private val service = "com.ruanmoura.ludo.sala"
    private val strategy = Strategy.P2P_STAR
    private val connected = mutableSetOf<String>()
    private val names = mutableMapOf<String, String>()
    private var myName = "Jogador"

    private fun emit(o: JSONObject) {
        val js = "window.onNearby&&window.onNearby(" + o.toString() + ")"
        act.runOnUiThread { web.evaluateJavascript(js, null) }
    }
    private fun ev(type: String, id: String? = null, name: String? = null, data: String? = null): JSONObject {
        val o = JSONObject().put("type", type)
        if (id != null) o.put("id", id)
        if (name != null) o.put("name", name)
        if (data != null) o.put("data", data)
        return o
    }
    fun emitError(msg: String) = emit(JSONObject().put("type", "error").put("msg", msg))

    private val payloads = object : PayloadCallback() {
        override fun onPayloadReceived(id: String, p: Payload) {
            val b = p.asBytes() ?: return
            emit(ev("msg", id, names[id], String(b, StandardCharsets.UTF_8)))
        }
        override fun onPayloadTransferUpdate(id: String, u: PayloadTransferUpdate) {}
    }

    private val life = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(id: String, info: ConnectionInfo) {
            names[id] = info.endpointName
            client.acceptConnection(id, payloads)
        }
        override fun onConnectionResult(id: String, r: ConnectionResolution) {
            if (r.status.isSuccess) {
                connected += id
                client.stopDiscovery()
                emit(ev("connected", id, names[id]))
            } else emit(ev("failed", id, names[id]))
        }
        override fun onDisconnected(id: String) {
            connected -= id
            emit(ev("disconnected", id, names[id]))
        }
    }

    private val discovery = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(id: String, info: DiscoveredEndpointInfo) {
            names[id] = info.endpointName
            emit(ev("found", id, info.endpointName))
        }
        override fun onEndpointLost(id: String) { emit(ev("lost", id)) }
    }

    @JavascriptInterface
    fun host(name: String) {
        myName = name
        client.startAdvertising(name, service, life, AdvertisingOptions.Builder().setStrategy(strategy).build())
            .addOnFailureListener { e -> emitError("Não abriu a sala: " + (e.message ?: "")) }
    }

    @JavascriptInterface
    fun join(name: String) {
        myName = name
        client.startDiscovery(service, discovery, DiscoveryOptions.Builder().setStrategy(strategy).build())
            .addOnFailureListener { e -> emitError("Não conseguiu procurar: " + (e.message ?: "")) }
    }

    @JavascriptInterface
    fun connect(id: String) {
        client.requestConnection(myName, id, life).addOnFailureListener { e -> emitError(e.message ?: "não conectou") }
    }

    @JavascriptInterface
    fun send(to: String, msg: String) {
        val p = Payload.fromBytes(msg.toByteArray(StandardCharsets.UTF_8))
        val ids = if (to == "*") connected.toList() else listOf(to)
        if (ids.isNotEmpty()) client.sendPayload(ids, p)
    }

    @JavascriptInterface
    fun disconnect(id: String) { client.disconnectFromEndpoint(id); connected -= id }

    @JavascriptInterface
    fun stop() {
        client.stopAdvertising(); client.stopDiscovery(); client.stopAllEndpoints(); connected.clear()
    }
}
