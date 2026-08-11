package com.ble.signal.analyzer.ui.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.selection.SelectionContainer
import com.ble.signal.analyzer.R

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    InformationScreen(
        title = stringResource(R.string.privacy_policy_title),
        backContentDescription = stringResource(R.string.navigate_back),
        onBack = onBack,
    ) {
        InformationHeading(stringResource(R.string.privacy_policy_heading))
        BodyText(stringResource(R.string.privacy_intro))

        InformationSubheading(stringResource(R.string.privacy_app_does_not))
        BulletList(
            stringResource(R.string.privacy_no_accounts),
            stringResource(R.string.privacy_no_login),
            stringResource(R.string.privacy_no_personal_info),
            stringResource(R.string.privacy_no_scan_upload),
            stringResource(R.string.privacy_no_address_upload),
            stringResource(R.string.privacy_no_server_history),
            stringResource(R.string.privacy_no_ads),
            stringResource(R.string.privacy_no_analytics),
            stringResource(R.string.privacy_no_sale),
        )

        InformationSubheading(stringResource(R.string.privacy_memory_intro))
        BulletList(
            stringResource(R.string.privacy_device_name),
            stringResource(R.string.privacy_signal_strength),
            stringResource(R.string.privacy_manufacturer_data),
            stringResource(R.string.privacy_service_uuids),
            stringResource(R.string.privacy_device_address),
            stringResource(R.string.privacy_tx_power),
        )

        InformationSubheading(stringResource(R.string.privacy_settings_intro))
        BulletList(
            stringResource(R.string.privacy_theme),
            stringResource(R.string.privacy_scan_duration),
            stringResource(R.string.privacy_minimum_rssi),
            stringResource(R.string.privacy_signal_preferences),
            stringResource(R.string.privacy_alert_threshold),
            stringResource(R.string.privacy_language_preference),
        )

        BodyText(stringResource(R.string.privacy_permission_prompt_state))
        BodyText(stringResource(R.string.privacy_storage_summary))
        BodyText(stringResource(R.string.privacy_export_summary))
        BodyText(stringResource(R.string.privacy_permissions))
        ContactSection()
    }
}

@Composable
fun HowBleSignalsWorkScreen(onBack: () -> Unit) {
    InformationScreen(
        title = stringResource(R.string.how_ble_signals_work_title),
        backContentDescription = stringResource(R.string.navigate_back),
        onBack = onBack,
    ) {
        BodyText(stringResource(R.string.rssi_explanation))

        InformationSubheading(stringResource(R.string.example_readings))
        MeasurementExample(stringResource(R.string.reading_very_strong))
        MeasurementExample(stringResource(R.string.reading_good))
        MeasurementExample(stringResource(R.string.reading_weak))

        InformationSubheading(stringResource(R.string.not_exact_distance_heading))
        BodyText(stringResource(R.string.not_exact_distance_body))

        InformationSubheading(stringResource(R.string.signal_affected_by))
        BulletList(
            stringResource(R.string.factor_walls),
            stringResource(R.string.factor_furniture),
            stringResource(R.string.factor_people),
            stringResource(R.string.factor_orientation),
            stringResource(R.string.factor_transmitter_power),
            stringResource(R.string.factor_antenna_design),
            stringResource(R.string.factor_interference),
            stringResource(R.string.factor_reflections),
        )

        InformationSubheading(stringResource(R.string.how_to_use))
        NumberedList(
            stringResource(R.string.how_step_select),
            stringResource(R.string.how_step_tracker),
            stringResource(R.string.how_step_move),
            stringResource(R.string.how_step_observe),
            stringResource(R.string.how_step_compare),
        )
    }
}

@Composable
fun AboutScreen(
    appVersion: String,
    onBack: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenHowBleSignalsWork: () -> Unit,
) {
    InformationScreen(
        title = stringResource(R.string.about_title),
        backContentDescription = stringResource(R.string.navigate_back),
        onBack = onBack,
    ) {
        BodyText(stringResource(R.string.about_intro))

        InformationSubheading(stringResource(R.string.features))
        BulletList(
            stringResource(R.string.feature_ble_scanning),
            stringResource(R.string.feature_compare_devices),
            stringResource(R.string.feature_signal_stability),
            stringResource(R.string.feature_advertisement_inspector),
            stringResource(R.string.feature_ble_environment),
            stringResource(R.string.feature_csv_export),
            stringResource(R.string.feature_realtime_rssi),
            stringResource(R.string.feature_signal_labels),
            stringResource(R.string.feature_trend),
            stringResource(R.string.feature_graph),
            stringResource(R.string.feature_advertisement_info),
            stringResource(R.string.feature_vibration),
            stringResource(R.string.feature_local),
            stringResource(R.string.feature_no_ads),
            stringResource(R.string.feature_no_account),
            stringResource(R.string.feature_no_cloud),
        )

        InformationSubheading(stringResource(R.string.disclaimer_title))
        BodyText(stringResource(R.string.disclaimer))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp,
        ) {
            Column {
                InformationValueRow(
                    label = stringResource(R.string.version),
                    value = appVersion,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InformationNavigationRow(
                    title = stringResource(R.string.privacy_policy_title),
                    onClick = onOpenPrivacyPolicy,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InformationNavigationRow(
                    title = stringResource(R.string.how_ble_signals_work_title),
                    onClick = onOpenHowBleSignalsWork,
                )
            }
        }
        ContactSection()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InformationScreen(
    title: String,
    backContentDescription: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backContentDescription,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = { content() }) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun InformationHeading(text: String) {
    Text(
        text = text,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun InformationSubheading(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .padding(top = 6.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun BodyText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun BulletList(vararg items: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "•", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = item,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun NumberedList(vararg items: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { index, item ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "${index + 1}.", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = item,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun MeasurementExample(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ContactSection() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        InformationSubheading(stringResource(R.string.support_contact))
        SelectionContainer {
            Text(
                text = stringResource(R.string.support_email),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun InformationValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InformationNavigationRow(title: String, onClick: () -> Unit) {
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
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
