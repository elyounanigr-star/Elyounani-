package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SimCardDownload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CountryPreset
import com.example.model.EsimAccent
import com.example.model.EsimCatalog
import com.example.model.EsimPlanType
import com.example.model.EsimStatus
import com.example.model.VirtualEsim
import com.example.ui.components.VirtualEsimCard
import com.example.ui.theme.MonoTelemetryStyle
import com.example.ui.viewmodel.CreateEsimFormState
import kotlin.math.roundToInt

@Composable
fun CreateEsimScreen(
    form: CreateEsimFormState,
    onApplyPreset: (CountryPreset) -> Unit,
    onUpdateForm: ((CreateEsimFormState) -> CreateEsimFormState) -> Unit,
    onRegenerateIccid: () -> Unit,
    onRegenerateCode: () -> Unit,
    onParseLpaImport: () -> Unit,
    onCreateSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var modeIndex by remember { mutableIntStateOf(0) }

    val previewEsim = VirtualEsim(
        id = "preview",
        label = form.label.ifBlank { "Custom Virtual eSIM" },
        carrierName = form.carrierName.ifBlank { "Virtual Carrier" },
        countryCode = form.countryCode,
        countryName = form.countryName.ifBlank { "Global" },
        planType = form.planType.name,
        status = if (form.activateImmediately) EsimStatus.ACTIVE.name else EsimStatus.STANDBY.name,
        iccid = form.iccid,
        smdpAddress = form.smdpAddress,
        activationCode = form.activationCode,
        apn = form.apn,
        mccMnc = form.mccMnc,
        dataLimitMb = if (form.unlimitedData) 0L else form.dataLimitGb.toLong() * 1024L,
        dataUsedMb = 0L,
        validityDays = form.validityDays.toLong(),
        roamingEnabled = form.roamingEnabled,
        fiveGEnabled = form.fiveGEnabled,
        unlimitedData = form.unlimitedData,
        colorAccent = form.accent.name,
        notes = form.notes
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = stringResource(R.string.create_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.create_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Live Virtual SIM Preview
        item {
            VirtualEsimCard(esim = previewEsim, isHero = true)
        }

        // Mode Switcher: Custom Builder vs Import LPA URI
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = modeIndex == 0,
                    onClick = { modeIndex = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp)) }
                ) {
                    Text(stringResource(R.string.create_mode_builder))
                }
                SegmentedButton(
                    selected = modeIndex == 1,
                    onClick = { modeIndex = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp)) }
                ) {
                    Text(stringResource(R.string.create_mode_import))
                }
            }
        }

        if (modeIndex == 1) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Decode Standard GSMA LPA String", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = form.lpaImportInput,
                            onValueChange = { input -> onUpdateForm { it.copy(lpaImportInput = input) } },
                            label = { Text(stringResource(R.string.create_import_hint)) },
                            textStyle = MonoTelemetryStyle,
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lpa_import_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onUpdateForm {
                                        it.copy(lpaImportInput = "LPA:1\$smdp.eu-roam.net\$LPA-EU-9K2M-4P8Q")
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(stringResource(R.string.create_sample_lpa))
                            }
                            Button(
                                onClick = onParseLpaImport,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("parse_lpa_button")
                            ) {
                                Text(stringResource(R.string.create_parse_lpa))
                            }
                        }
                    }
                }
            }
        }

        // Country / Region Presets
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Region & Carrier Preset", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EsimCatalog.presets) { preset ->
                        FilterChip(
                            selected = form.selectedPresetCode == preset.code,
                            onClick = { onApplyPreset(preset) },
                            label = { Text("${preset.code} • ${preset.name}") },
                            modifier = Modifier.testTag("preset_${preset.code.lowercase()}")
                        )
                    }
                }
            }
        }

        // Plan Type & Accent Picker
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Plan Profile Type", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EsimPlanType.entries) { planType ->
                        FilterChip(
                            selected = form.planType == planType,
                            onClick = { onUpdateForm { it.copy(planType = planType) } },
                            label = { Text(planType.displayName) }
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Card Accent", style = MaterialTheme.typography.titleSmall)
                    EsimAccent.entries.forEach { accent ->
                        val selected = form.accent == accent
                        Surface(
                            onClick = { onUpdateForm { it.copy(accent = accent) } },
                            shape = CircleShape,
                            color = accent.primaryColor,
                            modifier = Modifier
                                .size(34.dp)
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = accent.displayName,
                                    tint = Color.Black,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Core Identity Fields
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Carrier & Profile Identity", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = form.label,
                        onValueChange = { v -> if (v.length <= 50) onUpdateForm { it.copy(label = v) } },
                        label = { Text("Profile Label") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_label_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = form.carrierName,
                            onValueChange = { v -> if (v.length <= 40) onUpdateForm { it.copy(carrierName = v) } },
                            label = { Text("Carrier") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_carrier_input")
                        )
                        OutlinedTextField(
                            value = form.apn,
                            onValueChange = { v -> if (v.length <= 60) onUpdateForm { it.copy(apn = v) } },
                            label = { Text("APN") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // ICCID with Luhn Generator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = form.iccid,
                            onValueChange = { v ->
                                val digits = v.filter { it.isDigit() }.take(22)
                                onUpdateForm { it.copy(iccid = digits) }
                            },
                            label = { Text("ICCID (Luhn-19)") },
                            textStyle = MonoTelemetryStyle,
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onRegenerateIccid,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.create_regenerate_iccid))
                        }
                    }

                    // SM-DP+ & Activation Code
                    OutlinedTextField(
                        value = form.smdpAddress,
                        onValueChange = { v -> if (v.length <= 120) onUpdateForm { it.copy(smdpAddress = v) } },
                        label = { Text("SM-DP+ Server Host") },
                        textStyle = MonoTelemetryStyle,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = form.activationCode,
                            onValueChange = { v -> if (v.length <= 120) onUpdateForm { it.copy(activationCode = v) } },
                            label = { Text("Activation Code") },
                            textStyle = MonoTelemetryStyle,
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onRegenerateCode,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.create_regenerate_code))
                        }
                    }
                }
            }
        }

        // Data Quota & Radio Config
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Allowance & Radio Preferences", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.create_unlimited_toggle), style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = form.unlimitedData,
                            onCheckedChange = { checked -> onUpdateForm { it.copy(unlimitedData = checked) } }
                        )
                    }

                    if (!form.unlimitedData) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Data Allowance", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${form.dataLimitGb} GB",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = form.dataLimitGb.toFloat(),
                                onValueChange = { v -> onUpdateForm { it.copy(dataLimitGb = v.roundToInt().coerceIn(1, 100)) } },
                                valueRange = 1f..100f
                            )
                        }
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Validity Window", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${form.validityDays} Days",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf(7, 15, 30, 90, 365)) { days ->
                                FilterChip(
                                    selected = form.validityDays == days,
                                    onClick = { onUpdateForm { it.copy(validityDays = days) } },
                                    label = { Text("${days}d") }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.detail_roaming), style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = form.roamingEnabled,
                            onCheckedChange = { checked -> onUpdateForm { it.copy(roamingEnabled = checked) } }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.detail_five_g), style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = form.fiveGEnabled,
                            onCheckedChange = { checked -> onUpdateForm { it.copy(fiveGEnabled = checked) } }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.create_activate_now), style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = form.activateImmediately,
                            onCheckedChange = { checked -> onUpdateForm { it.copy(activateImmediately = checked) } }
                        )
                    }

                    OutlinedTextField(
                        value = form.notes,
                        onValueChange = { v -> if (v.length <= 250) onUpdateForm { it.copy(notes = v) } },
                        label = { Text("Notes (optional)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Validation Error + Submit CTA
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                form.validationError?.let { err ->
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                Button(
                    onClick = onCreateSubmit,
                    enabled = !form.isSubmitting,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("create_submit_button")
                ) {
                    if (form.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Provisioning...")
                    } else {
                        Icon(Icons.Rounded.SimCardDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.create_submit_button))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
