package id.ac.binus.bounty.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ListenerRegistration
import id.ac.binus.bounty.R
import id.ac.binus.bounty.data.ChatMessage
import id.ac.binus.bounty.data.FirebaseSocialRepository
import id.ac.binus.bounty.data.SocialUser
import id.ac.binus.bounty.models.User
import id.ac.binus.bounty.utils.ScreenInsets
import id.ac.binus.bounty.utils.SessionManager
import id.ac.binus.bounty.utils.ViewMotion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatRoomActivity : AppCompatActivity() {

    private lateinit var currentUser: User
    private lateinit var friend: SocialUser
    private lateinit var adapter: MessageAdapter
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)
        ScreenInsets.applyHeroEdgeToEdge(this, findViewById(R.id.chat_composer_bar))
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        val me = SessionManager(this).getCurrentUser()
        val friendId = intent.getStringExtra(UserProfileActivity.EXTRA_USER_ID).orEmpty()
        if (me == null || friendId.isBlank()) {
            finish()
            return
        }
        currentUser = me
        friend = SocialUser(
            id = friendId,
            name = intent.getStringExtra(UserProfileActivity.EXTRA_USER_NAME).orEmpty().ifBlank { "Teman Bounty" },
            email = intent.getStringExtra(UserProfileActivity.EXTRA_USER_EMAIL).orEmpty(),
            avatarUrl = intent.getStringExtra(UserProfileActivity.EXTRA_USER_AVATAR).orEmpty()
        )

        findViewById<TextView>(R.id.chat_room_friend_name).text = friend.name
        ViewMotion.applySpringPress(findViewById(R.id.btn_chat_room_back)) { finishWithSlide() }
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWithSlide()
            }
        })

        val openFriendProfile = {
            startActivity(
                Intent(this, UserProfileActivity::class.java)
                    .putExtra(UserProfileActivity.EXTRA_USER_ID, friend.id)
                    .putExtra(UserProfileActivity.EXTRA_USER_NAME, friend.name)
                    .putExtra(UserProfileActivity.EXTRA_USER_EMAIL, friend.email)
                    .putExtra(UserProfileActivity.EXTRA_USER_AVATAR, friend.avatarUrl)
            )
        }
        ViewMotion.applySpringPress(findViewById(R.id.btn_chat_room_profile)) { openFriendProfile() }
        ViewMotion.applySpringPress(findViewById(R.id.chat_room_header_profile)) { openFriendProfile() }

        val list = findViewById<RecyclerView>(R.id.chat_messages_list)
        val layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        list.layoutManager = layoutManager
        adapter = MessageAdapter(currentUser.id, friend.avatarUrl)
        list.adapter = adapter

        val input = findViewById<EditText>(R.id.chat_message_input)
        val send = {
            val text = input.text?.toString().orEmpty().trim()
            if (text.isNotEmpty()) {
                input.setText("")
                FirebaseSocialRepository.sendMessage(this, currentUser, friend, text) { updated ->
                    renderMessages(updated, list)
                }
            }
        }
        ViewMotion.applySpringPress(findViewById(R.id.btn_chat_send)) { send() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                send()
                true
            } else {
                false
            }
        }

        val roomId = FirebaseSocialRepository.chatRoomId(currentUser.id, friend.id)
        listener = FirebaseSocialRepository.listenMessages(this, roomId) { messages ->
            if (isDestroyed || isFinishing) return@listenMessages
            renderMessages(messages, list)
        }
    }

    private fun renderMessages(messages: List<ChatMessage>, list: RecyclerView) {
        adapter.submit(messages)
        findViewById<View>(R.id.chat_room_empty).visibility =
            if (messages.isEmpty()) View.VISIBLE else View.GONE
        if (messages.isNotEmpty()) {
            list.scrollToPosition(messages.size - 1)
        }
    }

    private fun finishWithSlide() {
        finish()
        ViewMotion.applySlideFromRight(this, opening = false)
    }

    override fun onDestroy() {
        listener?.remove()
        super.onDestroy()
    }

    private class MessageAdapter(
        private val currentUid: String,
        private val friendAvatar: String
    ) : RecyclerView.Adapter<MessageAdapter.Holder>() {

        private val items = mutableListOf<ChatMessage>()
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun submit(newItems: List<ChatMessage>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val sentContainer: View = view.findViewById(R.id.msg_sent_container)
            val sentText: TextView = view.findViewById(R.id.msg_sent_text)
            val sentTime: TextView = view.findViewById(R.id.msg_sent_time)
            val receivedContainer: View = view.findViewById(R.id.msg_received_container)
            val receivedAvatar: ImageView = view.findViewById(R.id.msg_received_avatar)
            val receivedText: TextView = view.findViewById(R.id.msg_received_text)
            val receivedTime: TextView = view.findViewById(R.id.msg_received_time)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val msg = items[position]
            val formattedTime = timeFormat.format(Date(msg.timestamp))
            if (msg.senderId == currentUid) {
                holder.sentContainer.visibility = View.VISIBLE
                holder.receivedContainer.visibility = View.GONE
                holder.sentText.text = msg.text
                holder.sentTime.text = formattedTime
            } else {
                holder.sentContainer.visibility = View.GONE
                holder.receivedContainer.visibility = View.VISIBLE
                holder.receivedText.text = msg.text
                holder.receivedTime.text = formattedTime
                Glide.with(holder.receivedAvatar)
                    .load(friendAvatar)
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .circleCrop()
                    .into(holder.receivedAvatar)
            }
        }

        override fun getItemCount() = items.size
    }
}
