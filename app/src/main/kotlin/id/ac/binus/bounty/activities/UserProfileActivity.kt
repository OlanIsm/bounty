package id.ac.binus.bounty.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButtonToggleGroup
import id.ac.binus.bounty.R
import id.ac.binus.bounty.adapters.PortfolioChallengeAdapter
import id.ac.binus.bounty.data.FirebaseBountyRepository
import id.ac.binus.bounty.data.FirebaseSocialRepository
import id.ac.binus.bounty.data.SocialUser
import id.ac.binus.bounty.models.Challenge
import id.ac.binus.bounty.utils.ScreenInsets
import id.ac.binus.bounty.utils.SessionManager
import id.ac.binus.bounty.utils.ViewMotion

class UserProfileActivity : AppCompatActivity() {

    private lateinit var targetUser: SocialUser
    private lateinit var portfolioAdapter: PortfolioChallengeAdapter
    private var allUserChallenges: List<Challenge> = emptyList()
    private var selectedTab = TAB_ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
        ScreenInsets.apply(this)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        val uid = intent.getStringExtra(EXTRA_USER_ID).orEmpty()
        val name = intent.getStringExtra(EXTRA_USER_NAME).orEmpty().ifBlank { "Pengguna Bounty" }
        val email = intent.getStringExtra(EXTRA_USER_EMAIL).orEmpty()
        val avatar = intent.getStringExtra(EXTRA_USER_AVATAR).orEmpty()
        if (uid.isBlank()) {
            finish()
            return
        }
        targetUser = SocialUser(uid, name, email, avatar)

        val currentUser = SessionManager(this).getCurrentUser()
        val isOwnProfile = currentUser?.id == uid

        findViewById<TextView>(R.id.other_profile_name).text = targetUser.name
        findViewById<TextView>(R.id.other_profile_subtitle).text =
            targetUser.email.ifBlank { "Bounty Hunter & Creator" }

        val avatarView = findViewById<ImageView>(R.id.other_profile_avatar)
        Glide.with(avatarView)
            .load(targetUser.avatarUrl)
            .placeholder(R.drawable.ic_avatar)
            .error(R.drawable.ic_avatar)
            .circleCrop()
            .into(avatarView)

        ViewMotion.applySpringPress(findViewById(R.id.btn_user_profile_back)) { finish() }

        val actionsRow = findViewById<View>(R.id.other_profile_actions)
        if (isOwnProfile) {
            actionsRow.visibility = View.GONE
        } else {
            updateFriendButton()
            ViewMotion.applySpringPress(findViewById(R.id.btn_other_friend_toggle)) {
                val me = currentUser ?: return@applySpringPress
                FirebaseSocialRepository.toggleFriend(this, me.id, targetUser) { nowFriend ->
                    updateFriendButton()
                    val msg = if (nowFriend) "Berteman dengan ${targetUser.name}!" else "Batal berteman dengan ${targetUser.name}"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            }
            ViewMotion.applySpringPress(findViewById(R.id.btn_other_chat)) {
                openChatWithTarget()
            }
        }

        val list = findViewById<RecyclerView>(R.id.other_portfolio_list)
        list.layoutManager = GridLayoutManager(this, 2)
        portfolioAdapter = PortfolioChallengeAdapter(targetUser.id) { challenge ->
            startActivity(
                Intent(this, ChallengeDetailActivity::class.java)
                    .putExtra(ChallengeDetailActivity.EXTRA_CLOUD_CHALLENGE_ID, challenge.cloudId)
            )
        }
        list.adapter = portfolioAdapter
        list.itemAnimator = null

        val toggle = findViewById<MaterialButtonToggleGroup>(R.id.other_portfolio_toggle)
        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedTab = when (checkedId) {
                R.id.other_tab_joined -> TAB_JOINED
                R.id.other_tab_created -> TAB_CREATED
                else -> TAB_ALL
            }
            renderPortfolio()
        }

        loadUserChallenges()
    }

    private fun openChatWithTarget() {
        startActivity(
            Intent(this, ChatRoomActivity::class.java)
                .putExtra(EXTRA_USER_ID, targetUser.id)
                .putExtra(EXTRA_USER_NAME, targetUser.name)
                .putExtra(EXTRA_USER_EMAIL, targetUser.email)
                .putExtra(EXTRA_USER_AVATAR, targetUser.avatarUrl)
        )
    }

    private fun updateFriendButton() {
        val btn = findViewById<TextView>(R.id.btn_other_friend_toggle)
        val isFriend = FirebaseSocialRepository.isFriend(this, targetUser.id)
        if (isFriend) {
            btn.setText(R.string.friend_added_action)
            btn.setBackgroundResource(R.drawable.bg_btn_duolingo_3d_green)
        } else {
            btn.setText(R.string.add_friend_action)
            btn.setBackgroundResource(R.drawable.bg_btn_duolingo_3d_primary)
        }
    }

    private fun loadUserChallenges() {
        val loading = findViewById<View>(R.id.other_portfolio_loading)
        loading.visibility = View.VISIBLE
        FirebaseBountyRepository.loadChallenges(targetUser.id, false) { result ->
            if (isDestroyed || isFinishing) return@loadChallenges
            loading.visibility = View.GONE
            val all = result.getOrNull().orEmpty()
            allUserChallenges = all.filter {
                it.creatorId == targetUser.id || it.participantId == targetUser.id
            }
            val joinedCount = allUserChallenges.count { it.participantId == targetUser.id }
            val createdCount = allUserChallenges.count { it.creatorId == targetUser.id }
            val completedCount = allUserChallenges.count {
                it.participantId == targetUser.id && it.status == Challenge.COMPLETED
            }
            findViewById<TextView>(R.id.other_stat_joined).text = joinedCount.toString()
            findViewById<TextView>(R.id.other_stat_created).text = createdCount.toString()
            findViewById<TextView>(R.id.other_stat_completed).text = completedCount.toString()
            renderPortfolio()
        }
    }

    private fun renderPortfolio() {
        val filtered = when (selectedTab) {
            TAB_JOINED -> allUserChallenges.filter { it.participantId == targetUser.id }
            TAB_CREATED -> allUserChallenges.filter { it.creatorId == targetUser.id }
            else -> allUserChallenges
        }
        val list = findViewById<RecyclerView>(R.id.other_portfolio_list)
        val emptyCard = findViewById<View>(R.id.other_portfolio_empty)
        val emptyText = findViewById<TextView>(R.id.other_portfolio_empty_text)
        portfolioAdapter.submitList(filtered)
        if (filtered.isEmpty()) {
            list.visibility = View.GONE
            emptyCard.visibility = View.VISIBLE
            emptyText.setText(R.string.portfolio_empty_other)
        } else {
            list.visibility = View.VISIBLE
            emptyCard.visibility = View.GONE
        }
    }

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_USER_NAME = "extra_user_name"
        const val EXTRA_USER_EMAIL = "extra_user_email"
        const val EXTRA_USER_AVATAR = "extra_user_avatar"
        private const val TAB_ALL = 0
        private const val TAB_JOINED = 1
        private const val TAB_CREATED = 2
    }
}
