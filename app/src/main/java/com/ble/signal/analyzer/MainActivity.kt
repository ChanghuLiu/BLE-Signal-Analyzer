package com.ble.signal.analyzer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ble.signal.analyzer.localization.AppLanguage
import com.ble.signal.analyzer.ui.BleSignalAnalyzerApp
import com.ble.signal.analyzer.ui.theme.BLESignalAnalyzerTheme
import com.ble.signal.analyzer.data.ble.BleScanErrorKind

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var bluetoothReceiverRegistered = false
    private var permissionRequestInFlight = false

    private val permissionPreferences by lazy {
        getSharedPreferences(PERMISSION_PREFERENCES, MODE_PRIVATE)
    }

    private val requiredRuntimePermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionRequestInFlight = false
        permissionPreferences.edit { putBoolean(KEY_PERMISSION_REQUESTED, true) }
        refreshBluetoothEnvironment(permissionRequestCompleted = true)
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        refreshBluetoothEnvironment()
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                refreshBluetoothEnvironment()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshBluetoothEnvironment()
        enableEdgeToEdge()
        setContent {
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

            BLESignalAnalyzerTheme(themeMode = uiState.themeMode) {
                BleSignalAnalyzerApp(
                    uiState = uiState,
                    onToggleScanning = ::toggleScanningAfterEnvironmentCheck,
                    onFilterApplied = viewModel::applyFilter,
                    onSortChanged = viewModel::setSortMode,
                    onFreezeChanged = viewModel::setFreezeEnabled,
                    onDeviceSelected = viewModel::openDevice,
                    onOpenTracker = ::openTrackerAfterEnvironmentCheck,
                    onResumeTracking = ::resumeTrackingAfterEnvironmentCheck,
                    onOpenSettings = viewModel::openSettings,
                    onOpenPrivacyPolicy = viewModel::openPrivacyPolicy,
                    onOpenHowBleSignalsWork = viewModel::openHowBleSignalsWork,
                    onOpenAbout = viewModel::openAbout,
                    onBack = viewModel::navigateBack,
                    onBackToScanner = viewModel::backToScannerFromTracker,
                    onRequestBluetoothPermission = ::requestBluetoothPermissions,
                    onPermissionNotNow = viewModel::deferPermissionRequest,
                    onOpenAppSettings = ::openAppSettings,
                    onEnableBluetooth = ::requestEnableBluetooth,
                    onScanDurationChanged = viewModel::setScanDuration,
                    onShowUnnamedChanged = viewModel::setShowUnnamedDevices,
                    onMinimumRssiChanged = viewModel::setMinimumRssi,
                    onKeepScreenAwakeChanged = viewModel::setKeepScreenAwake,
                    onThemeChanged = viewModel::setThemeMode,
                    currentLanguage = currentAppLanguage(),
                    onLanguageChanged = ::setAppLanguage,
                    onSignalDescriptionsChanged = viewModel::setSignalDescriptions,
                    onProximityAlertThresholdChanged =
                        viewModel::setProximityAlertThreshold,
                    onProximityAlertEnabledChanged =
                        viewModel::setProximityAlertEnabled,
                    onVibrationConsumed = viewModel::consumeProximityAlertVibration,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!bluetoothReceiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                bluetoothStateReceiver,
                IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            bluetoothReceiverRegistered = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (!permissionRequestInFlight) {
            refreshBluetoothEnvironment()
        }
    }

    override fun onPause() {
        viewModel.onAppPaused()
        super.onPause()
    }

    override fun onStop() {
        if (bluetoothReceiverRegistered) {
            unregisterReceiver(bluetoothStateReceiver)
            bluetoothReceiverRegistered = false
        }
        super.onStop()
    }

    private fun requestBluetoothPermissions() {
        if (currentPermissionState() == BluetoothPermissionState.Granted) {
            refreshBluetoothEnvironment()
            return
        }
        permissionRequestInFlight = true
        permissionLauncher.launch(requiredRuntimePermissions)
    }

    private fun currentPermissionState(): BluetoothPermissionState {
        val deniedPermissions = requiredRuntimePermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED
        }
        val hasRequested = permissionPreferences.getBoolean(KEY_PERMISSION_REQUESTED, false)
        return BluetoothPermissionStateResolver.resolve(
            allGranted = deniedPermissions.isEmpty(),
            hasRequestedBefore = hasRequested,
            allDeniedPermissionsCanShowRationale = deniedPermissions.all { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
            },
        )
    }

    private fun refreshBluetoothEnvironment(permissionRequestCompleted: Boolean = false) {
        val bleSupported = viewModel.isBleSupportedOnDevice()
        val permissionState = currentPermissionState()
        val canReadBluetoothState = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            permissionState == BluetoothPermissionState.Granted
        val bluetoothEnabled = bleSupported && canReadBluetoothState &&
            viewModel.isBluetoothEnabledOnDevice()

        viewModel.updateBluetoothEnvironment(
            bleSupported = bleSupported,
            bluetoothEnabled = bluetoothEnabled,
            permissionState = permissionState,
            permissionRequestCompleted = permissionRequestCompleted,
        )
    }

    private fun requestEnableBluetooth() {
        try {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } catch (_: SecurityException) {
            viewModel.showScanError(BleScanErrorKind.PermissionRequired)
            refreshBluetoothEnvironment()
        }
    }

    private fun toggleScanningAfterEnvironmentCheck() {
        refreshBluetoothEnvironment()
        viewModel.toggleScanning()
    }

    private fun openTrackerAfterEnvironmentCheck() {
        refreshBluetoothEnvironment()
        viewModel.openTracker()
    }

    private fun resumeTrackingAfterEnvironmentCheck() {
        refreshBluetoothEnvironment()
        viewModel.resumeSignalTracking()
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
        startActivity(intent)
    }

    private fun currentAppLanguage(): AppLanguage = AppLanguage.fromLanguageTags(
        AppCompatDelegate.getApplicationLocales().toLanguageTags(),
    )

    private fun setAppLanguage(language: AppLanguage) {
        val locales = language.languageTag?.let(LocaleListCompat::forLanguageTags)
            ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }

    private companion object {
        const val PERMISSION_PREFERENCES = "bluetooth_permission_state"
        const val KEY_PERMISSION_REQUESTED = "permission_requested"
    }
}
