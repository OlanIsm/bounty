package id.ac.binus.bounty.models;

/** Local mock account or read-only API demo profile; no credentials or real payment data. */
public class User {
    public final String id;
    public final String name;
    public final String email;
    public final String avatarUrl;
    public final double demoBalance;

    public User(String id, String name, String email, String avatarUrl, double demoBalance) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.demoBalance = demoBalance;
    }
}
