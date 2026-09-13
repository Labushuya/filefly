package de.filefly.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Application-Einstiegspunkt. @HiltAndroidApp bootet den DI-Graph.
@HiltAndroidApp
class FileFlyApplication : Application()
