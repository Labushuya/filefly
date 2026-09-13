package de.filefly.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.filefly.core.data.SettingsStore
import de.filefly.core.data.history.UploadHistoryRepository
import de.filefly.core.network.ChunkedUploader
import de.filefly.core.network.CredentialProvider
import de.filefly.core.network.FileFlyApi
import de.filefly.feature.updater.ApkDownloader
import de.filefly.feature.updater.PackageInstallerSession
import de.filefly.feature.updater.UpdateChecker
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Singleton

// Stellt die core-Bausteine app-weit bereit. core-* bleiben DI-annotationsfrei;
// die Bindung passiert hier zentral. SettingsStore ist zugleich der CredentialProvider
// (Server-URL + JWT) für den HTTP-Client.
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // latest.json des Updaters. GitHub Pages ist für das Repo NICHT aktiviert, daher
    // direkt den gh-pages-Branch über raw.githubusercontent lesen (liefert 200; die
    // .github.io-URL gäbe 404). Wird von release.yml auf gh-pages publiziert.
    private const val LATEST_JSON_URL =
        "https://raw.githubusercontent.com/Labushuya/filefly/gh-pages/latest.json"

    @Provides
    @Singleton
    fun provideSettingsStore(
        @ApplicationContext context: Context,
    ): SettingsStore = SettingsStore(context)

    // SettingsStore implementiert CredentialProvider — dieselbe Instanz binden.
    @Provides
    @Singleton
    fun provideCredentialProvider(settingsStore: SettingsStore): CredentialProvider = settingsStore

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = FileFlyApi.defaultClient()

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Provides
    @Singleton
    fun provideFileFlyApi(
        client: OkHttpClient,
        credentials: CredentialProvider,
        json: Json,
    ): FileFlyApi = FileFlyApi(client, credentials, json)

    @Provides
    @Singleton
    fun provideChunkedUploader(
        client: OkHttpClient,
        api: FileFlyApi,
        credentials: CredentialProvider,
    ): ChunkedUploader = ChunkedUploader(client, api, credentials)

    @Provides
    @Singleton
    fun provideUploadHistoryRepository(
        @ApplicationContext context: Context,
    ): UploadHistoryRepository = UploadHistoryRepository.create(context)

    // --- Updater ---

    @Provides
    @Singleton
    fun provideUpdateChecker(
        client: OkHttpClient,
        json: Json,
    ): UpdateChecker = UpdateChecker(client, json, LATEST_JSON_URL)

    @Provides
    @Singleton
    fun provideApkDownloader(
        @ApplicationContext context: Context,
        client: OkHttpClient,
    ): ApkDownloader = ApkDownloader(context, client)

    @Provides
    @Singleton
    fun providePackageInstallerSession(
        @ApplicationContext context: Context,
    ): PackageInstallerSession = PackageInstallerSession(context)
}
