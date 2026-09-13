package de.filefly.core.network

// Liefert dem Client zur Laufzeit die aktuelle Server-URL + das JWT. Wird von
// core-data (SettingsStore) implementiert; core-network bleibt so persistenz-frei
// und JVM-testbar (Test liefert eine feste Fake-Implementierung).
interface CredentialProvider {
    // Basis-URL des Servers inkl. Schema, z.B. "http://192.168.1.50:8000". Ohne
    // Trailing-Slash. Leer, solange kein Server konfiguriert ist.
    fun baseUrl(): String

    // Aktuelles Bearer-Token oder null, solange kein Invite eingelöst wurde.
    fun token(): String?
}
