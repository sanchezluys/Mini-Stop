package com.example.update

import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class InAppUpdateUiState(
    val isDownloading: Boolean = false,
    val downloadProgressPercent: Int = 0,
    val isUpdateReadyToInstall: Boolean = false
)

/**
 * Manages Google Play Store In-App Updates following Google's recommended best practices:
 * - Uses Flexible updates by default so players can continue using the app while downloading.
 * - Uses Immediate updates for high-priority (priority >= 4) or long-stale (>= 14 days) updates.
 * - Monitors download progress and surfaces InstallStatus.DOWNLOADED both via listener and onResume().
 * - Resumes stalled developer-triggered immediate updates on onResume().
 */
class InAppUpdateController(private val activity: ComponentActivity) {

    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(activity.applicationContext)
    }

    private val _uiState = MutableStateFlow(InAppUpdateUiState())
    val uiState: StateFlow<InAppUpdateUiState> = _uiState.asStateFlow()

    private var hasPromptedThisSession = false
    private var activeUpdateType: Int? = null
    private var isListenerRegistered = false

    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
            if (result.resultCode != Activity.RESULT_OK) {
                Log.d(TAG, "In-app update flow cancelled or failed: resultCode=${result.resultCode}")
                if (activeUpdateType == AppUpdateType.FLEXIBLE) {
                    _uiState.update { it.copy(isDownloading = false) }
                }
            }
        }

    private val installStateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.PENDING,
            InstallStatus.DOWNLOADING -> {
                val total = state.totalBytesToDownload()
                val downloaded = state.bytesDownloaded()
                val percent = if (total > 0L) {
                    ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
                } else {
                    0
                }
                _uiState.update {
                    it.copy(
                        isDownloading = true,
                        downloadProgressPercent = percent,
                        isUpdateReadyToInstall = false
                    )
                }
            }
            InstallStatus.DOWNLOADED -> {
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        downloadProgressPercent = 100,
                        isUpdateReadyToInstall = true
                    )
                }
            }
            InstallStatus.INSTALLED,
            InstallStatus.CANCELED,
            InstallStatus.FAILED -> {
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        isUpdateReadyToInstall = false
                    )
                }
            }
            else -> Unit
        }
    }

    fun checkForUpdates() {
        try {
            registerListenerIfNeeded()
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->
                    handleAppUpdateInfo(appUpdateInfo, isResumeCheck = false)
                }
                .addOnFailureListener { error ->
                    // Normal in debug/sideloaded builds or devices without Play Store
                    Log.d(TAG, "Unable to query Play Store updates: ${error.message}")
                }
        } catch (e: Exception) {
            Log.d(TAG, "In-app update check skipped: ${e.message}")
        }
    }

    fun onResumeCheck() {
        try {
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->
                    handleAppUpdateInfo(appUpdateInfo, isResumeCheck = true)
                }
                .addOnFailureListener { error ->
                    Log.d(TAG, "onResume update check skipped: ${error.message}")
                }
        } catch (e: Exception) {
            Log.d(TAG, "onResume update check error: ${e.message}")
        }
    }

    fun completeFlexibleUpdate() {
        try {
            appUpdateManager.completeUpdate()
        } catch (e: Exception) {
            Log.w(TAG, "completeUpdate failed: ${e.message}")
        }
    }

    fun onDestroy() {
        if (isListenerRegistered) {
            try {
                appUpdateManager.unregisterListener(installStateListener)
            } catch (e: Exception) {
                Log.d(TAG, "unregisterListener error: ${e.message}")
            }
            isListenerRegistered = false
        }
    }

    private fun registerListenerIfNeeded() {
        if (!isListenerRegistered) {
            appUpdateManager.registerListener(installStateListener)
            isListenerRegistered = true
        }
    }

    private fun handleAppUpdateInfo(appUpdateInfo: AppUpdateInfo, isResumeCheck: Boolean) {
        // 1. Check if a flexible update has already finished downloading and is waiting to install
        if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
            _uiState.update {
                it.copy(
                    isDownloading = false,
                    downloadProgressPercent = 100,
                    isUpdateReadyToInstall = true
                )
            }
            return
        }

        // 2. Resume any stalled immediate update
        if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
            if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                startUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE)
            }
            return
        }

        // 3. Only prompt once per session for newly available updates
        if (isResumeCheck || hasPromptedThisSession) return

        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
            val chosenType = decideUpdateType(
                updatePriority = appUpdateInfo.updatePriority(),
                stalenessDays = appUpdateInfo.clientVersionStalenessDays(),
                isImmediateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
                isFlexibleAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            )
            if (chosenType != null) {
                hasPromptedThisSession = true
                startUpdate(appUpdateInfo, chosenType)
            }
        }
    }

    private fun startUpdate(appUpdateInfo: AppUpdateInfo, updateType: Int) {
        try {
            activeUpdateType = updateType
            if (updateType == AppUpdateType.FLEXIBLE) {
                registerListenerIfNeeded()
            }
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                updateLauncher,
                AppUpdateOptions.newBuilder(updateType).build()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start update flow: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "InAppUpdateController"
        const val HIGH_PRIORITY_THRESHOLD = 4
        const val STALENESS_DAYS_FOR_IMMEDIATE = 14

        /**
         * Determines the recommended Google Play update flow based on priority & staleness:
         * - High priority (>= 4) or long-stale (>= 14 days) -> IMMEDIATE when allowed.
         * - Standard updates -> FLEXIBLE when allowed (fallback to IMMEDIATE if only immediate is allowed).
         */
        fun decideUpdateType(
            updatePriority: Int,
            stalenessDays: Int?,
            isImmediateAllowed: Boolean,
            isFlexibleAllowed: Boolean
        ): Int? {
            val shouldForceImmediate = updatePriority >= HIGH_PRIORITY_THRESHOLD ||
                (stalenessDays != null && stalenessDays >= STALENESS_DAYS_FOR_IMMEDIATE)

            return when {
                shouldForceImmediate && isImmediateAllowed -> AppUpdateType.IMMEDIATE
                isFlexibleAllowed -> AppUpdateType.FLEXIBLE
                isImmediateAllowed -> AppUpdateType.IMMEDIATE
                else -> null
            }
        }
    }
}
