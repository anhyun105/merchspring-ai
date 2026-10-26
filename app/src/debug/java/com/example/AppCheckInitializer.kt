package com.example

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

internal object AppCheckInitializer {
    fun install(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) return

        runCatching {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        }.onFailure { error ->
            Log.e("MerchSpring", "Unable to initialize debug Firebase App Check", error)
        }
    }
}
