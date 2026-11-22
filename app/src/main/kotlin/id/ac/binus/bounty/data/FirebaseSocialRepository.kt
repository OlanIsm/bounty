package id.ac.binus.bounty.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.ac.binus.bounty.models.Challenge
import id.ac.binus.bounty.models.User

data class SocialUser(
    val id: String,
    val name: String,
    val email: String = "",
    val avatarUrl: String = ""
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long
)

data class ChatThread(
    val friend: SocialUser,
    val lastMessage: String,
    val lastTimestamp: Long
)

/**
 * Handles friends/follows, user directory search, and friend chat.
 * Uses Firestore when permitted and mirrors to SharedPreferences so social flows
 * remain responsive even offline or before Firestore rules are redeployed.
 */
object FirebaseSocialRepository {
    private const val PREFS_FOLLOWS = "bounty_follows"
    private const val KEY_FOLLOWED_IDS = "followed_ids"
    private const val KEY_FRIEND_PROFILES = "friend_profiles_json"
    private const val PREFS_CHATS = "bounty_chats_cache"
    private val gson = Gson()
    private val firestore get() = FirebaseFirestore.getInstance()

    fun chatRoomId(uidA: String, uidB: String): String =
        if (uidA <= uidB) "${uidA}_${uidB}" else "${uidB}_${uidA}"

