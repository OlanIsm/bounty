package id.ac.binus.bounty.network;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import id.ac.binus.bounty.models.User;
import id.ac.binus.bounty.utils.SessionManager;

/** Only demo profile fields are parsed; no credentials, account writes, or reward data. */
public class RandomUserResponse {
    public List<Result> results;
    public String error;
    public static class Result { public Name name; public String email; public Picture picture; public Login login; }
    public static class Name { public String first; public String last; }
    public static class Picture { public String large; }
    public static class Login { public String uuid; }

    public List<User> toUsers() {
        List<User> users = new ArrayList<>();
        if (error != null || results == null) return users;
        for (Result result : results) {
            if (result == null || result.login == null || result.name == null) continue;
            String id = text(result.login.uuid);
            String name = (text(result.name.first) + " " + text(result.name.last)).trim();
            String email = text(result.email);
            if (id.isEmpty() || name.isEmpty() || !SessionManager.isValidEmail(email)) continue;
            String avatar = result.picture == null ? "" : text(result.picture.large);
            try {
                URI uri = new URI(avatar);
                if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) avatar = "";
            } catch (URISyntaxException exception) { avatar = ""; }
            users.add(new User(id, name, email, avatar, 0));
            if (users.size() == 10) break;
        }
        return users;
    }
    private static String text(String value) { return value == null ? "" : value.trim(); }
}
