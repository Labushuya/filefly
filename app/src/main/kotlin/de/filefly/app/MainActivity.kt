package de.filefly.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.filefly.common.ThemePreferences
import de.filefly.core.data.SettingsStore
import de.filefly.core.data.history.UploadHistoryRepository
import de.filefly.core.network.ChunkedUploader
import de.filefly.core.network.FileFlyApi
import de.filefly.core.ui.theme.FileFlyTheme
import de.filefly.feature.updater.ApkDownloader
import de.filefly.feature.updater.PackageInstallerSession
import de.filefly.feature.updater.UpdateChecker
import de.filefly.feature.upload.MediaItem
import de.filefly.feature.upload.MediaResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Einzige Activity. Empfängt Share-Intents (SEND/SEND_MULTIPLE), lädt die Theme-
// Präferenz (Splash hält, bis geladen) und hostet den NavGraph.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsStore: SettingsStore

    @Inject lateinit var api: FileFlyApi

    @Inject lateinit var uploader: ChunkedUploader

    @Inject lateinit var history: UploadHistoryRepository

    @Inject lateinit var updateChecker: UpdateChecker

    @Inject lateinit var apkDownloader: ApkDownloader

    @Inject lateinit var packageInstaller: PackageInstallerSession

    private val themeState = MutableStateFlow<ThemePreferences?>(null)

    // Vom Share-Sheet übergebene Medien — vom NavHost einmalig konsumiert.
    private val sharedMedia = MutableStateFlow<List<MediaItem>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { themeState.value == null }
        handleShareIntent(intent)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                settingsStore.themePreferences.collect { themeState.value = it }
            }
        }

        enableEdgeToEdge()
        setContent {
            val prefs by themeState.collectAsStateWithLifecycle()
            val resolved = prefs ?: return@setContent // Splash hält noch
            FileFlyTheme(prefs = resolved) {
                FileFlyApp(
                    sharedMedia = sharedMedia,
                    deps =
                        AppDependencies(
                            settingsStore = settingsStore,
                            api = api,
                            uploader = uploader,
                            history = history,
                            updater = UpdaterBundle(updateChecker, apkDownloader, packageInstaller),
                            versionName = versionName(),
                            contentResolver = contentResolver,
                        ),
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    // ACTION_SEND (eine Datei) oder ACTION_SEND_MULTIPLE (Bulk) auswerten und die
    // enthaltenen content:// URIs zu MediaItems auflösen.
    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        val uris: List<Uri> =
            when (intent.action) {
                Intent.ACTION_SEND -> listOfNotNull(intent.parcelableExtra(Intent.EXTRA_STREAM))
                Intent.ACTION_SEND_MULTIPLE ->
                    intent.parcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
                else -> emptyList()
            }
        if (uris.isEmpty()) return
        sharedMedia.value = uris.map { MediaResolver.resolve(contentResolver, it) }
    }

    private fun versionName(): String =
        runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        }.getOrDefault("?")
}

// Typ-sichere Parcelable-Extras (API-Level-abhängig).
private inline fun <reified T : Parcelable> Intent.parcelableExtra(key: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }

private inline fun <reified T : Parcelable> Intent.parcelableArrayListExtra(key: String): ArrayList<T>? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableArrayListExtra(key)
    }
