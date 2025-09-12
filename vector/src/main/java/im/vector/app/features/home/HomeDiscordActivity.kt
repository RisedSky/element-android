/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.viewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.SpaceStateHandler
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.extensions.restart
import im.vector.app.core.extensions.validateBackPressed
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.core.pushers.UnifiedPushHelper
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.core.utils.startSharePlainTextIntent
import im.vector.app.databinding.ActivityHomeDiscordBinding
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import im.vector.app.features.analytics.accountdata.AnalyticsAccountDataViewModel
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.analytics.plan.ViewRoom
import im.vector.app.features.crypto.recover.SetupMode
import im.vector.app.features.home.room.list.actions.RoomListSharedAction
import im.vector.app.features.home.room.list.actions.RoomListSharedActionViewModel
import im.vector.app.features.home.room.list.home.layout.HomeLayoutSettingBottomDialogFragment
import im.vector.app.features.home.room.list.home.release.ReleaseNotesActivity
import im.vector.app.features.matrixto.MatrixToBottomSheet
import im.vector.app.features.matrixto.OriginOfMatrixTo
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.notifications.NotificationDrawerManager
import im.vector.app.features.onboarding.AuthenticationDescription
import im.vector.app.features.permalink.NavigationInterceptor
import im.vector.app.features.permalink.PermalinkHandler
import im.vector.app.features.permalink.PermalinkHandler.Companion.MATRIX_TO_CUSTOM_SCHEME_URL_BASE
import im.vector.app.features.permalink.PermalinkHandler.Companion.ROOM_LINK_PREFIX
import im.vector.app.features.permalink.PermalinkHandler.Companion.USER_LINK_PREFIX
import im.vector.app.features.popup.DefaultVectorAlert
import im.vector.app.features.popup.PopupAlertManager
import im.vector.app.features.popup.VerificationVectorAlert
import im.vector.app.features.rageshake.ReportType
import im.vector.app.features.rageshake.VectorUncaughtExceptionHandler
import im.vector.app.features.session.coroutineScope
import im.vector.app.features.settings.VectorSettingsActivity
import im.vector.app.features.spaces.SpaceCreationActivity
import im.vector.app.features.spaces.SpacePreviewActivity
import im.vector.app.features.spaces.SpaceSettingsMenuBottomSheet
import im.vector.app.features.spaces.invite.SpaceInviteBottomSheet
import im.vector.app.features.spaces.share.ShareSpaceBottomSheet
import im.vector.app.features.themes.ThemeUtils
import im.vector.app.features.avatars.AvatarRenderer
import im.vector.app.features.home.room.list.home.NewHomeDetailFragment
import im.vector.app.features.sync.InitSyncStepFormatter
import im.vector.app.features.notifications.NotificationPermissionManager
import im.vector.app.features.home.HomeSharedActionViewModel
import im.vector.app.features.home.ShortcutsHandler
import im.vector.app.features.home.discord.*
import im.vector.app.nightlies.NightlyProxy
import org.matrix.android.sdk.api.failure.GlobalError
import im.vector.app.features.workers.signout.ServerBackupStatusViewModel
import im.vector.lib.core.utils.compat.getParcelableExtraCompat
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.permalinks.PermalinkService
import org.matrix.android.sdk.api.session.sync.InitialSyncStrategy
import org.matrix.android.sdk.api.session.sync.SyncRequestState
import org.matrix.android.sdk.api.session.sync.initialSyncStrategy
import org.matrix.android.sdk.api.util.MatrixItem
import timber.log.Timber
import javax.inject.Inject

@Parcelize
data class HomeActivityArgs(
        val clearNotification: Boolean,
        val authenticationDescription: AuthenticationDescription? = null,
        val hasExistingSession: Boolean = false,
        val inviteNotificationRoomId: String? = null
) : Parcelable

