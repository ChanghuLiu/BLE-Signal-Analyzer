package com.ble.signal.analyzer.ui.settings

import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ble.signal.analyzer.R
import com.ble.signal.analyzer.localization.AppLanguage
import com.ble.signal.analyzer.ui.components.SectionLabel
import com.ble.signal.analyzer.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appVersion: String,
    scanDurationSeconds: Int,
    showUnnamedDevices: Boolean,
    minimumRssi: Int,
    keepScreenAwake: Boolean,
    themeMode: ThemeMode,
    currentLanguage: AppLanguage,
    signalDescriptions: Boolean,
    proximityAlertThreshold: Int,
    onBack: () -> Unit,
    onScanDurationChanged: (Int) -> Unit,
    onShowUnnamedChanged: (Boolean) -> Unit,
    onMinimumRssiChanged: (Int) -> Unit,
    onKeepScreenAwakeChanged: (Boolean) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
    onSignalDescriptionsChanged: (Boolean) -> Unit,
    onProximityAlertThresholdChanged: (Int) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenHowBleSignalsWork: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    var showScanDurationDialog by rememberSaveable { mutableStateOf(false) }
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    val minimumSignalDescription = stringResource(R.string.minimum_signal)
    val alertThresholdDescription = stringResource(R.string.proximity_alert_threshold)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_to_scanner),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            SectionLabel(text = stringResource(R.string.settings_scan_section))
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSurface {
                SettingsNavigationRow(
                    title = stringResource(R.string.scan_duration),
                    value = pluralStringResource(
                        R.plurals.scan_duration_seconds,
                        scanDurationSeconds,
                        scanDurationSeconds,
                    ),
                    onClick = { showScanDurationDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchSettingRow(
                    title = stringResource(R.string.show_unnamed_devices),
                    description = stringResource(R.string.show_unnamed_devices_description),
                    checked = showUnnamedDevices,
                    onCheckedChange = onShowUnnamedChanged,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        stringResource(R.string.minimum_signal),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.dbm_value, minimumRssi),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = minimumRssi.toFloat(),
                        onValueChange = { onMinimumRssiChanged(it.toInt()) },
                        modifier = Modifier.semantics {
                            contentDescription = minimumSignalDescription
                        },
                        valueRange = -100f..-30f,
                        steps = 69,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchSettingRow(
                    title = stringResource(R.string.keep_screen_awake),
                    description = stringResource(R.string.keep_screen_awake_description),
                    checked = keepScreenAwake,
                    onCheckedChange = onKeepScreenAwakeChanged,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        stringResource(R.string.proximity_alert_threshold),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.dbm_value, proximityAlertThreshold),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = proximityAlertThreshold.toFloat(),
                        onValueChange = {
                            onProximityAlertThresholdChanged(it.toInt())
                        },
                        modifier = Modifier.semantics {
                            contentDescription = alertThresholdDescription
                        },
                        valueRange = -90f..-35f,
                        steps = 54,
                    )
                    Text(
                        text = stringResource(R.string.proximity_alert_threshold_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel(text = stringResource(R.string.settings_display_section))
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSurface {
                SettingsNavigationRow(
                    title = stringResource(R.string.theme),
                    value = themeModeLabel(themeMode),
                    onClick = { showThemeDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsNavigationRow(
                    title = stringResource(R.string.language),
                    value = appLanguageLabel(currentLanguage),
                    onClick = { showLanguageDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchSettingRow(
                    title = stringResource(R.string.signal_descriptions),
                    description = stringResource(R.string.signal_descriptions_description),
                    checked = signalDescriptions,
                    onCheckedChange = onSignalDescriptionsChanged,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel(text = stringResource(R.string.settings_about_section))
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSurface {
                AboutRow(
                    title = stringResource(R.string.how_ble_signals_work_title),
                    onClick = onOpenHowBleSignalsWork,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AboutRow(
                    title = stringResource(R.string.privacy_policy_title),
                    onClick = onOpenPrivacyPolicy,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AboutRow(
                    title = stringResource(R.string.about_title),
                    onClick = onOpenAbout,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.app_version),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = appVersion,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showScanDurationDialog) {
        SelectionDialog(
            title = stringResource(R.string.select_scan_duration),
            options = listOf(15, 30, 60),
            selected = scanDurationSeconds,
            labelFor = { seconds ->
                pluralStringResource(
                    R.plurals.scan_duration_seconds,
                    seconds,
                    seconds,
                )
            },
            onSelected = { seconds ->
                onScanDurationChanged(seconds)
                showScanDurationDialog = false
            },
            onDismiss = { showScanDurationDialog = false },
        )
    }

    if (showThemeDialog) {
        SelectionDialog(
            title = stringResource(R.string.select_theme),
            options = ThemeMode.entries,
            selected = themeMode,
            labelFor = { mode -> themeModeLabel(mode) },
            onSelected = { mode ->
                onThemeChanged(mode)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showLanguageDialog) {
        SelectionDialog(
            title = stringResource(R.string.select_language),
            options = AppLanguage.entries,
            selected = currentLanguage,
            labelFor = { language -> appLanguageLabel(language) },
            onSelected = { language ->
                showLanguageDialog = false
                onLanguageChanged(language)
            },
            onDismiss = { showLanguageDialog = false },
        )
    }
}

@Composable
private fun SettingsSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
    ) {
        Column(content = { content() })
    }
}

@Composable
private fun SwitchSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics(mergeDescendants = true) { }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

@Composable
private fun SettingsNavigationRow(
    title: String,
    value: String? = null,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            value?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AboutRow(title: String, onClick: () -> Unit) {
    SettingsNavigationRow(title = title, onClick = onClick)
}

@Composable
private fun <T> SelectionDialog(
    title: String,
    options: List<T>,
    selected: T,
    labelFor: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = selected == option,
                                role = Role.RadioButton,
                                onClick = { onSelected(option) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == option,
                            onClick = null,
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                        Text(labelFor(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.System -> stringResource(R.string.theme_system)
    ThemeMode.Light -> stringResource(R.string.theme_light)
    ThemeMode.Dark -> stringResource(R.string.theme_dark)
}

@Composable
private fun appLanguageLabel(language: AppLanguage): String = stringResource(
    when (language) {
        AppLanguage.SystemDefault -> R.string.language_system_default
        AppLanguage.English -> R.string.language_english
        AppLanguage.French -> R.string.language_french
        AppLanguage.German -> R.string.language_german
        AppLanguage.Spanish -> R.string.language_spanish
        AppLanguage.PortugueseBrazil -> R.string.language_portuguese_brazil
        AppLanguage.SimplifiedChinese -> R.string.language_simplified_chinese
        AppLanguage.TraditionalChinese -> R.string.language_traditional_chinese
        AppLanguage.Japanese -> R.string.language_japanese
        AppLanguage.Korean -> R.string.language_korean
        AppLanguage.Arabic -> R.string.language_arabic
        AppLanguage.Turkish -> R.string.language_turkish
        AppLanguage.Indonesian -> R.string.language_indonesian
        AppLanguage.Hindi -> R.string.language_hindi
    },
)
