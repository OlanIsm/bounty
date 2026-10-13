package id.ac.binus.bounty.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.os.BundleCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import id.ac.binus.bounty.R
import id.ac.binus.bounty.adapters.ChallengeAdapter
import id.ac.binus.bounty.database.AppDatabase
import id.ac.binus.bounty.models.Challenge
import id.ac.binus.bounty.models.User
import id.ac.binus.bounty.network.ApiClient
import id.ac.binus.bounty.network.RandomUserApi
import id.ac.binus.bounty.network.RandomUserResponse
import id.ac.binus.bounty.utils.ChallengeDisplay
import id.ac.binus.bounty.utils.ScreenInsets
import id.ac.binus.bounty.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException

class MainActivity : AppCompatActivity() {
    private var navigation: BottomNavigationView? = null
    private var selectedDestination = R.id.nav_home
    private var feedRequest = 0
    private var feedAdapter: ChallengeAdapter? = null
    private var homeHeaderAdapter: HomeHeaderAdapter? = null
    private var balanceRequest = 0
    private var createScreen: View? = null
    private var createState: SparseArray<Parcelable>? = null
    private lateinit var publishModel: PublishModel
    private lateinit var demoUsersModel: DemoUsersModel
    private val backToHome = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            navigation?.setSelectedItemId(R.id.nav_home)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SessionManager(this).isFirebaseAuthenticated()) {
            openLogin()
            return
        }
        setContentView(R.layout.activity_main)
        ScreenInsets.apply(this)
        navigation = findViewById(R.id.bottom_navigation)
        publishModel = ViewModelProvider(this)[PublishModel::class.java]
        demoUsersModel = ViewModelProvider(this)[DemoUsersModel::class.java]
        createState = savedInstanceState?.let {
            BundleCompat.getSparseParcelableArray(it, "create_state", Parcelable::class.java)
        }
        onBackPressedDispatcher.addCallback(this, backToHome)
        val nav = navigation ?: return
        nav.setOnItemSelectedListener { showScreen(it.itemId) }
        val destination = savedInstanceState?.getInt("selected_destination", R.id.nav_home) ?: R.id.nav_home
        nav.selectedItemId = if (nav.menu.findItem(destination) == null) R.id.nav_home else destination
        nav.setOnItemReselectedListener { }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (navigation == null) return
        demoUsersModel.state.observe(this) { bindDemoUsers() }
        publishModel.state.observe(this) { state ->
            updatePublishState()
            if (state == PublishModel.SAVED) {
                createScreen = null
                createState = null
                publishModel.state.value = PublishModel.IDLE
                WindowInsetsControllerCompat(window, findViewById(R.id.main)).hide(WindowInsetsCompat.Type.ime())
                val alreadyHome = selectedDestination == R.id.nav_home
                navigation?.selectedItemId = R.id.nav_home
                if (alreadyHome) loadFeed()
                showSnackbar(R.string.challenge_published)
            } else if (state == PublishModel.ERROR) {
                showSnackbar(R.string.challenge_publish_error)
                publishModel.state.value = PublishModel.IDLE
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val session = SessionManager(this)
        val user = session.getCurrentUser()
        if (!session.isFirebaseAuthenticated() || user == null) {
            openLogin()
            return
        }
        bindUser(user)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("selected_destination", selectedDestination)
        createScreen?.let {
            createState = SparseArray()
            it.saveHierarchyState(createState)
        }
        outState.putSparseParcelableArray("create_state", createState)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        refreshBalance()
        if (navigation != null && (selectedDestination == R.id.nav_home || selectedDestination == R.id.nav_my_challenges)) loadFeed()
    }

    private fun showScreen(destination: Int): Boolean {
        val (layout, title) = when (destination) {
            R.id.nav_home -> R.layout.screen_home to R.string.home_title
            R.id.nav_my_challenges -> R.layout.screen_my_challenges to R.string.my_challenges_title
            R.id.nav_create -> R.layout.screen_create to R.string.create_title
            R.id.nav_profile -> R.layout.screen_profile to R.string.profile_title
            else -> return false
        }
        val content = findViewById<FrameLayout>(R.id.screen_content)
        if (selectedDestination == R.id.nav_create && destination != R.id.nav_create && createScreen != null) {
            createScreen?.clearFocus()
            WindowInsetsControllerCompat(window, content).hide(WindowInsetsCompat.Type.ime())
        }
        content.removeAllViews()
        if (destination == R.id.nav_create) {
            if (createScreen == null) {
                createScreen = layoutInflater.inflate(layout, content, false).also { screen ->
                    createState?.let(screen::restoreHierarchyState)
                    screen.findViewById<View>(R.id.publish_button).setOnClickListener { publishChallenge() }
                    screen.findViewById<TextInputLayout>(R.id.create_deadline_layout)
                        .setEndIconOnClickListener { chooseDeadline() }
                }
            }
            content.addView(createScreen)
            updatePublishState()
        } else {
            layoutInflater.inflate(layout, content, true)
        }
        findViewById<MaterialToolbar>(R.id.main_toolbar).setTitle(title)
        selectedDestination = destination
        backToHome.isEnabled = destination != R.id.nav_home
        val user = SessionManager(this).getCurrentUser()
        if (destination == R.id.nav_profile) {
            findViewById<View>(R.id.demo_users_retry).setOnClickListener { demoUsersModel.load(ApiClient.getApi()) }
            if (demoUsersModel.state.value == null) demoUsersModel.load(ApiClient.getApi())
            bindDemoUsers()
            findViewById<View>(R.id.logout_button).setOnClickListener {
                SessionManager(this).logout()
                openLogin()
            }
        } else if (destination == R.id.nav_home || destination == R.id.nav_my_challenges) {
            val list = findViewById<RecyclerView>(R.id.challenge_list)
            list.layoutManager = LinearLayoutManager(this)
            feedAdapter = ChallengeAdapter { challenge ->
                startActivity(Intent(this, ChallengeDetailActivity::class.java)
                    .putExtra(ChallengeDetailActivity.EXTRA_CHALLENGE_ID, challenge.id))
            }
            list.itemAnimator = null
            if (destination == R.id.nav_home) {
                homeHeaderAdapter = HomeHeaderAdapter(user)
                list.adapter = ConcatAdapter(homeHeaderAdapter!!, feedAdapter!!)
            } else list.adapter = feedAdapter
            findViewById<View>(R.id.feed_retry).setOnClickListener { loadFeed() }
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) loadFeed()
        }
        user?.let(::bindUser)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) refreshBalance()
        return true
    }

    private fun chooseDeadline() {
        val input = createScreen?.findViewById<TextInputEditText>(R.id.create_deadline_input) ?: return
        var selected = LocalDate.now().plusDays(7)
        try {
            selected = LocalDate.parse(input.text?.toString().orEmpty())
        } catch (_: DateTimeParseException) { }
        DatePickerDialog(this, { _, year, month, day ->
            input.setText(LocalDate.of(year, month + 1, day).toString())
            createScreen?.findViewById<TextInputLayout>(R.id.create_deadline_layout)?.error = null
        }, selected.year, selected.monthValue - 1, selected.dayOfMonth).apply {
            datePicker.minDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            show()
        }
    }

    private fun publishChallenge() {
        if (publishModel.state.value == PublishModel.SAVING) return
        val user = SessionManager(this).getCurrentUser()
        if (user == null) {
            openLogin()
            return
        }
        val title = inputText(R.id.create_title_input)
        val description = inputText(R.id.create_description_input)
        val rewardText = inputText(R.id.create_reward_input)
        val deadline = inputText(R.id.create_deadline_input)
        val titleValid = title.isNotEmpty() && title.length <= 100
        val descriptionValid = description.isNotEmpty() && description.length <= 2000
        var reward = 0.0
        try {
            if (rewardText.matches(Regex("[0-9]+"))) reward = rewardText.toDouble()
        } catch (_: NumberFormatException) { }
        val rewardValid = reward.isFinite() && reward > 0
        var deadlineValid = false
        try {
            val date = LocalDate.parse(deadline)
            deadlineValid = deadline.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")) && !date.isBefore(LocalDate.now())
        } catch (_: DateTimeParseException) { }
        setFormError(R.id.create_title_layout, titleValid, R.string.required_challenge_title)
        setFormError(R.id.create_description_layout, descriptionValid, R.string.required_challenge_description)
        setFormError(R.id.create_reward_layout, rewardValid, R.string.invalid_reward)
        setFormError(R.id.create_deadline_layout, deadlineValid, R.string.invalid_deadline)
        if (!titleValid || !descriptionValid || !rewardValid || !deadlineValid) return
        val challenge = Challenge().apply {
            this.title = title
            this.description = description
            this.reward = reward
            this.deadline = deadline
            creatorId = user.id
            creatorName = user.name
            creatorAvatar = user.avatarUrl
            createdAt = System.currentTimeMillis()
        }
        publishModel.publish(AppDatabase.getInstance(this), challenge)
    }

    private fun inputText(id: Int): String =
        createScreen?.findViewById<TextInputEditText>(id)?.text?.toString()?.trim().orEmpty()

    private fun setFormError(id: Int, valid: Boolean, message: Int) {
        createScreen?.findViewById<TextInputLayout>(id)?.error = if (valid) null else getString(message)
    }

    private fun updatePublishState() {
        val screen = createScreen ?: return
        val saving = publishModel.state.value == PublishModel.SAVING
        listOf(R.id.create_title_layout, R.id.create_description_layout, R.id.create_reward_layout,
            R.id.create_deadline_layout, R.id.publish_button).forEach { screen.findViewById<View>(it).isEnabled = !saving }
        screen.findViewById<View>(R.id.publish_loading).visibility = if (saving) View.VISIBLE else View.GONE
        screen.findViewById<MaterialButton>(R.id.publish_button).setText(
            if (saving) R.string.publishing_challenge else R.string.publish_action
        )
    }

    private fun loadFeed() {
        val screenId = if (selectedDestination == R.id.nav_my_challenges) R.id.my_challenges_screen else R.id.home_screen
        val mine = selectedDestination == R.id.nav_my_challenges
        val user = SessionManager(this).getCurrentUser() ?: return
        val home = findViewById<View>(screenId) ?: return
        val request = ++feedRequest
        home.findViewById<View>(R.id.feed_loading).visibility = View.VISIBLE
        home.findViewById<View>(R.id.feed_message).visibility = View.GONE
        home.findViewById<View>(R.id.feed_retry).visibility = View.GONE
        val db = AppDatabase.getInstance(this)
        db.queryExecutor.execute {
            try {
                val challenges = if (mine) db.challengeDao().getMyChallenges(user.id) else db.challengeDao().getAllChallenges()
                runOnUiThread {
                    if (isDestroyed || isFinishing || request != feedRequest || findViewById<View>(screenId) !== home) return@runOnUiThread
                    val list = home.findViewById<RecyclerView>(R.id.challenge_list)
                    feedAdapter?.submitList(challenges)
                    home.findViewById<View>(R.id.feed_loading).visibility = View.GONE
                    list.visibility = if (mine && challenges.isEmpty()) View.GONE else View.VISIBLE
                    home.findViewById<TextView>(R.id.feed_message).apply {
                        setText(if (mine) R.string.no_my_challenges else R.string.no_available_challenges)
                        visibility = if (challenges.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            } catch (error: RuntimeException) {
                Log.e("BountyFeed", "Unable to load challenges", error)
                runOnUiThread {
                    if (isDestroyed || isFinishing || request != feedRequest || findViewById<View>(screenId) !== home) return@runOnUiThread
                    home.findViewById<View>(R.id.feed_loading).visibility = View.GONE
                    feedAdapter?.submitList(emptyList())
                    home.findViewById<RecyclerView>(R.id.challenge_list).visibility = if (mine) View.GONE else View.VISIBLE
                    home.findViewById<TextView>(R.id.feed_message).apply {
                        setText(R.string.challenge_load_error)
                        visibility = View.VISIBLE
                    }
                    home.findViewById<View>(R.id.feed_retry).visibility = View.VISIBLE
                    showSnackbar(R.string.challenge_load_error)
                }
            }
        }
    }

    private class HomeHeaderAdapter(var user: User?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, type: Int) = object : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_home_header, parent, false)
        ) { }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val currentUser = user ?: return
            val root = holder.itemView
            root.findViewById<TextView>(R.id.welcome_text).text =
                root.context.getString(R.string.welcome_user, currentUser.name)
            root.findViewById<TextView>(R.id.demo_balance_amount).text =
                ChallengeDisplay.rupiah(root.context, currentUser.demoBalance)
        }

        override fun getItemCount() = 1
    }

    private fun refreshBalance() {
        if (navigation == null || (selectedDestination != R.id.nav_home && selectedDestination != R.id.nav_profile)) return
        val session = SessionManager(applicationContext)
        val user = session.getCurrentUser() ?: return
        val request = ++balanceRequest
        val db = AppDatabase.getInstance(this)
        db.transactionExecutor.execute {
            try {
                session.creditCompletedRewards(db, user.id)
                runOnUiThread {
                    val current = session.getCurrentUser()
                    if (!isDestroyed && !isFinishing && request == balanceRequest && current != null && current.id == user.id) bindUser(current)
                }
            } catch (error: RuntimeException) {
                Log.e("BountyReward", "Unable to recover demo rewards", error)
                runOnUiThread {
                    if (!isDestroyed && !isFinishing && request == balanceRequest) showSnackbar(R.string.reward_refresh_error)
                }
            }
        }
    }

    private fun bindUser(user: User) {
        findViewById<TextView?>(R.id.demo_balance_amount)?.text = ChallengeDisplay.rupiah(this, user.demoBalance)
        if (selectedDestination == R.id.nav_home) {
            homeHeaderAdapter?.let {
                it.user = user
                it.notifyItemChanged(0)
            }
        } else if (selectedDestination == R.id.nav_profile) {
            findViewById<TextView>(R.id.profile_name).text = user.name
            findViewById<TextView>(R.id.user_email).text = user.email
        }
    }

    private fun bindDemoUsers() {
        val list = findViewById<LinearLayout?>(R.id.demo_users_list) ?: return
        val state = demoUsersModel.state.value ?: return
        findViewById<View>(R.id.demo_users_loading).visibility = if (state.loading) View.VISIBLE else View.GONE
        findViewById<View>(R.id.demo_users_error).visibility = if (state.failed) View.VISIBLE else View.GONE
        findViewById<View>(R.id.demo_users_retry).visibility = if (state.failed) View.VISIBLE else View.GONE
        list.removeAllViews()
        state.users.forEach { user ->
            val row = layoutInflater.inflate(R.layout.item_demo_user, list, false)
            row.findViewById<TextView>(R.id.demo_user_name).text = user.name
            row.findViewById<TextView>(R.id.demo_user_email).apply {
                text = user.email
                visibility = if (user.email.isEmpty()) View.GONE else View.VISIBLE
            }
            Glide.with(row.findViewById<ImageView>(R.id.demo_user_avatar)).load(user.avatarUrl)
                .placeholder(R.drawable.ic_avatar).error(R.drawable.ic_avatar).circleCrop()
                .into(row.findViewById(R.id.demo_user_avatar))
            list.addView(row)
        }
    }

    class DemoUsersModel : ViewModel() {
        data class State(
            @JvmField val users: List<User>,
            @JvmField val loading: Boolean,
            @JvmField val failed: Boolean
        )

        @JvmField val state = MutableLiveData<State>()
        private var request: Call<RandomUserResponse>? = null

        fun load(api: RandomUserApi) {
            if (state.value?.loading == true) return
            state.value = State(emptyList(), true, false)
            request = api.getUsers(10).also { current ->
                current.enqueue(object : Callback<RandomUserResponse> {
                    override fun onResponse(call: Call<RandomUserResponse>, response: Response<RandomUserResponse>) {
                        if (call !== request || call.isCanceled) return
                        val users = if (response.isSuccessful) response.body()?.toUsers().orEmpty() else emptyList()
                        if (users.isEmpty()) fallback() else state.value = State(users, false, false)
                    }

                    override fun onFailure(call: Call<RandomUserResponse>, error: Throwable) {
                        if (call !== request || call.isCanceled) return
                        Log.w("BountyApi", "Unable to load demo users", error)
                        fallback()
                    }
                })
            }
        }

        private fun fallback() {
            state.value = State(listOf(User("api_fallback", "Bounty User", "", "", 0.0)), false, true)
        }

        override fun onCleared() {
            request?.cancel()
        }
    }

    private fun openLogin() {
        startActivity(Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    private fun showSnackbar(message: Int) {
        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG).setAnchorView(navigation).show()
    }

    class PublishModel : ViewModel() {
        @JvmField val state = MutableLiveData(IDLE)

        fun publish(db: AppDatabase, challenge: Challenge) {
            if (state.value == SAVING || state.value == SAVED) return
            state.value = SAVING
            db.transactionExecutor.execute {
                try {
                    db.challengeDao().insertChallenge(challenge)
                    state.postValue(SAVED)
                } catch (error: RuntimeException) {
                    Log.e("BountyCreate", "Unable to publish challenge", error)
                    state.postValue(ERROR)
                }
            }
        }

        companion object {
            const val IDLE = 0
            const val SAVING = 1
            const val SAVED = 2
            const val ERROR = 3
        }
    }
}
