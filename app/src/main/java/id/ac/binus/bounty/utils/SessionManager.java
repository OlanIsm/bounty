package id.ac.binus.bounty.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Patterns;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.util.Locale;
import java.util.UUID;

import id.ac.binus.bounty.models.User;
import id.ac.binus.bounty.models.Challenge;
import id.ac.binus.bounty.database.AppDatabase;

/** Mock email-only authentication. Never use this as production authentication. */
public class SessionManager {
    public static final String DEMO_EMAIL = "demo@bounty.local";
    // ponytail: one-process local demo; use a transactional ledger for a backend/multi-process app.
    private static final Object REWARD_LOCK = new Object();
    private final SharedPreferences accounts;
    private final SharedPreferences session;
    private final Gson gson = new Gson();

    public SessionManager(Context context) {
        accounts = context.getSharedPreferences("bounty_accounts", Context.MODE_PRIVATE);
        session = context.getSharedPreferences("bounty_session", Context.MODE_PRIVATE);
        if (!accounts.contains(DEMO_EMAIL)) {
            User demo = new User("local_user_001", "Insan", DEMO_EMAIL, "", 50000);
            accounts.edit().putString(DEMO_EMAIL, gson.toJson(demo)).apply();
        }
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValidEmail(String email) {
        String normalized = normalizeEmail(email);
        return normalized.length() <= 254 && Patterns.EMAIL_ADDRESS.matcher(normalized).matches();
    }

    /** Returns false for a duplicate email; invalid input is rejected at this boundary. */
    public boolean register(String name, String email) {
        email = normalizeEmail(email);
        if (name == null || name.trim().isEmpty() || name.trim().length() > 100
                || !isValidEmail(email)) {
            throw new IllegalArgumentException("Name and valid email are required");
        }
        if (accounts.contains(email)) {
            return false;
        }
        User user = new User(UUID.randomUUID().toString(), name.trim(), email, "", 50000);
        accounts.edit().putString(email, gson.toJson(user)).apply();
        return true;
    }

    public boolean login(String email) {
        email = normalizeEmail(email);
        if (!isValidEmail(email) || getUser(email) == null) {
            return false;
        }
        session.edit().putString("user_email", email).putBoolean("logged_in", true).apply();
        return true;
    }

    public User getCurrentUser() {
        return session.getBoolean("logged_in", false)
                ? getUser(session.getString("user_email", "")) : null;
    }

    public boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    public void logout() {
        session.edit().clear().apply();
    }

    /** Off-main-thread recovery: credit each completed/approved challenge once, including after restart. */
    public double creditCompletedRewards(AppDatabase db, String hunterId) {
        synchronized (REWARD_LOCK) {
            User hunter = null;
            for (String key : accounts.getAll().keySet()) {
                if (isValidEmail(key)) {
                    User candidate = getUser(key);
                    if (candidate != null && candidate.id.equals(hunterId)) { hunter = candidate; break; }
                }
            }
            if (hunter == null) throw new IllegalStateException("Hunter account unavailable");
            double balance = hunter.demoBalance;
            if (!Double.isFinite(balance) || balance < 0) throw new IllegalStateException("Invalid demo balance");
            SharedPreferences.Editor payment = accounts.edit();
            for (Challenge challenge : db.challengeDao().getRewardableChallenges(hunterId)) {
                String receipt = "reward_" + challenge.id;
                if (accounts.contains(receipt)) continue;
                double next = balance + challenge.reward;
                if (!Double.isFinite(challenge.reward) || challenge.reward <= 0
                        || !Double.isFinite(next) || next <= balance) {
                    throw new IllegalStateException("Invalid demo reward");
                }
                balance = next;
                payment.putString(receipt, hunterId);
            }
            double credited = balance - hunter.demoBalance;
            User updated = new User(hunter.id, hunter.name, hunter.email, hunter.avatarUrl, balance);
            payment.putString(hunter.email, gson.toJson(updated));
            // Also retry a failed disk commit whose snapshot is already visible in memory.
            // Balance and receipts share one snapshot; never write inside the Room transaction.
            if (!payment.commit()) throw new IllegalStateException("Unable to persist demo reward");
            return credited;
        }
    }

    public boolean isRewardCredited(int challengeId, String hunterId) {
        return hunterId != null && hunterId.equals(accounts.getString("reward_" + challengeId, ""));
    }

    private User getUser(String email) {
        try {
            User user = gson.fromJson(accounts.getString(email, ""), User.class);
            return user != null && user.id != null && user.name != null
                    && email.equals(user.email) ? user : null;
        } catch (JsonParseException exception) {
            return null;
        }
    }
}
