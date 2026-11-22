package id.ac.binus.bounty.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import id.ac.binus.bounty.R
import id.ac.binus.bounty.data.ChatThread
import id.ac.binus.bounty.data.FirebaseSocialRepository
import id.ac.binus.bounty.data.SocialUser
import id.ac.binus.bounty.utils.ScreenInsets
import id.ac.binus.bounty.utils.SessionManager
import id.ac.binus.bounty.utils.ViewMotion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListActivity : AppCompatActivity() {

    private lateinit var threadAdapter: ChatThreadAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)
        ScreenInsets.applyHeroEdgeToEdge(this, findViewById(R.id.chat_list_sheet))
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        val user = SessionManager(this).getCurrentUser()
        if (user == null) {
            finish()
            return
        }

        findViewById<TextView>(R.id.chat_list_greeting).text =
            getString(R.string.welcome_user, user.name)

        ViewMotion.applySpringPress(findViewById(R.id.btn_chat_list_back)) { finishWithSlide() }
        ViewMotion.applySpringPress(findViewById(R.id.btn_chat_find_friends)) { openFindFriends() }
        ViewMotion.applySpringPress(findViewById(R.id.btn_chat_empty_find)) { openFindFriends() }
        findViewById<View>(R.id.fab_new_chat).setOnClickListener { openFindFriends() }

        val list = findViewById<RecyclerView>(R.id.chat_threads_list)
        list.layoutManager = LinearLayoutManager(this)
        threadAdapter = ChatThreadAdapter { friend -> openChatRoom(friend) }
        list.adapter = threadAdapter

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWithSlide()
            }
        })
    }

    private fun finishWithSlide() {
        finish()
        ViewMotion.applySlideFromRight(this, opening = false)
    }

    override fun onResume() {
        super.onResume()
        refreshChats()
    }

    private fun refreshChats() {
        val user = SessionManager(this).getCurrentUser() ?: return
        FirebaseSocialRepository.syncFriendsFromCloud(this, user.id) { friends ->
            if (isDestroyed || isFinishing) return@syncFriendsFromCloud
            val threads = FirebaseSocialRepository.buildChatThreads(this, user.id, friends)
            findViewById<TextView>(R.id.chat_list_count_title).text =
                "${threads.size} Percakapan Teman"
            findViewById<TextView>(R.id.chat_pill_count).text = threads.size.toString()

            val contactsContainer = findViewById<LinearLayout>(R.id.chat_contacts_container)
            contactsContainer.removeAllViews()
            val inflater = LayoutInflater.from(this)
            friends.forEach { friend ->
                val chip = inflater.inflate(R.layout.item_chat_contact, contactsContainer, false)
                chip.findViewById<TextView>(R.id.contact_name).text = friend.name.substringBefore(" ")
                val avatar = chip.findViewById<ImageView>(R.id.contact_avatar)
                Glide.with(avatar)
                    .load(friend.avatarUrl)
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .circleCrop()
                    .into(avatar)
                ViewMotion.applySpringPress(chip) { openChatRoom(friend) }
                contactsContainer.addView(chip)
            }

            threadAdapter.submit(threads)
            val emptyView = findViewById<View>(R.id.chat_list_empty)
            val listView = findViewById<View>(R.id.chat_threads_list)
            if (threads.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                listView.visibility = View.GONE
            } else {
                emptyView.visibility = View.GONE
                listView.visibility = View.VISIBLE
            }
        }
    }

    private fun openFindFriends() {
        startActivity(Intent(this, FindFriendsActivity::class.java))
    }

    private fun openChatRoom(friend: SocialUser) {
        startActivity(
            Intent(this, ChatRoomActivity::class.java)
                .putExtra(UserProfileActivity.EXTRA_USER_ID, friend.id)
                .putExtra(UserProfileActivity.EXTRA_USER_NAME, friend.name)
                .putExtra(UserProfileActivity.EXTRA_USER_EMAIL, friend.email)
                .putExtra(UserProfileActivity.EXTRA_USER_AVATAR, friend.avatarUrl)
        )
        ViewMotion.applySlideFromRight(this, opening = true)
    }

    private class ChatThreadAdapter(
        private val onOpen: (SocialUser) -> Unit
    ) : RecyclerView.Adapter<ChatThreadAdapter.Holder>() {

        private val items = mutableListOf<ChatThread>()
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun submit(newItems: List<ChatThread>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val avatar: ImageView = view.findViewById(R.id.thread_avatar)
            val name: TextView = view.findViewById(R.id.thread_name)
            val preview: TextView = view.findViewById(R.id.thread_preview)
            val time: TextView = view.findViewById(R.id.thread_time)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_chat_thread, parent, false)
        )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val thread = items[position]
            holder.name.text = thread.friend.name
            holder.preview.text = thread.lastMessage
            holder.time.text = if (thread.lastTimestamp > 0L) {
                timeFormat.format(Date(thread.lastTimestamp))
            } else {
                ""
            }
            Glide.with(holder.avatar)
                .load(thread.friend.avatarUrl)
                .placeholder(R.drawable.ic_avatar)
                .error(R.drawable.ic_avatar)
                .circleCrop()
                .into(holder.avatar)

            ViewMotion.applySpringPress(holder.itemView) { onOpen(thread.friend) }
        }

        override fun getItemCount() = items.size
    }
}
