package de.filefly.common

// Rollen des FileFly-Servers, gespiegelt für die App. Bestimmt, welche Datei-
// Operationen die UI überhaupt anbietet (der Server erzwingt sie zusätzlich).
enum class Role {
    // Vollzugriff inkl. DIR delete/create/rename, Invite-Management.
    ADMIN,

    // Festes Basisverzeichnis (aus Invite); Upload + DIR create/rename.
    USER,

    // Festes Verzeichnis, nur Upload (kein UI-Eingriff, automatisiert).
    SERVICE,

    // Temporär (Ablaufzeit im Invite), nur Upload in fixem Verzeichnis.
    GUEST,

    // Unbekannte/nicht gesetzte Rolle -> konservativ nur lesen.
    UNKNOWN,
    ;

    val canUpload: Boolean get() = this == ADMIN || this == USER || this == SERVICE || this == GUEST
    val canMkdir: Boolean get() = this == ADMIN || this == USER
    val canRename: Boolean get() = this == ADMIN || this == USER
    val canDelete: Boolean get() = this == ADMIN

    companion object {
        fun fromString(raw: String?): Role =
            when (raw?.lowercase()) {
                "admin" -> ADMIN
                "user" -> USER
                "service" -> SERVICE
                "guest" -> GUEST
                else -> UNKNOWN
            }
    }
}

// Strategie bei Namenskonflikt beim Upload (vom Server bei /upload/init erwartet).
enum class ConflictStrategy(val wireValue: String) {
    OVERWRITE("overwrite"),
    RENAME("rename"),
    SKIP("skip"),
}
