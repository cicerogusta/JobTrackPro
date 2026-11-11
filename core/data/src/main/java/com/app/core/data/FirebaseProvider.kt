package com.app.core.data

import android.content.Context
import com.app.core.model.AppException
import com.app.core.model.Failure
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseProvider @Inject constructor(@ApplicationContext context: Context) {
    private val emulators = BuildConfig.DEBUG && BuildConfig.FIREBASE_EMULATORS
    private val app: FirebaseApp? = if (emulators) {
        FirebaseApp.getApps(context).firstOrNull { it.name == LOCAL_APP } ?: FirebaseApp.initializeApp(context,
            FirebaseOptions.Builder().setProjectId("demo-jobtrackpro")
                .setApplicationId("1:1234567890:android:local-emulator")
                .setApiKey("fake-api-key-for-emulator-only").build(), LOCAL_APP)
    } else FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
        ?: FirebaseApp.initializeApp(context)

    init {
        if (emulators) synchronized(emulatorLock) {
            if (!emulatorsConnected) {
                auth.useEmulator("127.0.0.1", 9099)
                firestore.useEmulator("127.0.0.1", 8080)
                emulatorsConnected = true
            }
        }
    }
    val configured: Boolean get() = app != null
    val auth: FirebaseAuth get() = FirebaseAuth.getInstance(app ?: throw AppException(Failure.CONFIGURATION))
    val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance(app ?: throw AppException(Failure.CONFIGURATION))

    private companion object {
        const val LOCAL_APP = "jobtrack-local"
        val emulatorLock = Any()
        var emulatorsConnected = false
    }
}