    fun getFollowedIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_FOLLOWS, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_FOLLOWED_IDS, emptySet())?.toSet() ?: emptySet()
    }

    fun isFriend(context: Context, targetId: String): Boolean =
        getFollowedIds(context).contains(targetId)

    fun getSavedFriends(context: Context): List<SocialUser> {
        val prefs = context.getSharedPreferences(PREFS_FOLLOWS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_FRIEND_PROFILES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SocialUser>>() {}.type
            gson.fromJson<List<SocialUser>>(raw, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveFriendProfileLocally(context: Context, user: SocialUser, add: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_FOLLOWS, Context.MODE_PRIVATE)
        val ids = getFollowedIds(context).toMutableSet()
        val list = getSavedFriends(context).filter { it.id != user.id }.toMutableList()
        if (add) {
            ids.add(user.id)
            list.add(0, user)
        } else {
            ids.remove(user.id)
        }
        prefs.edit()
            .putStringSet(KEY_FOLLOWED_IDS, ids)
            .putString(KEY_FRIEND_PROFILES, gson.toJson(list))
            .apply()
    }

    fun toggleFriend(
        context: Context,
        currentUid: String,
        target: SocialUser,
        callback: (Boolean) -> Unit
    ) {
        val nowFriend = !isFriend(context, target.id)
        saveFriendProfileLocally(context, target, nowFriend)

        val friendDoc = firestore.collection("users").document(currentUid)
            .collection("friends").document(target.id)
        if (nowFriend) {
            friendDoc.set(
                mapOf(
                    "uid" to target.id,
                    "name" to target.name,
                    "email" to target.email,
                    "avatarUrl" to target.avatarUrl,
                    "addedAt" to FieldValue.serverTimestamp()
                )
            ).addOnFailureListener {
                Log.w("BountySocial", "Friend cloud sync skipped: ${it.message}")
            }
        } else {
            friendDoc.delete().addOnFailureListener {
                Log.w("BountySocial", "Friend cloud removal skipped: ${it.message}")
            }
        }
        callback(nowFriend)
    }

    fun syncFriendsFromCloud(context: Context, currentUid: String, onDone: (List<SocialUser>) -> Unit) {
        firestore.collection("users").document(currentUid).collection("friends").get()
            .addOnSuccessListener { snap ->
                val cloudFriends = snap.documents.mapNotNull { doc ->
                    val id = doc.getString("uid") ?: doc.id
                    val name = doc.getString("name") ?: return@mapNotNull null
                    SocialUser(
                        id = id,
                        name = name,
                        email = doc.getString("email").orEmpty(),
                        avatarUrl = doc.getString("avatarUrl").orEmpty()
                    )
                }
                if (cloudFriends.isNotEmpty()) {
                    val prefs = context.getSharedPreferences(PREFS_FOLLOWS, Context.MODE_PRIVATE)
                    val mergedMap = LinkedHashMap<String, SocialUser>()
                    cloudFriends.forEach { mergedMap[it.id] = it }
                    getSavedFriends(context).forEach { mergedMap.putIfAbsent(it.id, it) }
                    val merged = mergedMap.values.toList()
                    val mergedIds = getFollowedIds(context) + merged.map { it.id }
                    prefs.edit()
                        .putStringSet(KEY_FOLLOWED_IDS, mergedIds)
                        .putString(KEY_FRIEND_PROFILES, gson.toJson(merged))
                        .apply()
                    onDone(merged)
                } else {
                    onDone(getSavedFriends(context))
                }
            }
            .addOnFailureListener {
                onDone(getSavedFriends(context))
            }
    }

    /**
     * Discovers users from Firestore users collection, active challenges, and saved friends.
     */
    fun loadDiscoverableUsers(
        context: Context,
        currentUid: String,
        demoUsers: List<User>,
        callback: (List<SocialUser>) -> Unit
    ) {
        val collected = LinkedHashMap<String, SocialUser>()
        getSavedFriends(context).forEach { if (it.id != currentUid) collected[it.id] = it }
        demoUsers.forEach { u ->
            if (u.id != currentUid && u.id != "api_fallback") {
                collected.putIfAbsent(u.id, SocialUser(u.id, u.name, u.email, u.avatarUrl))
            }
        }

        // Also pull real users from challenges (always readable by signed-in users)
        FirebaseBountyRepository.loadChallenges(currentUid, false) { challengeResult ->
            challengeResult.onSuccess { list ->
                extractUsersFromChallenges(list, currentUid).forEach { u ->
                    collected[u.id] = u
                }
            }
            // Try Firestore users collection (succeeds when updated rules are deployed)
            firestore.collection("users").limit(50).get()
                .addOnSuccessListener { snap ->
                    snap.documents.forEach { doc ->
                        val uid = doc.id
                        if (uid != currentUid) {
                            val name = doc.getString("displayName").orEmpty()
                            if (name.isNotBlank()) {
                                collected[uid] = SocialUser(
                                    id = uid,
                                    name = name,
                                    email = doc.getString("email").orEmpty(),
                                    avatarUrl = doc.getString("avatarUrl").orEmpty()
                                )
                            }
                        }
                    }
                    callback(collected.values.toList())
                }
                .addOnFailureListener {
                    callback(collected.values.toList())
                }
        }
    }

    private fun extractUsersFromChallenges(challenges: List<Challenge>, excludeUid: String): List<SocialUser> {
        val map = LinkedHashMap<String, SocialUser>()
        challenges.forEach { c ->
            if (c.creatorId.isNotBlank() && c.creatorId != excludeUid) {
                map.putIfAbsent(
                    c.creatorId,
                    SocialUser(c.creatorId, c.creatorName.ifBlank { "Creator" }, "", c.creatorAvatar)
                )
            }
            val pid = c.participantId
            if (!pid.isNullOrBlank() && pid != excludeUid) {
                map.putIfAbsent(
                    pid,
                    SocialUser(pid, c.participantName?.ifBlank { "Hunter" } ?: "Hunter", "", "")
                )
            }
        }
        return map.values.toList()
    }

    fun getLocalMessages(context: Context, roomId: String): List<ChatMessage> {
        val prefs = context.getSharedPreferences(PREFS_CHATS, Context.MODE_PRIVATE)
        val raw = prefs.getString("room_$roomId", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ChatMessage>>() {}.type
            gson.fromJson<List<ChatMessage>>(raw, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveLocalMessages(context: Context, roomId: String, messages: List<ChatMessage>) {
        val prefs = context.getSharedPreferences(PREFS_CHATS, Context.MODE_PRIVATE)
        prefs.edit().putString("room_$roomId", gson.toJson(messages.takeLast(100))).apply()
    }

    fun listenMessages(
        context: Context,
        roomId: String,
        onUpdate: (List<ChatMessage>) -> Unit
    ): ListenerRegistration {
        val local = getLocalMessages(context, roomId)
        if (local.isNotEmpty()) onUpdate(local)

        return firestore.collection("chats").document(roomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) {
                    Log.w("BountyChat", "Cloud chat listener fallback: ${error?.message}")
                    onUpdate(getLocalMessages(context, roomId))
                    return@addSnapshotListener
                }
                val cloud = snap.documents.map { doc ->
                    ChatMessage(
                        id = doc.id,
                        senderId = doc.getString("senderId").orEmpty(),
                        senderName = doc.getString("senderName").orEmpty(),
                        text = doc.getString("text").orEmpty(),
                        timestamp = doc.getTimestamp("timestamp")?.toDate()?.time
                            ?: doc.getLong("localTimestamp") ?: System.currentTimeMillis()
                    )
                }
                if (cloud.isNotEmpty()) {
                    saveLocalMessages(context, roomId, cloud)
                    onUpdate(cloud)
                } else {
                    onUpdate(getLocalMessages(context, roomId))
                }
            }
    }

    fun sendMessage(
        context: Context,
        currentUser: User,
        friend: SocialUser,
        text: String,
        onDone: (List<ChatMessage>) -> Unit
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val roomId = chatRoomId(currentUser.id, friend.id)
        val now = System.currentTimeMillis()
        val msg = ChatMessage(
            id = "local_${now}",
            senderId = currentUser.id,
            senderName = currentUser.name,
            text = trimmed,
            timestamp = now
        )
        // Ensure friend is saved locally so thread appears in ChatList
        if (!isFriend(context, friend.id)) {
            saveFriendProfileLocally(context, friend, true)
        }
        val updated = getLocalMessages(context, roomId) + msg
        saveLocalMessages(context, roomId, updated)
        onDone(updated)

        val roomRef = firestore.collection("chats").document(roomId)
        roomRef.set(
            mapOf(
                "participants" to listOf(currentUser.id, friend.id),
                "lastMessage" to trimmed,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).addOnFailureListener {
            Log.w("BountyChat", "Chat room metadata cloud sync skipped: ${it.message}")
        }
        roomRef.collection("messages").add(
            mapOf(
                "senderId" to currentUser.id,
                "senderName" to currentUser.name,
                "text" to trimmed,
                "localTimestamp" to now,
                "timestamp" to FieldValue.serverTimestamp()
            )
        ).addOnFailureListener {
            Log.w("BountyChat", "Chat message cloud sync skipped: ${it.message}")
        }
    }

    fun buildChatThreads(context: Context, currentUid: String, friends: List<SocialUser>): List<ChatThread> {
        return friends.map { friend ->
            val roomId = chatRoomId(currentUid, friend.id)
            val msgs = getLocalMessages(context, roomId)
            val last = msgs.lastOrNull()
            ChatThread(
                friend = friend,
                lastMessage = last?.text ?: "Mulai obrolan dengan ${friend.name}",
                lastTimestamp = last?.timestamp ?: 0L
            )
        }.sortedByDescending { it.lastTimestamp }
    }
}
