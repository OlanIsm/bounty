package id.ac.binus.bounty.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Patterns
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.JsonParseException
import id.ac.binus.bounty.database.AppDatabase
import id.ac.binus.bounty.models.Challenge
import id.ac.binus.bounty.models.User
import java.util.Locale
import java.util.UUID

class SessionManager(context: Context) {
    private val accounts: SharedPreferences = context.getSharedPreferences("bounty_accounts", Context.MODE_PRIVATE)
    private val session: SharedPreferences = context.getSharedPreferences("bounty_session", Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        if (!accounts.contains(DEMO_EMAIL)) {
            val demo = User("local_user_001", "Insan", DEMO_EMAIL, "", 50000.0)
            accounts.edit().putString(DEMO_EMAIL, gson.toJson(demo)).apply()
        }
    }

    fun register(name: String?, email: String?): Boolean {
        val normalizedEmail = normalizeEmail(email)
        val normalizedName = name?.trim().orEmpty()
        if (normalizedName.isEmpty() || normalizedName.length > 100 || !isValidEmail(normalizedEmail)) {
            throw IllegalArgumentException("Name and valid email are required")
        }
        if (accounts.contains(normalizedEmail)) return false
        val user = User(UUID.randomUUID().toString(), normalizedName, normalizedEmail, "", 50000.0)
        accounts.edit().putString(normalizedEmail, gson.toJson(user)).apply()
        return true
    }

    fun login(email: String?): Boolean {
        val normalizedEmail = normalizeEmail(email)
        if (!isValidEmail(normalizedEmail) || getUser(normalizedEmail) == null) return false
        session.edit().putString("user_email", normalizedEmail).putBoolean("logged_in", true).apply()
        return true
    }

    fun getCurrentUser(): User? {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            val email = normalizeEmail(firebaseUser.email)
            val cached = getUser(email)
            return if (session.getBoolean("logged_in", false) && cached != null && firebaseUser.uid == cached.id) cached else null
        }
        return if (session.getBoolean("logged_in", false)) getUser(session.getString("user_email", "")) else null
    }

    fun cacheFirebaseUser(uid: String?, name: String?, email: String?) {
        val normalizedEmail = normalizeEmail(email)
        val normalizedName = name?.trim().orEmpty()
        if (uid.isNullOrBlank() || normalizedName.isEmpty() || !isValidEmail(normalizedEmail)) {
            throw IllegalArgumentException("Invalid Firebase account profile")
        }
        val previous = getUser(normalizedEmail)
        val balance = if (previous != null && uid == previous.id) previous.demoBalance else 50000.0
        val user = User(uid, normalizedName, normalizedEmail, "", balance)
        accounts.edit().putString(normalizedEmail, gson.toJson(user)).apply()
        session.edit().putString("user_email", normalizedEmail).putBoolean("logged_in", true).apply()
    }

    fun updateDemoBalance(uid: String, balance: Double) {
        val email = session.getString("user_email", "") ?: return
        val user = getUser(email)?.takeIf { it.id == uid } ?: return
        accounts.edit().putString(email, gson.toJson(User(user.id, user.name, user.email, user.avatarUrl, balance))).apply()
    }

    fun isLoggedIn() = getCurrentUser() != null

    fun isFirebaseAuthenticated(): Boolean {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return false
        val email = firebaseUser.email ?: return false
        val user = getUser(normalizeEmail(email))
        return session.getBoolean("logged_in", false) && user != null && firebaseUser.uid == user.id
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        session.edit().clear().apply()
    }

    fun creditCompletedRewards(db: AppDatabase, hunterId: String): Double = synchronized(REWARD_LOCK) {
        var hunter: User? = null
        for (key in accounts.all.keys) {
            if (isValidEmail(key)) {
                val candidate = getUser(key)
                if (candidate != null && candidate.id == hunterId) {
                    hunter = candidate
                    break
                }
            }
        }
        val account = hunter ?: throw IllegalStateException("Hunter account unavailable")
        var balance = account.demoBalance
        if (!balance.isFinite() || balance < 0) throw IllegalStateException("Invalid demo balance")
        val payment = accounts.edit()
        for (challenge in db.challengeDao().getRewardableChallenges(hunterId)) {
            val receipt = "reward_${challenge.id}"
            if (accounts.contains(receipt)) continue
            val next = balance + challenge.reward
            if (!challenge.reward.isFinite() || challenge.reward <= 0 || !next.isFinite() || next <= balance) {
                throw IllegalStateException("Invalid demo reward")
            }
            balance = next
            payment.putString(receipt, hunterId)
        }
        val credited = balance - account.demoBalance
        val updated = User(account.id, account.name, account.email, account.avatarUrl, balance)
        payment.putString(account.email, gson.toJson(updated))
        if (!payment.commit()) throw IllegalStateException("Unable to persist demo reward")
        credited
    }

    fun isRewardCredited(challengeId: Int, hunterId: String?): Boolean =
        hunterId != null && hunterId == accounts.getString("reward_$challengeId", "")

    private fun getUser(email: String?): User? {
        return try {
            val user = gson.fromJson(accounts.getString(email, ""), User::class.java)
            @Suppress("SENSELESS_COMPARISON")
            user?.takeIf { it.id != null && it.name != null && email == it.email }
        } catch (_: JsonParseException) {
            null
        }
    }

    companion object {
        const val DEMO_EMAIL = "demo@bounty.local"
        private val REWARD_LOCK = Any()

        @JvmStatic fun normalizeEmail(email: String?): String = email?.trim()?.lowercase(Locale.ROOT).orEmpty()

        @JvmStatic fun isValidEmail(email: String?): Boolean {
            val normalized = normalizeEmail(email)
            return normalized.length <= 254 && Patterns.EMAIL_ADDRESS.matcher(normalized).matches()
        }
    }
}
