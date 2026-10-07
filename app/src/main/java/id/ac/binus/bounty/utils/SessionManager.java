package id.ac.binus.bounty.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Patterns;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.util.Locale;
import java.util.UUID;

import id.ac.binus.bounty.models.User;

/** Mock email-only authentication. Never use this as production authentication. */
public class SessionManager {
    public static final String DEMO_EMAIL = "demo@bounty.local";
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
