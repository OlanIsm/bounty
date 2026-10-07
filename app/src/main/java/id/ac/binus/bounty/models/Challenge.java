package id.ac.binus.bounty.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "challenges")
public class Challenge {
    public static final String OPEN = "OPEN";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String SUBMITTED = "SUBMITTED";
    public static final String COMPLETED = "COMPLETED";

    @PrimaryKey(autoGenerate = true)
    public int id;
    @NonNull public String creatorId = "";
    @NonNull public String creatorName = "";
    @NonNull public String creatorAvatar = "";
    @NonNull public String title = "";
    @NonNull public String description = "";
    public double reward;
    @NonNull public String deadline = "";
    @NonNull public String status = OPEN;
    public String participantId;
    public String participantName;
    public long createdAt;
}
