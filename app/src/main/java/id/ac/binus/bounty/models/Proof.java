package id.ac.binus.bounty.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "proofs", indices = @Index("challengeId"), foreignKeys = @ForeignKey(
        entity = Challenge.class, parentColumns = "id", childColumns = "challengeId",
        onDelete = ForeignKey.CASCADE))
public class Proof {
    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    @PrimaryKey(autoGenerate = true)
    public int id;
    public int challengeId;
    @NonNull public String hunterId = "";
    @NonNull public String description = "";
    @NonNull public String imageUri = "";
    @NonNull public String status = PENDING;
    public long submittedAt;
}
