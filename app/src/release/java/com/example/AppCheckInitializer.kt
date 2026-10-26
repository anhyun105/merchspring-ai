package com.example

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

internal object AppCheckInitializer {
    fun install(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) return

        runCatching {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }.onFailure { error ->
            Log.e("MerchSpring", "Unable to initialize release Firebase App Check", error)
        }
    }
}
