package de.filefly.app

import android.content.ContentResolver
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.filefly.core.data.SettingsStore
import de.filefly.core.data.history.UploadHistoryRepository
import de.filefly.core.network.ChunkedUploader
import de.filefly.core.network.FileFlyApi
import de.filefly.feature.history.HistoryScreen
import de.filefly.feature.history.HistoryViewModel
import de.filefly.feature.invite.InviteScreen
import de.filefly.feature.invite.InviteViewModel
import de.filefly.feature.onboarding.OnboardingScreen
import de.filefly.feature.onboarding.OnboardingViewModel
import de.filefly.feature.settings.SettingsScreen
import de.filefly.feature.settings.SettingsViewModel
import de.filefly.feature.updater.ApkDownloader
import de.filefly.feature.updater.PackageInstallerSession
import de.filefly.feature.updater.UpdateChecker
import de.filefly.feature.updater.UpdateViewModel
import de.filefly.feature.updater.UpdatesScreen
import de.filefly.feature.upload.MediaItem
import de.filefly.feature.upload.UploadScreen
import de.filefly.feature.upload.UploadViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

// Gebündelte App-Abhängigkeiten, die der NavGraph an die Screens durchreicht.
class AppDependencies(
    val settingsStore: SettingsStore,
    val api: FileFlyApi,
    val uploader: ChunkedUploader,
    val history: UploadHistoryRepository,
    val updater: UpdaterBundle,
    val versionName: String,
    val contentResolver: ContentResolver,
)

class UpdaterBundle(
    val checker: UpdateChecker,
    val downloader: ApkDownloader,
    val installer: PackageInstallerSession,
)

object Routes {
    const val ONBOARDING = "onboarding"
    const val UPLOAD = "upload"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val UPDATES = "updates"
    const val INVITE = "invite"
}

private enum class TopTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    UPLOAD(Routes.UPLOAD, "Upload", Icons.Filled.UploadFile),
    HISTORY(Routes.HISTORY, "Verlauf", Icons.Filled.History),
    SETTINGS(Routes.SETTINGS, "Einstellungen", Icons.Filled.Settings),
}

@Composable
fun FileFlyApp(
    sharedMedia: MutableStateFlow<List<MediaItem>>,
    pendingInvite: MutableStateFlow<String?>,
    deps: AppDependencies,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in setOf(Routes.UPLOAD, Routes.HISTORY, Routes.SETTINGS)

    // Startziel abhängig davon, ob schon eine Session besteht. Wird einmalig geprüft.
    // Ein per Deep-Link empfangener Invite erzwingt Onboarding (neuen Zugang einlösen).
    var startRoute by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val loggedIn = deps.settingsStore.isLoggedIn.first()
        startRoute =
            if (loggedIn && pendingInvite.value == null) Routes.UPLOAD else Routes.ONBOARDING
    }
    val resolvedStart = startRoute ?: return

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(currentRoute = currentRoute, navController = navController)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = resolvedStart,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingRoute(deps = deps, navController = navController, pendingInvite = pendingInvite)
            }
            composable(Routes.UPLOAD) {
                UploadRoute(sharedMedia = sharedMedia, deps = deps, navController = navController)
            }
            composable(Routes.HISTORY) {
                HistoryRoute(deps = deps)
            }
            composable(Routes.SETTINGS) {
                SettingsRoute(deps = deps, navController = navController)
            }
            composable(Routes.UPDATES) {
                UpdatesRoute(deps = deps, navController = navController)
            }
            composable(Routes.INVITE) {
                InviteRoute(deps = deps, navController = navController)
            }
        }
    }
}

@Composable
private fun BottomNavBar(
    currentRoute: String?,
    navController: NavHostController,
) {
    NavigationBar {
        TopTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { navController.navigateToTab(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}

@Composable
private fun OnboardingRoute(
    deps: AppDependencies,
    navController: NavHostController,
    pendingInvite: MutableStateFlow<String?>,
) {
    val vm =
        viewModel<OnboardingViewModel>(
            factory = simpleFactory { OnboardingViewModel(deps.api, deps.settingsStore) },
        )
    // Deep-Link-Invite einmalig übernehmen (Server-URL + Code vorbefüllen).
    val invite by pendingInvite.collectAsStateWithLifecycle()
    LaunchedEffect(invite) {
        invite?.let {
            vm.applyInviteUri(it)
            pendingInvite.value = null
        }
    }
    OnboardingScreen(
        viewModel = vm,
        onDone = {
            navController.navigate(Routes.UPLOAD) {
                popUpTo(Routes.ONBOARDING) { inclusive = true }
            }
        },
    )
}

@Composable
private fun UploadRoute(
    sharedMedia: MutableStateFlow<List<MediaItem>>,
    deps: AppDependencies,
    navController: NavHostController,
) {
    val media by sharedMedia.collectAsStateWithLifecycle()
    val vm =
        viewModel<UploadViewModel>(
            factory =
                simpleFactory {
                    UploadViewModel(
                        resolver = deps.contentResolver,
                        api = deps.api,
                        uploader = deps.uploader,
                        settings = deps.settingsStore,
                        history = deps.history,
                    )
                },
        )
    // Geteilte Medien einmalig übernehmen + Listing starten.
    LaunchedEffect(media) {
        if (media.isNotEmpty()) {
            vm.setItems(media)
            sharedMedia.value = emptyList()
        }
        vm.start()
    }
    UploadScreen(viewModel = vm, onBack = { navController.navigateToTab(Routes.UPLOAD) })
}

@Composable
private fun HistoryRoute(deps: AppDependencies) {
    val vm =
        viewModel<HistoryViewModel>(
            factory = simpleFactory { HistoryViewModel(deps.history) },
        )
    HistoryScreen(viewModel = vm)
}

@Composable
private fun SettingsRoute(
    deps: AppDependencies,
    navController: NavHostController,
) {
    val vm =
        viewModel<SettingsViewModel>(
            factory = simpleFactory { SettingsViewModel(deps.api, deps.settingsStore) },
        )
    SettingsScreen(
        viewModel = vm,
        onOpenUpdates = { navController.navigate(Routes.UPDATES) },
        onOpenInvites = { navController.navigate(Routes.INVITE) },
        onLoggedOut = {
            navController.navigate(Routes.ONBOARDING) {
                popUpTo(0) { inclusive = true }
            }
        },
    )
}

@Composable
private fun InviteRoute(
    deps: AppDependencies,
    navController: NavHostController,
) {
    val vm =
        viewModel<InviteViewModel>(
            factory = simpleFactory { InviteViewModel(deps.api, deps.settingsStore) },
        )
    InviteScreen(viewModel = vm, onBack = { navController.popBackStack() })
}

@Composable
private fun UpdatesRoute(
    deps: AppDependencies,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val vm =
        viewModel<UpdateViewModel>(
            factory =
                simpleFactory {
                    UpdateViewModel(
                        currentVersion = deps.versionName,
                        checker = deps.updater.checker,
                        downloader = deps.updater.downloader,
                        installer = deps.updater.installer,
                    )
                },
        )
    UpdatesScreen(
        viewModel = vm,
        onBack = { navController.popBackStack() },
        onFixInstallPermission = {
            context.startActivity(installUnknownAppsIntent(context.packageName))
        },
    )
}

private fun installUnknownAppsIntent(packageName: String) =
    android.content.Intent(
        android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        android.net.Uri.parse("package:$packageName"),
    )

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

// Minimaler ViewModelProvider.Factory-Helfer für parametrisierte ViewModels.
private inline fun <reified VM : ViewModel> simpleFactory(crossinline builder: () -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = builder() as T
    }
