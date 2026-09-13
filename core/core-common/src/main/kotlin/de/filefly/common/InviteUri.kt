package de.filefly.common

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// Gemeinsames Format für Invite-Weitergabe per QR-Code und Deep-Link:
//
//     filefly://invite?server=<url-encoded base>&code=<code>
//
// Bewusst in :core-common (reines Kotlin/JVM, keine android.net.Uri-Abhängigkeit),
// damit dieselbe Logik von QR-Encode (feature-invite), QR-Scan und Deep-Link-Handling
// (feature-onboarding / MainActivity) genutzt wird und round-trip-testbar bleibt.
object InviteUri {
    const val SCHEME = "filefly"
    const val HOST = "invite"

    data class Parsed(
        val server: String,
        val code: String,
    )

    // Baut die kanonische Invite-URI aus Server-Basis-URL und Code.
    fun build(
        server: String,
        code: String,
    ): String {
        val s = enc(server.trim().trimEnd('/'))
        val c = enc(code.trim())
        return "$SCHEME://$HOST?server=$s&code=$c"
    }

    // Parst eine Invite-URI. Gibt null zurück, wenn Schema/Host nicht passen oder
    // der Code fehlt. server darf leer sein (dann muss der Nutzer die URL separat setzen).
    fun parse(raw: String?): Parsed? {
        val uri = raw?.trim().orEmpty()
        if (uri.isEmpty()) return null
        val prefix = "$SCHEME://$HOST"
        if (!uri.startsWith(prefix)) return null

        val query = uri.substringAfter('?', "")
        if (query.isEmpty()) return null

        val params =
            query.split('&').mapNotNull { pair ->
                val idx = pair.indexOf('=')
                if (idx <= 0) {
                    null
                } else {
                    dec(pair.substring(0, idx)) to dec(pair.substring(idx + 1))
                }
            }.toMap()

        val code = params["code"].orEmpty()
        if (code.isBlank()) return null
        return Parsed(server = params["server"].orEmpty(), code = code)
    }

    private fun enc(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun dec(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}
