package de.filefly.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.filefly.common.Role
import de.filefly.common.ThemeMode
import de.filefly.common.ThemePreferences
import de.filefly.core.network.CredentialProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

// Persistiert die Server-Konfiguration (Basis-URL, JWT, Rolle, base_path) sowie
// UI-Einstellungen (Theme, Chunk-Größe) in DataStore. Implementiert zugleich den
// CredentialProvider für core-network — der Client fragt Basis-URL/Token synchron ab,
// deshalb wird der zuletzt bekannte Wert zusätzlich in-memory gecacht.
private val Context.settingsStore by preferencesDataStore(name = "filefly_settings")

class SettingsStore(
    private val context: Context,
) : CredentialProvider {
    private val serverUrlKey = stringPreferencesKey("server_url")
    private val tokenKey = stringPreferencesKey("jwt_token")
    private val roleKey = stringPreferencesKey("role")
    private val basePathKey = stringPreferencesKey("base_path")
    private val permissionsKey = stringPreferencesKey("permissions")
    private val chunkSizeKey = intPreferencesKey("chunk_size_kb")
    private val themeModeKey = stringPreferencesKey("theme_mode")

    // In-memory-Cache für den synchronen CredentialProvider (der HTTP-Client kann
    // nicht suspend abfragen). Wird beim Start und bei jedem Setzen aktualisiert.
    @Volatile private var cachedUrl: String = ""

    @Volatile private var cachedToken: String? = null

    init {
        // Einmalig den persistierten Stand in den Cache laden (Boot).
        runCatching {
            runBlocking {
                val prefs = context.settingsStore.data.first()
                cachedUrl = prefs[serverUrlKey].orEmpty()
                cachedToken = prefs[tokenKey]
            }
        }
    }

    // --- CredentialProvider ---

    override fun baseUrl(): String = cachedUrl.trimEnd('/')

    override fun token(): String? = cachedToken

    // --- Server-URL ---

    val serverUrl: Flow<String> =
        context.settingsStore.data.map { it[serverUrlKey].orEmpty() }.distinctUntilChanged()

    suspend fun setServerUrl(url: String) {
        val cleaned = url.trim().trimEnd('/')
        cachedUrl = cleaned
        context.settingsStore.edit { it[serverUrlKey] = cleaned }
    }

    // --- Session (Token + Rolle + base_path + Rechte) ---

    val token: Flow<String?> =
        context.settingsStore.data.map { it[tokenKey] }.distinctUntilChanged()

    val role: Flow<Role> =
        context.settingsStore.data.map { Role.fromString(it[roleKey]) }.distinctUntilChanged()

    val basePath: Flow<String> =
        context.settingsStore.data.map { it[basePathKey].orEmpty() }.distinctUntilChanged()

    val permissions: Flow<List<String>> =
        context.settingsStore.data
            .map { it[permissionsKey]?.split(',')?.filter(String::isNotBlank) ?: emptyList() }
            .distinctUntilChanged()

    val isLoggedIn: Flow<Boolean> =
        context.settingsStore.data.map { !it[tokenKey].isNullOrBlank() }.distinctUntilChanged()

    suspend fun saveSession(
        token: String,
        role: String,
        basePath: String,
        permissions: List<String>,
    ) {
        cachedToken = token
        context.settingsStore.edit {
            it[tokenKey] = token
            it[roleKey] = role
            it[basePathKey] = basePath
            it[permissionsKey] = permissions.joinToString(",")
        }
    }

    suspend fun logout() {
        cachedToken = null
        context.settingsStore.edit {
            it.remove(tokenKey)
            it.remove(roleKey)
            it.remove(basePathKey)
            it.remove(permissionsKey)
        }
    }

    // --- Chunk-Größe (KB) ---

    val chunkSizeKb: Flow<Int> =
        context.settingsStore.data.map { it[chunkSizeKey] ?: DEFAULT_CHUNK_KB }.distinctUntilChanged()

    suspend fun setChunkSizeKb(kb: Int) {
        context.settingsStore.edit { it[chunkSizeKey] = kb.coerceIn(MIN_CHUNK_KB, MAX_CHUNK_KB) }
    }

    // --- Theme ---

    val themePreferences: Flow<ThemePreferences> =
        context.settingsStore.data
            .map { prefs ->
                ThemePreferences(
                    mode =
                        prefs[themeModeKey]
                            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                            ?: ThemeMode.SYSTEM,
                )
            }.distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsStore.edit { it[themeModeKey] = mode.name }
    }

    companion object {
        const val DEFAULT_CHUNK_KB = 2048 // 2 MB
        const val MIN_CHUNK_KB = 256
        const val MAX_CHUNK_KB = 10240 // 10 MB (Server-Limit)
    }
}
