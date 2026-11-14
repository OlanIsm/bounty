package id.ac.binus.bounty.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "challenges")
class Challenge {
    @field:PrimaryKey(autoGenerate = true)
    @JvmField var id = 0
    @JvmField var creatorId = ""
    @JvmField var creatorName = ""
    @JvmField var creatorAvatar = ""
    @JvmField var title = ""
    @JvmField var description = ""
    @JvmField var reward = 0.0
    @JvmField var deadline = ""
    @JvmField var status = OPEN
    @JvmField var participantId: String? = null
    @JvmField var participantName: String? = null
    @JvmField var createdAt = 0L
    @Ignore @JvmField var cloudId: String? = null
    @Ignore @JvmField var latestProofId: String? = null

    companion object {
        const val OPEN = "OPEN"
        const val ACCEPTED = "ACCEPTED"
        const val SUBMITTED = "SUBMITTED"
        const val COMPLETED = "COMPLETED"
    }
}
