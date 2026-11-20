package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SettingsCell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.device.DeviceEuiccInspector
import com.example.device.QrCodeGenerator
import com.example.model.EsimAccent
import com.example.model.EsimStatus
import com.example.model.VirtualEsim
import com.example.ui.theme.MonoTelemetryStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EsimDetailSheet(
    esim: VirtualEsim,
    onDismiss: () -> Unit,
    onActivateSlot: () -> Unit,
    onSetStandby: () -> Unit,
    onSimulateUsage: () -> Unit,
    onTopUp: () -> Unit,
    onToggleRoaming: () -> Unit,
    onToggleFiveG: () -> Unit,
    onSaveEdit: (label: String, carrier: String, apn: String, notes: String, accent: EsimAccent) -> Unit,
    onDelete: () -> Unit,
    onNotify: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val qrBitmap = remember(esim.lpaString) { QrCodeGenerator.generateQrBitmap(esim.lpaString, 520) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = esim.label,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${esim.carrierName} • ${esim.planTypeEnum.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_close_sheet)
                    )
                }
            }

            VirtualEsimCard(esim = esim, isHero = true)

            // Primary Slot & Quota Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (esim.statusEnum != EsimStatus.ACTIVE) {
                    Button(
                        onClick = onActivateSlot,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("activate_esim_button")
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_activate))
                    }
                } else {
                    OutlinedButton(
                        onClick = onSetStandby,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("standby_esim_button")
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_set_standby))
                    }
                }

                FilledTonalButton(
                    onClick = onSimulateUsage,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("simulate_usage_button")
                ) {
                    Icon(Icons.Rounded.DataUsage, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_use_data))
                }
            }

            if (!esim.unlimitedData) {
                FilledTonalButton(
                    onClick = onTopUp,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("top_up_button")
                ) {
                    Icon(Icons.Rounded.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Top Up +5 GB & Extend +15 Days")
                }
            }

            // LPA QR Provisioning Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.detail_lpa_section),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (qrBitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(196.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White)
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = stringResource(R.string.cd_esim_qr_code),
                                modifier = Modifier.size(176.dp)
                            )
                        }
                    }

                    CopyableTelemetryRow(
                        label = "LPA Activation URI",
                        value = esim.lpaString,
                        onCopy = {
                            clipboard.setText(AnnotatedString(esim.lpaString))
                            onNotify("Copied LPA activation URI")
                        }
                    )

                    OutlinedButton(
                        onClick = {
                            val opened = DeviceEuiccInspector.openSystemEsimSettings(context)
                            if (!opened) {
                                onNotify("Device SIM settings unavailable in this environment.")
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_device_sim_settings_button")
                    ) {
                        Icon(Icons.Rounded.SettingsCell, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.detail_open_device_esim))
                    }
                }
            }

            // Radio Controls
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.detail_telemetry_section),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.detail_roaming), style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = esim.roamingEnabled,
                            onCheckedChange = { onToggleRoaming() },
                            modifier = Modifier.testTag("roaming_switch")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.detail_five_g), style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = esim.fiveGEnabled,
                            onCheckedChange = { onToggleFiveG() },
                            modifier = Modifier.testTag("fiveg_switch")
                        )
                    }
                }
            }

            // SIM Identifiers
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.detail_identifiers_section),
                        style = MaterialTheme.typography.titleMedium
                    )
                    CopyableTelemetryRow("ICCID", esim.iccid) {
                        clipboard.setText(AnnotatedString(esim.iccid))
                        onNotify("Copied ICCID")
                    }
                    CopyableTelemetryRow("SM-DP+ Server", esim.smdpAddress) {
                        clipboard.setText(AnnotatedString(esim.smdpAddress))
                        onNotify("Copied SM-DP+ address")
                    }
                    CopyableTelemetryRow("Activation Code", esim.activationCode) {
                        clipboard.setText(AnnotatedString(esim.activationCode))
                        onNotify("Copied activation code")
                    }
                    CopyableTelemetryRow("APN / MCC-MNC", "${esim.apn}  •  ${esim.mccMnc}") {
                        clipboard.setText(AnnotatedString(esim.apn))
                        onNotify("Copied APN")
                    }
                    if (esim.notes.isNotBlank()) {
                        Text(
                            text = esim.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Edit & Delete Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showEditDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("edit_profile_button")
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.detail_edit_profile))
                }
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("delete_profile_button")
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.detail_delete_profile))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.detail_confirm_delete_title)) },
            text = { Text(stringResource(R.string.detail_confirm_delete_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showEditDialog) {
        EditProfileDialog(
            esim = esim,
            onDismiss = { showEditDialog = false },
            onSave = { label, carrier, apn, notes, accent ->
                showEditDialog = false
                onSaveEdit(label, carrier, apn, notes, accent)
            }
        )
    }
}

@Composable
private fun CopyableTelemetryRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MonoTelemetryStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = stringResource(R.string.cd_copy_value),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    esim: VirtualEsim,
    onDismiss: () -> Unit,
    onSave: (label: String, carrier: String, apn: String, notes: String, accent: EsimAccent) -> Unit
) {
    var label by remember(esim.id) { mutableStateOf(esim.label) }
    var carrier by remember(esim.id) { mutableStateOf(esim.carrierName) }
    var apn by remember(esim.id) { mutableStateOf(esim.apn) }
    var notes by remember(esim.id) { mutableStateOf(esim.notes) }
    var accent by remember(esim.id) { mutableStateOf(esim.accentEnum) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_edit_profile)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { if (it.length <= 50) label = it },
                    label = { Text("Profile Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = carrier,
                    onValueChange = { if (it.length <= 40) carrier = it },
                    label = { Text("Carrier Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apn,
                    onValueChange = { if (it.length <= 60) apn = it },
                    label = { Text("APN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (it.length <= 250) notes = it },
                    label = { Text("Notes") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Card Accent", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EsimAccent.entries.forEach { candidate ->
                        val selected = candidate == accent
                        Surface(
                            onClick = { accent = candidate },
                            shape = CircleShape,
                            color = candidate.primaryColor,
                            modifier = Modifier
                                .size(34.dp)
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(label, carrier, apn, notes, accent) },
                enabled = label.isNotBlank() && carrier.isNotBlank() && apn.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save_changes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