@AndroidEntryPoint
class HomeDiscordActivity :
        VectorBaseActivity<ActivityHomeDiscordBinding>(),
        NavigationInterceptor,
        SpaceInviteBottomSheet.InteractionListener,
        MatrixToBottomSheet.InteractionListener,
        VectorMenuProvider {

    private lateinit var sharedActionViewModel: HomeSharedActionViewModel
    private lateinit var roomListSharedActionViewModel: RoomListSharedActionViewModel

    private val homeActivityViewModel: HomeActivityViewModel by viewModel()

    @Suppress("UNUSED")
    private val analyticsAccountDataViewModel: AnalyticsAccountDataViewModel by viewModel()

    private val serverBackupStatusViewModel: ServerBackupStatusViewModel by viewModel()

    @Inject lateinit var vectorUncaughtExceptionHandler: VectorUncaughtExceptionHandler
    @Inject lateinit var notificationDrawerManager: NotificationDrawerManager
    @Inject lateinit var popupAlertManager: PopupAlertManager
    @Inject lateinit var shortcutsHandler: ShortcutsHandler
    @Inject lateinit var permalinkHandler: PermalinkHandler
    @Inject lateinit var avatarRenderer: AvatarRenderer
    @Inject lateinit var initSyncStepFormatter: InitSyncStepFormatter
    @Inject lateinit var spaceStateHandler: SpaceStateHandler
    @Inject lateinit var unifiedPushHelper: UnifiedPushHelper
    @Inject lateinit var nightlyProxy: NightlyProxy
    @Inject lateinit var notificationPermissionManager: NotificationPermissionManager

    private lateinit var serverAdapter: DiscordServerAdapter
    private lateinit var channelAdapter: DiscordChannelAdapter

    companion object {
        fun newIntent(context: Context,
                      clearNotification: Boolean = false,
                      authenticationDescription: AuthenticationDescription? = null,
                      hasExistingSession: Boolean = false): Intent {
            val args = HomeActivityArgs(
                    clearNotification = clearNotification,
                    authenticationDescription = authenticationDescription,
                    hasExistingSession = hasExistingSession
            )

            return Intent(context, HomeDiscordActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, args)
            }
        }
    }

    override val rootView: View
        get() = views.discordMainPanel

    override fun getBinding() = ActivityHomeDiscordBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupDiscordInterface()
        
        // Initialize the main content with the home fragment
        if (savedInstanceState == null) {
            replaceFragment(views.discordMainContent, NewHomeDetailFragment::class.java)
        }
    }

    private fun setupDiscordInterface() {
        // Setup server list
        setupServerList()
        
        // Setup channel list  
        setupChannelList()
        
        // Setup user panel
        setupUserPanel()
        
        // Setup main panel header
        setupMainPanelHeader()
    }

    private fun setupServerList() {
        // Setup adapters
        serverAdapter = DiscordServerAdapter(avatarRenderer) { server ->
            handleServerClick(server)
        }
        
        views.discordServersRecycler.layoutManager = LinearLayoutManager(this)
        views.discordServersRecycler.adapter = serverAdapter
        
        // Setup home button click
        views.discordHomeButton.setOnClickListener {
            navigateToHome()
        }
        
        // Setup add server button click
        views.discordAddServerButton.setOnClickListener {
            showAddServerDialog()
        }
        
        // Load mock server data
        loadMockServers()
    }

    private fun setupChannelList() {
        // Setup adapter
        channelAdapter = DiscordChannelAdapter { channel ->
            handleChannelClick(channel)
        }
        
        views.discordChannelsRecycler.layoutManager = LinearLayoutManager(this)
        views.discordChannelsRecycler.adapter = channelAdapter
        
        // Setup server header click
        views.discordServerHeader.setOnClickListener {
            showServerDropdown()
        }
        
        // Load mock channel data
        loadMockChannels()
    }

    private fun setupUserPanel() {
        // Setup settings button click
        views.discordSettingsButton.setOnClickListener {
            navigator.openSettings(this)
        }
        
        // Setup user avatar and status
        setupUserAvatarAndStatus()
    }

    private fun setupMainPanelHeader() {
        // Setup search button
        views.discordChannelSearch.setOnClickListener {
            openSearch()
        }
        
        // Setup members button
        views.discordChannelMembers.setOnClickListener {
            showMembersList()
        }
    }

    private fun loadMockServers() {
        val mockServers = listOf(
            DiscordServer("1", "Main Server", null, true, 3, true),
            DiscordServer("2", "Gaming", null, false, 0, false),
            DiscordServer("3", "Work", null, false, 1, true),
            DiscordServer("4", "Friends", null, false, 0, false)
        )
        serverAdapter.submitList(mockServers)
    }

    private fun loadMockChannels() {
        val mockChannels = listOf(
            DiscordChannel("cat1", "Text Channels", ChannelType.CATEGORY, false, 0, false, true),
            DiscordChannel("1", "general", ChannelType.TEXT, true, 0, false),
            DiscordChannel("2", "announcements", ChannelType.TEXT, false, 2, true),
            DiscordChannel("3", "random", ChannelType.TEXT, false, 0, false),
            DiscordChannel("cat2", "Voice Channels", ChannelType.CATEGORY, false, 0, false, true),
            DiscordChannel("4", "General", ChannelType.VOICE, false, 0, false),
            DiscordChannel("5", "Gaming", ChannelType.VOICE, false, 0, false)
        )
        channelAdapter.submitList(mockChannels)
    }

    private fun setupUserAvatarAndStatus() {
        views.discordUsername.text = "User"
        views.discordUserStatus.text = "Online"
        
        // Load user avatar - placeholder for now
        views.discordUserAvatar.setImageResource(R.drawable.ic_person)
    }

    private fun handleServerClick(server: DiscordServer) {
        views.discordServerName.text = server.name
        
        // Update selected state
        val currentList = serverAdapter.currentList.toMutableList()
        for (i in currentList.indices) {
            currentList[i] = currentList[i].copy(isSelected = currentList[i].id == server.id)
        }
        serverAdapter.submitList(currentList)
        
        // Load channels for this server
        loadMockChannels()
    }

    private fun handleChannelClick(channel: DiscordChannel) {
        if (channel.type == ChannelType.CATEGORY) {
            // Handle category expand/collapse
            return
        }
        
        views.discordChannelName.text = channel.name
        
        // Update selected state
        val currentList = channelAdapter.currentList.toMutableList()
        for (i in currentList.indices) {
            if (!currentList[i].isCategory) {
                currentList[i] = currentList[i].copy(isSelected = currentList[i].id == channel.id)
            }
        }
        channelAdapter.submitList(currentList)
    }

    private fun navigateToHome() {
        views.discordChannelName.text = "All Chats"
        views.discordServerName.text = "Element"
        
        // Clear server selection
        val currentList = serverAdapter.currentList.toMutableList()
        for (i in currentList.indices) {
            currentList[i] = currentList[i].copy(isSelected = false)
        }
        serverAdapter.submitList(currentList)
    }

    private fun showAddServerDialog() {
        val dialog = DiscordAddServerDialog(this,
            onJoinServer = { inviteCode ->
                handleJoinServer(inviteCode)
            },
            onCreateServer = {
                handleCreateServer()
            }
        )
        dialog.show()
    }

    private fun showServerDropdown() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Server Options")
            .setItems(arrayOf("Server Settings", "Create Invite", "Leave Server")) { _, which ->
                when (which) {
                    0 -> handleServerSettings()
                    1 -> handleCreateInvite()
                    2 -> handleLeaveServer()
                }
            }
            .show()
    }

    private fun openSearch() {
        val dialog = DiscordSearchDialog(this,
            onSearchQuery = { query ->
                // Handle search query
                Timber.d("Search query: $query")
            },
            onItemClick = { result ->
                handleSearchResult(result)
            }
        )
        dialog.show()
    }

    private fun showMembersList() {
        val dialog = DiscordMembersDialog(this) { member ->
            handleMemberClick(member)
        }
        dialog.show()
    }

    private fun handleJoinServer(inviteCode: String) {
        // Handle joining server with invite code
        Timber.d("Joining server with code: $inviteCode")
        // Add to server list
        val currentList = serverAdapter.currentList.toMutableList()
        currentList.add(DiscordServer("new_${System.currentTimeMillis()}", "New Server", null, false, 0, false))
        serverAdapter.submitList(currentList)
    }

    private fun handleCreateServer() {
        // Handle creating new server
        Timber.d("Creating new server")
        val currentList = serverAdapter.currentList.toMutableList()
        currentList.add(DiscordServer("created_${System.currentTimeMillis()}", "My Server", null, false, 0, false))
        serverAdapter.submitList(currentList)
    }

    private fun handleServerSettings() {
        Timber.d("Server settings clicked")
    }

    private fun handleCreateInvite() {
        Timber.d("Create invite clicked")
    }

    private fun handleLeaveServer() {
        Timber.d("Leave server clicked")
    }

    private fun handleSearchResult(result: SearchResult) {
        when (result.type) {
            SearchType.CHANNEL -> {
                views.discordChannelName.text = result.name
            }
            SearchType.USER -> {
                // Navigate to DM with user
                Timber.d("Opening DM with ${result.name}")
            }
            SearchType.MESSAGE -> {
                // Navigate to message
                Timber.d("Navigating to message: ${result.name}")
            }
        }
    }

    private fun handleMemberClick(member: DiscordMember) {
        Timber.d("Member clicked: ${member.name}")
        // Open user profile or start DM
    }

    override fun handleInvalidToken(globalError: GlobalError.InvalidToken) {
        Timber.w("Invalid token received")
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun navToMemberProfile(userId: String, deepLink: Uri): Boolean {
        return false
    }

    override fun navToRoom(roomId: String?, eventId: String?, deepLink: Uri?, rootThreadEventId: String?): Boolean {
        return false
    }

    override fun spaceInviteBottomSheetOnAccept(spaceId: String) {
        // Handle space invite acceptance
    }

    override fun spaceInviteBottomSheetOnDecline(spaceId: String) {
        // Handle space invite decline
    }

    override fun mxToBottomSheetNavigateToRoom(roomId: String, trigger: ViewRoom.Trigger?) {
        // Navigate to room
    }

    override fun mxToBottomSheetSwitchToSpace(spaceId: String) {
        // Switch to space
    }
}