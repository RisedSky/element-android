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
        // Setup home button click
        views.discordHomeButton.setOnClickListener {
            // Navigate to home/DMs
        }
        
        // Setup add server button click
        views.discordAddServerButton.setOnClickListener {
            // Show add server dialog
        }
        
        // TODO: Setup RecyclerView for server list
    }

    private fun setupChannelList() {
        // Setup server header click
        views.discordServerHeader.setOnClickListener {
            // Show server dropdown menu
        }
        
        // TODO: Setup RecyclerView for channels
    }

    private fun setupUserPanel() {
        // Setup settings button click
        views.discordSettingsButton.setOnClickListener {
            navigator.openSettings(this)
        }
        
        // TODO: Setup user avatar and status
    }

    private fun setupMainPanelHeader() {
        // Setup search button
        views.discordChannelSearch.setOnClickListener {
            // Open search
        }
        
        // Setup members button
        views.discordChannelMembers.setOnClickListener {
            // Show members list
        }
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