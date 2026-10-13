package id.ac.binus.bounty.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestore
import id.ac.binus.bounty.R

data class FirebaseAccount(val uid: String, val name: String, val email: String)

object FirebaseAccountRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore get() = FirebaseFirestore.getInstance()

    fun signIn(email: String, password: String, callback: (Result<FirebaseAccount>) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) callback(Result.failure(IllegalStateException("Missing account")))
                else loadOrCreateProfile(user, callback)
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun register(name: String, email: String, password: String, callback: (Result<FirebaseAccount>) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    callback(Result.failure(IllegalStateException("Missing account")))
                    return@addOnSuccessListener
                }
                val request = UserProfileChangeRequest.Builder().setDisplayName(name).build()
                user.updateProfile(request)
                    .addOnSuccessListener { saveProfile(user, name, callback) }
                    .addOnFailureListener { saveProfile(user, name, callback) }
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    private fun loadOrCreateProfile(user: FirebaseUser, callback: (Result<FirebaseAccount>) -> Unit) {
        val reference = firestore.collection("users").document(user.uid)
        reference.get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.getString("displayName")?.takeIf(String::isNotBlank)
                    ?: user.displayName?.takeIf(String::isNotBlank)
                if (name == null) {
                    failAndSignOut(IllegalStateException("Account profile is missing"), callback)
                } else if (snapshot.exists()) {
                    callback(Result.success(FirebaseAccount(user.uid, name, user.email.orEmpty())))
                } else {
                    saveProfile(user, name, callback)
                }
            }
            .addOnFailureListener { failAndSignOut(it, callback) }
    }

    private fun saveProfile(
        user: FirebaseUser,
        name: String,
        callback: (Result<FirebaseAccount>) -> Unit
    ) {
        val email = user.email.orEmpty()
        val profile = mapOf(
            "displayName" to name,
            "email" to email,
            "initialDemoBalance" to 50000,
            "createdAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("users").document(user.uid).set(profile)
            .addOnSuccessListener { callback(Result.success(FirebaseAccount(user.uid, name, email))) }
            .addOnFailureListener { failAndSignOut(it, callback) }
    }

    private fun failAndSignOut(
        error: Exception,
        callback: (Result<FirebaseAccount>) -> Unit
    ) {
        auth.signOut()
        callback(Result.failure(error))
    }

    fun errorMessage(error: Throwable, login: Boolean): Int = when {
        error is FirebaseNetworkException -> R.string.auth_network_error
        error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            R.string.firebase_profile_rules_error
        error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            R.string.auth_network_error
        login && (error is FirebaseAuthException
            || error.message?.contains("INVALID_LOGIN_CREDENTIALS") == true) -> R.string.login_failed
        !login && error is FirebaseAuthException
            && error.errorCode.contains("EMAIL_ALREADY_IN_USE") -> R.string.duplicate_email
        else -> R.string.firebase_profile_error
    }
}
