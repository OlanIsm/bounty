package id.ac.binus.bounty.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButtonToggleGroup
import id.ac.binus.bounty.R
import id.ac.binus.bounty.data.FirebaseSocialRepository
import id.ac.binus.bounty.data.SocialUser
import id.ac.binus.bounty.models.User
import id.ac.binus.bounty.network.ApiClient
import id.ac.binus.bounty.network.RandomUserResponse
import id.ac.binus.bounty.utils.ScreenInsets
import id.ac.binus.bounty.utils.SessionManager
import id.ac.binus.bounty.utils.ViewMotion
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FindFriendsActivity : AppCompatActivity() {

    private var allUsers: List<SocialUser> = emptyList()
    private var showOnlyFriends = false
    private var query = ""
    private lateinit var adapter: FriendSearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_friends)
        ScreenInsets.applyHeroEdgeToEdge(this, findViewById(R.id.find_friends_sheet))
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        val currentUser = SessionManager(this).getCurrentUser()
        if (currentUser == null) {
            finish()
            return
        }

        ViewMotion.applySpringPress(findViewById(R.id.btn_find_friends_back)) { finish() }

        val list = findViewById<RecyclerView>(R.id.find_friends_list)
        list.layoutManager = LinearLayoutManager(this)
        adapter = FriendSearchAdapter(
            isFriendCheck = { FirebaseSocialRepository.isFriend(this, it.id) },
            onToggleFriend = { user ->
                FirebaseSocialRepository.toggleFriend(this, currentUser.id, user) { nowFriend ->
                    val msg = if (nowFriend) "Berteman dengan ${user.name}!" else "Batal berteman dengan ${user.name}"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    applyFilter()
                }
            },
            onOpenProfile = { user ->
                startActivity(
                    Intent(this, UserProfileActivity::class.java)
                        .putExtra(UserProfileActivity.EXTRA_USER_ID, user.id)
                        .putExtra(UserProfileActivity.EXTRA_USER_NAME, user.name)
                        .putExtra(UserProfileActivity.EXTRA_USER_EMAIL, user.email)
                        .putExtra(UserProfileActivity.EXTRA_USER_AVATAR, user.avatarUrl)
                )
            }
        )
        list.adapter = adapter

        findViewById<EditText>(R.id.find_friends_search_input).doAfterTextChanged { text ->
            query = text?.toString().orEmpty().trim()
            applyFilter()
        }

        findViewById<MaterialButtonToggleGroup>(R.id.find_friends_toggle)
            .addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (!isChecked) return@addOnButtonCheckedListener
                showOnlyFriends = checkedId == R.id.tab_my_friends
                applyFilter()
            }

        loadUsers(currentUser.id)
    }

    override fun onResume() {
        super.onResume()
        if (allUsers.isNotEmpty()) applyFilter()
    }

    private fun loadUsers(currentUid: String) {
        val loading = findViewById<View>(R.id.find_friends_loading)
        loading.visibility = View.VISIBLE

        FirebaseSocialRepository.syncFriendsFromCloud(this, currentUid) {
            ApiClient.getApi().getUsers(10).enqueue(object : Callback<RandomUserResponse> {
                override fun onResponse(call: Call<RandomUserResponse>, response: Response<RandomUserResponse>) {
                    val demo = if (response.isSuccessful) response.body()?.toUsers().orEmpty() else emptyList()
                    finishLoad(currentUid, demo)
                }

                override fun onFailure(call: Call<RandomUserResponse>, t: Throwable) {
                    finishLoad(currentUid, emptyList())
                }
            })
        }
    }

    private fun finishLoad(currentUid: String, demoUsers: List<User>) {
        if (isDestroyed || isFinishing) return
        FirebaseSocialRepository.loadDiscoverableUsers(this, currentUid, demoUsers) { users ->
            if (isDestroyed || isFinishing) return@loadDiscoverableUsers
            findViewById<View>(R.id.find_friends_loading).visibility = View.GONE
            allUsers = users
            applyFilter()
        }
    }

    private fun applyFilter() {
        val followedIds = FirebaseSocialRepository.getFollowedIds(this)
        val base = if (showOnlyFriends) {
            val saved = FirebaseSocialRepository.getSavedFriends(this)
            val map = LinkedHashMap<String, SocialUser>()
            saved.forEach { map[it.id] = it }
            allUsers.filter { followedIds.contains(it.id) }.forEach { map.putIfAbsent(it.id, it) }
            map.values.toList()
        } else {
            allUsers
        }

        val filtered = if (query.isEmpty()) base else base.filter {
            it.name.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true)
        }
        adapter.submit(filtered)
        findViewById<View>(R.id.find_friends_empty).visibility =
            if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private class FriendSearchAdapter(
        private val isFriendCheck: (SocialUser) -> Boolean,
        private val onToggleFriend: (SocialUser) -> Unit,
        private val onOpenProfile: (SocialUser) -> Unit
    ) : RecyclerView.Adapter<FriendSearchAdapter.Holder>() {

        private val items = mutableListOf<SocialUser>()

        fun submit(newItems: List<SocialUser>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val avatar: ImageView = view.findViewById(R.id.friend_row_avatar)
            val name: TextView = view.findViewById(R.id.friend_row_name)
            val subtitle: TextView = view.findViewById(R.id.friend_row_subtitle)
            val action: TextView = view.findViewById(R.id.friend_row_action)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_friend_row, parent, false)
        )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val user = items[position]
            holder.name.text = user.name
            holder.subtitle.text = user.email.ifBlank { "Bounty Hunter" }
            Glide.with(holder.avatar)
                .load(user.avatarUrl)
                .placeholder(R.drawable.ic_avatar)
                .error(R.drawable.ic_avatar)
                .circleCrop()
                .into(holder.avatar)

            val isFriend = isFriendCheck(user)
            if (isFriend) {
                holder.action.setText(R.string.friend_added_action)
                holder.action.setBackgroundResource(R.drawable.bg_btn_duolingo_3d_green)
            } else {
                holder.action.setText(R.string.add_friend_action)
                holder.action.setBackgroundResource(R.drawable.bg_btn_duolingo_3d_primary)
            }

            ViewMotion.applySpringPress(holder.itemView) { onOpenProfile(user) }
            ViewMotion.applySpringPress(holder.action) { onToggleFriend(user) }
        }

        override fun getItemCount() = items.size
    }
}
