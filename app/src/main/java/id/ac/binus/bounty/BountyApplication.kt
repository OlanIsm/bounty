package id.ac.binus.bounty

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class BountyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!BuildConfig.DEBUG || !BuildConfig.USE_FIREBASE_EMULATORS) return
        FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
        FirebaseFirestore.getInstance().apply {
            useEmulator("10.0.2.2", 8080)
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build()
        }
    }
}
