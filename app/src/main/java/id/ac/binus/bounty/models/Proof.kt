package id.ac.binus.bounty.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(
    tableName = "proofs",
    indices = [Index("challengeId")],
    foreignKeys = [ForeignKey(
        entity = Challenge::class,
        parentColumns = ["id"],
        childColumns = ["challengeId"],
        onDelete = ForeignKey.CASCADE
    )]
)
class Proof {
    @field:PrimaryKey(autoGenerate = true)
    @JvmField var id = 0
    @JvmField var challengeId = 0
    @JvmField var hunterId = ""
    @JvmField var description = ""
    @JvmField var imageUri = ""
    @JvmField var status = PENDING
    @JvmField var submittedAt = 0L
    @Ignore @JvmField var cloudId: String? = null
    @Ignore @JvmField var challengeCloudId: String? = null
    @Ignore @JvmField var imageData: ByteArray? = null

    companion object {
        const val PENDING = "PENDING"
        const val APPROVED = "APPROVED"
        const val REJECTED = "REJECTED"
    }
}
