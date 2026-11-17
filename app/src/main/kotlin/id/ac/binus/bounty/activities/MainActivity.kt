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
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import id.ac.binus.bounty.R
import android.widget.Toast
import id.ac.binus.bounty.adapters.ChallengeAdapter
import id.ac.binus.bounty.adapters.LeaderboardAdapter
import id.ac.binus.bounty.adapters.LeaderboardItem
import id.ac.binus.bounty.data.FirebaseBountyRepository
import id.ac.binus.bounty.models.Challenge
import id.ac.binus.bounty.models.User
import id.ac.binus.bounty.network.ApiClient
import id.ac.binus.bounty.network.RandomUserApi
import id.ac.binus.bounty.network.RandomUserResponse
import id.ac.binus.bounty.utils.ChallengeDisplay
import id.ac.binus.bounty.utils.ScreenInsets
import id.ac.binus.bounty.utils.SessionManager
import id.ac.binus.bounty.utils.ViewMotion
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
    private var myChallengesRawList: List<Challenge> = emptyList()
    private var isMyChallengesOwnSelected: Boolean = true
    private var myChallengesSearchQuery: String = ""
    private var createScreen: View? = null
    private var createState: SparseArray<Parcelable>? = null
    private lateinit var publishModel: PublishModel
    private lateinit var demoUsersModel: DemoUsersModel
    private var leaderboardAdapter: LeaderboardAdapter? = null
    private var leaderboardRawChallenges: List<Challenge> = emptyList()
    private var isLeaderboardCreatorsSelected: Boolean = true
    private var leaderboardRequest = 0
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
        setupStitchNavigation(nav)
        nav.setOnItemSelectedListener { item ->
            val handled = showScreen(item.itemId)
            if (handled) {
                applyNavSelectionEffect(nav, item.itemId)
            }
            handled
        }
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
        else if (navigation != null && selectedDestination == R.id.nav_leaderboard) loadLeaderboardData()
    }

    private fun showScreen(destination: Int): Boolean {
        val (layout, title) = when (destination) {
            R.id.nav_home -> R.layout.screen_home to R.string.home_title
            R.id.nav_my_challenges -> R.layout.screen_my_challenges to R.string.my_challenges_title
            R.id.nav_create -> R.layout.screen_create to R.string.create_title
            R.id.nav_leaderboard -> R.layout.screen_leaderboard to R.string.leaderboard_title
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
                    val pubBtn = screen.findViewById<View>(R.id.publish_button)
                    ViewMotion.applySpringPress(pubBtn) { publishChallenge() }
                    screen.findViewById<TextInputLayout>(R.id.create_deadline_layout)
                        .setEndIconOnClickListener { chooseDeadline() }
                }
            }
            content.addView(createScreen)
            ViewMotion.animateEntrance(createScreen!!)
            updatePublishState()
        } else {
            val inflated = layoutInflater.inflate(layout, content, true)
            ViewMotion.animateEntrance(inflated)
        }
        val toolbar = findViewById<MaterialToolbar>(R.id.main_toolbar)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        val rootLayout = findViewById<View>(R.id.main)
        if (destination == R.id.nav_my_challenges) {
            toolbar.visibility = View.GONE
            rootLayout?.setBackgroundResource(R.color.bounty_primary)
            insetsController.isAppearanceLightStatusBars = false
        } else {
            toolbar.visibility = View.VISIBLE
            toolbar.setTitle(title)
            rootLayout?.setBackgroundResource(R.color.bounty_background)
            insetsController.isAppearanceLightStatusBars = true
        }
        selectedDestination = destination
        backToHome.isEnabled = destination != R.id.nav_home
        val user = SessionManager(this).getCurrentUser()
        if (destination == R.id.nav_profile) {
            setupProfileScreen(user)
        } else if (destination == R.id.nav_leaderboard) {
            setupLeaderboardScreen()
        } else if (destination == R.id.nav_home || destination == R.id.nav_my_challenges) {
            val list = findViewById<RecyclerView>(R.id.challenge_list)
            list.layoutManager = LinearLayoutManager(this)
            feedAdapter = ChallengeAdapter { challenge ->
                startActivity(Intent(this, ChallengeDetailActivity::class.java)
                    .putExtra(ChallengeDetailActivity.EXTRA_CLOUD_CHALLENGE_ID, challenge.cloudId))
            }
            list.itemAnimator = null
            if (destination == R.id.nav_home) {
                homeHeaderAdapter = HomeHeaderAdapter(user) {
                    navigation?.selectedItemId = R.id.nav_create
                }
                list.adapter = ConcatAdapter(homeHeaderAdapter!!, feedAdapter!!)
            } else list.adapter = feedAdapter
            val retryFeed = findViewById<View>(R.id.feed_retry)
            ViewMotion.applySpringPress(retryFeed) { loadFeed() }
            if (destination == R.id.nav_my_challenges) {
                val createFirst = findViewById<View>(R.id.empty_create_challenge)
                ViewMotion.applySpringPress(createFirst) {
                    navigation?.selectedItemId = R.id.nav_create
                }
                val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.challenge_toggle_group)
                toggleGroup?.check(if (isMyChallengesOwnSelected) R.id.toggle_own else R.id.toggle_other)
                toggleGroup?.addOnButtonCheckedListener { _, checkedId, isChecked ->
                    if (isChecked) {
                        isMyChallengesOwnSelected = (checkedId == R.id.toggle_own)
                        applyMyChallengesFilter()
                    }
                }
                val searchInput = findViewById<TextInputEditText>(R.id.challenge_search_input)
                searchInput?.setText(myChallengesSearchQuery)
                searchInput?.doAfterTextChanged { text ->
                    myChallengesSearchQuery = text?.toString().orEmpty()
                    applyMyChallengesFilter()
                }
            }
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
        publishModel.publish(challenge)
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
        val isMyChallenges = selectedDestination == R.id.nav_my_challenges
        val user = SessionManager(this).getCurrentUser() ?: return
        val home = findViewById<View>(screenId) ?: return
        val request = ++feedRequest
        home.findViewById<View>(R.id.feed_loading).visibility = View.VISIBLE
        home.findViewById<View>(R.id.feed_message)?.visibility = View.GONE
        home.findViewById<View>(R.id.feed_retry).visibility = View.GONE
        home.findViewById<View>(R.id.empty_create_challenge)?.visibility = View.GONE
        if (!isMyChallenges) {
            homeHeaderAdapter?.isEmpty = false
            homeHeaderAdapter?.errorMessage = null
        }
        FirebaseBountyRepository.loadChallenges(user.id, false) { result ->
            if (isDestroyed || isFinishing || request != feedRequest || findViewById<View>(screenId) !== home) return@loadChallenges
            val list = home.findViewById<RecyclerView>(R.id.challenge_list)
            home.findViewById<View>(R.id.feed_loading).visibility = View.GONE
            if (result.isSuccess) {
                val challenges = result.getOrThrow()
                if (isMyChallenges) {
                    myChallengesRawList = challenges
                    applyMyChallengesFilter()
                } else {
                    feedAdapter?.submitList(challenges)
                    list.visibility = View.VISIBLE
                    homeHeaderAdapter?.isEmpty = challenges.isEmpty()
                }
            } else {
                Log.e("BountyFeed", "Unable to load challenges", result.exceptionOrNull())
                myChallengesRawList = emptyList()
                feedAdapter?.submitList(emptyList())
                list.visibility = View.GONE
                home.findViewById<TextView>(R.id.feed_message)?.apply {
                    setText(R.string.challenge_load_error)
                    visibility = View.VISIBLE
                }
                home.findViewById<View>(R.id.feed_retry).visibility = View.VISIBLE
                if (!isMyChallenges) {
                    homeHeaderAdapter?.errorMessage = getString(R.string.challenge_load_error)
                }
                showSnackbar(R.string.challenge_load_error)
            }
        }
    }

    private fun applyMyChallengesFilter() {
        if (selectedDestination != R.id.nav_my_challenges) return
        val screen = findViewById<View>(R.id.my_challenges_screen) ?: return
        val user = SessionManager(this).getCurrentUser() ?: return
        val list = screen.findViewById<RecyclerView>(R.id.challenge_list)
        val feedMessage = screen.findViewById<TextView>(R.id.feed_message)
        val emptyCreate = screen.findViewById<View>(R.id.empty_create_challenge)

        val baseList = if (isMyChallengesOwnSelected) {
            myChallengesRawList.filter { it.creatorId == user.id || it.participantId == user.id }
        } else {
            myChallengesRawList.filter { it.creatorId != user.id }
        }

        val query = myChallengesSearchQuery.trim()
        val filtered = if (query.isEmpty()) {
            baseList
        } else {
            baseList.filter { challenge ->
                challenge.title.contains(query, ignoreCase = true) ||
                challenge.description.contains(query, ignoreCase = true)
            }
        }

        feedAdapter?.submitList(filtered)

        if (filtered.isEmpty()) {
            list.visibility = View.GONE
            feedMessage?.visibility = View.VISIBLE
            if (query.isNotEmpty()) {
                feedMessage?.text = getString(R.string.no_search_results)
                emptyCreate?.visibility = View.GONE
            } else if (isMyChallengesOwnSelected) {
                feedMessage?.text = getString(R.string.no_my_challenges)
                emptyCreate?.visibility = View.VISIBLE
            } else {
                feedMessage?.text = getString(R.string.no_other_challenges)
                emptyCreate?.visibility = View.GONE
            }
        } else {
            list.visibility = View.VISIBLE
            feedMessage?.visibility = View.GONE
            emptyCreate?.visibility = View.GONE
        }
    }

    private class HomeHeaderAdapter(
        var user: User?,
        private val onCreateClick: () -> Unit
    ) : RecyclerView.Adapter<HomeHeaderAdapter.ViewHolder>() {

        var isEmpty: Boolean = false
            set(value) {
                if (field != value) {
                    field = value
                    notifyItemChanged(0)
                }
            }

        var errorMessage: String? = null
            set(value) {
                if (field != value) {
                    field = value
                    notifyItemChanged(0)
                }
            }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(parent: ViewGroup, type: Int) = ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_home_header, parent, false)
        )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val currentUser = user
            val root = holder.itemView
            if (currentUser != null) {
                root.findViewById<TextView>(R.id.welcome_text).text =
                    root.context.getString(R.string.welcome_user, currentUser.name)
                root.findViewById<TextView>(R.id.demo_balance_amount).text =
                    ChallengeDisplay.rupiah(root.context, currentUser.demoBalance)
            }
            val emptyContainer = root.findViewById<View>(R.id.home_empty_container)
            val feedMsg = root.findViewById<TextView>(R.id.feed_message)
            val createBtn = root.findViewById<View>(R.id.home_create_challenge)

            if (errorMessage != null) {
                emptyContainer.visibility = View.VISIBLE
                feedMsg.text = errorMessage
                createBtn.visibility = View.GONE
            } else if (isEmpty) {
                emptyContainer.visibility = View.VISIBLE
                feedMsg.setText(R.string.no_available_challenges)
                createBtn.visibility = View.VISIBLE
                ViewMotion.applySpringPress(createBtn) { onCreateClick() }
            } else {
                emptyContainer.visibility = View.GONE
            }
        }

        override fun getItemCount() = 1
    }

    private fun refreshBalance() {
        if (navigation == null || (selectedDestination != R.id.nav_home && selectedDestination != R.id.nav_profile)) return
        val session = SessionManager(applicationContext)
        val user = session.getCurrentUser() ?: return
        val request = ++balanceRequest
        FirebaseBountyRepository.loadBalance(user.id) { result ->
            if (isDestroyed || isFinishing || request != balanceRequest) return@loadBalance
            result.onSuccess { balance ->
                session.updateDemoBalance(user.id, balance)
                session.getCurrentUser()?.let(::bindUser)
            }.onFailure { error ->
                Log.e("BountyReward", "Unable to load demo balance", error)
                showSnackbar(R.string.reward_refresh_error)
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
            findViewById<TextView?>(R.id.profile_name)?.text = user.name
            findViewById<TextView?>(R.id.user_email)?.text = user.email
            findViewById<ImageView?>(R.id.profile_avatar_view)?.let { img ->
                Glide.with(img).load(user.avatarUrl)
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .circleCrop()
                    .into(img)
            }
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

    private fun setupStitchNavigation(nav: BottomNavigationView) {
        nav.itemIconTintList = null
        nav.post {
            val menuView = nav.getChildAt(0) as? ViewGroup ?: return@post
            val density = resources.displayMetrics.density
            val createSize = (52 * density).toInt()

            for (i in 0 until menuView.childCount) {
                val itemView = menuView.getChildAt(i) as? ViewGroup ?: continue
                if (itemView.id == R.id.nav_create) {
                    val iconView = itemView.findViewById<ImageView>(com.google.android.material.R.id.navigation_bar_item_icon_view)
                    val iconContainer = itemView.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_icon_container)
                    iconView?.layoutParams = iconView?.layoutParams?.apply {
                        width = createSize
                        height = createSize
                    }
                    iconContainer?.layoutParams = iconContainer?.layoutParams?.apply {
                        width = createSize
                        height = createSize
                    }
                    itemView.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_labels_group)?.visibility = View.GONE
                    ViewMotion.applySpringPress(itemView)
                }
            }
            applyNavSelectionEffect(nav, nav.selectedItemId)
        }
    }

    private fun applyNavSelectionEffect(nav: BottomNavigationView, selectedId: Int) {
        nav.post {
            val menuView = nav.getChildAt(0) as? ViewGroup ?: return@post
            for (i in 0 until menuView.childCount) {
                val itemView = menuView.getChildAt(i) as? ViewGroup ?: continue
                val isSelected = itemView.id == selectedId
                val isCreate = itemView.id == R.id.nav_create

                if (isCreate) {
                    itemView.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_labels_group)?.visibility = View.GONE
                    continue
                }

                val iconContainer = itemView.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_icon_container)
                    ?: itemView.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_icon_view)
                val targetScale = if (isSelected) 1.10f else 1.0f
                iconContainer?.animate()
                    ?.scaleX(targetScale)
                    ?.scaleY(targetScale)
                    ?.setDuration(200)
                    ?.setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                    ?.start()
            }
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

        fun publish(challenge: Challenge) {
            if (state.value == SAVING || state.value == SAVED) return
            state.value = SAVING
            FirebaseBountyRepository.createChallenge(challenge) { result ->
                result.onSuccess { state.value = SAVED }
                    .onFailure { error ->
                        Log.e("BountyCreate", "Unable to publish challenge", error)
                        state.value = ERROR
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

    private fun setupProfileScreen(user: User?) {
        val retryUsers = findViewById<View?>(R.id.demo_users_retry)
        if (retryUsers != null) {
            ViewMotion.applySpringPress(retryUsers) { demoUsersModel.load(ApiClient.getApi()) }
        }
        if (demoUsersModel.state.value == null) demoUsersModel.load(ApiClient.getApi())
        bindDemoUsers()

        findViewById<View?>(R.id.logout_button)?.let { logoutBtn ->
            ViewMotion.applySpringPress(logoutBtn) {
                SessionManager(this).logout()
                openLogin()
            }
        }

        findViewById<View?>(R.id.menu_edit_profile)?.let { btn ->
            ViewMotion.applySpringPress(btn) {
                showToast("Profil tersinkronisasi dengan akun login.")
            }
        }

        findViewById<View?>(R.id.menu_activity_history)?.let { btn ->
            ViewMotion.applySpringPress(btn) {
                navigation?.selectedItemId = R.id.nav_my_challenges
            }
        }

        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_notifications)
            ?.setOnCheckedChangeListener { _, isChecked ->
                showToast(if (isChecked) "Notifikasi tantangan diaktifkan." else "Notifikasi tantangan dinonaktifkan.")
            }

        findViewById<View?>(R.id.menu_theme)?.let { btn ->
            ViewMotion.applySpringPress(btn) {
                showToast("Tema mengikuti preferensi sistem perangkat.")
            }
        }

        findViewById<View?>(R.id.menu_language)?.let { btn ->
            ViewMotion.applySpringPress(btn) {
                showToast("Bahasa saat ini: Bahasa Indonesia.")
            }
        }

        findViewById<View?>(R.id.menu_security)?.let { btn ->
            ViewMotion.applySpringPress(btn) {
                showToast("Akun terlindungi oleh autentikasi Firebase.")
            }
        }

        findViewById<View?>(R.id.menu_help)?.let { btn ->
            ViewMotion.applySpringPress(btn) {
                showToast("Bounty MVP: Aplikasi tantangan dan reward seru.")
            }
        }

        findViewById<View?>(R.id.menu_about)?.let { btn ->
            ViewMotion.applySpringPress(btn) {
                showToast("Bounty v1.0.0 • Dibuat untuk Coursework Android.")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setupLeaderboardScreen() {
        val screen = findViewById<View>(R.id.leaderboard_screen) ?: return
        val list = screen.findViewById<RecyclerView>(R.id.leaderboard_list)
        list.layoutManager = LinearLayoutManager(this)
        leaderboardAdapter = LeaderboardAdapter()
        list.adapter = leaderboardAdapter
        list.itemAnimator = null

        val toggleGroup = screen.findViewById<MaterialButtonToggleGroup>(R.id.leaderboard_toggle_group)
        toggleGroup?.check(if (isLeaderboardCreatorsSelected) R.id.toggle_creators else R.id.toggle_hunters)
        toggleGroup?.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isLeaderboardCreatorsSelected = (checkedId == R.id.toggle_creators)
                applyLeaderboardData()
            }
        }

        val retryBtn = screen.findViewById<View>(R.id.leaderboard_retry)
        if (retryBtn != null) {
            ViewMotion.applySpringPress(retryBtn) { loadLeaderboardData() }
        }

        loadLeaderboardData()
    }

    private fun loadLeaderboardData() {
        val screen = findViewById<View>(R.id.leaderboard_screen) ?: return
        val loading = screen.findViewById<View>(R.id.leaderboard_loading)
        val errorContainer = screen.findViewById<View>(R.id.leaderboard_error_container)
        val emptyContainer = screen.findViewById<View>(R.id.leaderboard_empty_container)
        val list = screen.findViewById<RecyclerView>(R.id.leaderboard_list)

        loading?.visibility = View.VISIBLE
        errorContainer?.visibility = View.GONE
        emptyContainer?.visibility = View.GONE

        val request = ++leaderboardRequest
        val user = SessionManager(this).getCurrentUser() ?: return
        FirebaseBountyRepository.loadChallenges(user.id, false) { result ->
            if (isDestroyed || isFinishing || request != leaderboardRequest || selectedDestination != R.id.nav_leaderboard) return@loadChallenges
            loading?.visibility = View.GONE
            if (result.isSuccess) {
                leaderboardRawChallenges = result.getOrThrow()
                applyLeaderboardData()
            } else {
                Log.e("BountyLeaderboard", "Unable to load leaderboard data", result.exceptionOrNull())
                errorContainer?.visibility = View.VISIBLE
                list?.visibility = View.GONE
                emptyContainer?.visibility = View.GONE
            }
        }
    }

    private fun applyLeaderboardData() {
        val screen = findViewById<View>(R.id.leaderboard_screen) ?: return
        val user = SessionManager(this).getCurrentUser() ?: return
        val list = screen.findViewById<RecyclerView>(R.id.leaderboard_list)
        val emptyContainer = screen.findViewById<View>(R.id.leaderboard_empty_container)
        val emptyMsg = screen.findViewById<TextView>(R.id.leaderboard_empty_message)

        val items = if (isLeaderboardCreatorsSelected) {
            val grouped = leaderboardRawChallenges
                .filter { it.creatorId.isNotEmpty() }
                .groupBy { it.creatorId }
            grouped.map { (creatorId, challenges) ->
                val sample = challenges.first()
                val name = sample.creatorName.ifEmpty { if (creatorId == user.id) user.name else "Creator" }
                val avatar = sample.creatorAvatar.ifEmpty { if (creatorId == user.id) user.avatarUrl else "" }
                LeaderboardItem(
                    rank = 0,
                    userId = creatorId,
                    name = name,
                    avatarUrl = avatar,
                    count = challenges.size,
                    isCreatorCategory = true,
                    isCurrentUser = (creatorId == user.id)
                )
            }.sortedByDescending { it.count }
        } else {
            val grouped = leaderboardRawChallenges
                .filter { !it.participantId.isNullOrEmpty() }
                .groupBy { it.participantId!! }
            grouped.map { (hunterId, challenges) ->
                val sample = challenges.first()
                val name = sample.participantName?.takeIf { it.isNotEmpty() } ?: if (hunterId == user.id) user.name else "Hunter"
                val avatar = if (hunterId == user.id) user.avatarUrl else ""
                LeaderboardItem(
                    rank = 0,
                    userId = hunterId,
                    name = name,
                    avatarUrl = avatar,
                    count = challenges.size,
                    isCreatorCategory = false,
                    isCurrentUser = (hunterId == user.id)
                )
            }.sortedByDescending { it.count }
        }

        val rankedItems = items.mapIndexed { index, item ->
            item.copy(rank = index + 1)
        }

        leaderboardAdapter?.submitList(rankedItems)

        if (rankedItems.isEmpty()) {
            list?.visibility = View.GONE
            emptyContainer?.visibility = View.VISIBLE
            emptyMsg?.text = getString(R.string.no_leaderboard_data)
        } else {
            list?.visibility = View.VISIBLE
            emptyContainer?.visibility = View.GONE
        }
    }
}
