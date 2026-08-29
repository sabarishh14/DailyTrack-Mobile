package com.example.dailytrack_mobile.presentation.screens.forms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailytrack_mobile.presentation.util.Dimens

import androidx.hilt.navigation.compose.hiltViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Add Asset Form
// ─────────────────────────────────────────────────────────────────────────────

private enum class AssetClass(val label: String, val emoji: String) {
    VEHICLE("Vehicle", "🚗"),
    ELECTRONICS("Electronics", "💻"),
    JEWELLERY("Jewellery", "💍"),
    FURNITURE("Furniture", "🪑"),
    APPLIANCE("Appliance", "🏠"),
    GADGET("Gadget", "📱"),
    OTHER("Other", "📦")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddAssetScreen(
    formsVM: FormsVM = hiltViewModel(),
    onDirtyStateChanged: (Boolean) -> Unit = {},
    onSaveSuccess: () -> Unit = {}
) {
    var selectedClass by remember { mutableStateOf<AssetClass?>(null) }

    var assetName by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var currentValue by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val dims = Dimens.current

    val isDirty = remember(selectedClass, assetName, purchasePrice, currentValue, note) {
        selectedClass != null ||
                assetName.isNotBlank() ||
                purchasePrice.isNotBlank() ||
                currentValue.isNotBlank() ||
                note.isNotBlank()
    }

    LaunchedEffect(isDirty) {
        onDirtyStateChanged(isDirty)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.screenHorizontalPadding, vertical = dims.itemSpacingLarge),
        verticalArrangement = Arrangement.spacedBy(dims.sectionSpacing)
    ) {
        // ── Asset Class ──────────────────────────────────────────────
        SectionLabel("Asset Class")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium),
            verticalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
        ) {
            AssetClass.entries.forEach { assetClass ->
                FilterChip(
                    selected = selectedClass == assetClass,
                    onClick = { selectedClass = assetClass },
                    label = { Text("${assetClass.emoji} ${assetClass.label}", style = MaterialTheme.typography.bodyMedium) },
                    shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp)
                )
            }
        }

        // ── Asset Name ───────────────────────────────────────────────
        SectionLabel("Asset Name")
        OutlinedTextField(
            value = assetName,
            onValueChange = { assetName = it },
            placeholder = {
                Text(
                    when (selectedClass) {
                        AssetClass.VEHICLE -> "e.g. Honda City"
                        AssetClass.ELECTRONICS -> "e.g. MacBook Pro"
                        AssetClass.JEWELLERY -> "e.g. Gold Necklace"
                        AssetClass.GADGET -> "e.g. iPhone 16"
                        else -> "e.g. Washing Machine"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(dims.buttonCornerRadius),
            modifier = Modifier.fillMaxWidth()
        )

        // ── Purchase Price ───────────────────────────────────────────
        SectionLabel("Purchase Price")
        OutlinedTextField(
            value = purchasePrice,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() || it == '.' }) purchasePrice = newValue
            },
            placeholder = { Text("0.00", style = MaterialTheme.typography.bodyMedium) },
            prefix = { Text("₹ ", style = MaterialTheme.typography.bodyLarge) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(dims.buttonCornerRadius),
            modifier = Modifier.fillMaxWidth()
        )

        // ── Current Value ────────────────────────────────────────────
        SectionLabel("Current Value (estimated)")
        OutlinedTextField(
            value = currentValue,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() || it == '.' }) currentValue = newValue
            },
            placeholder = { Text("0.00", style = MaterialTheme.typography.bodyMedium) },
            prefix = { Text("₹ ", style = MaterialTheme.typography.bodyLarge) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(dims.buttonCornerRadius),
            modifier = Modifier.fillMaxWidth()
        )

        // ── Note ─────────────────────────────────────────────────────
        SectionLabel("Note (optional)")
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            placeholder = { Text("Any details about this asset", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Outlined.Notes, contentDescription = null, modifier = Modifier.size(dims.iconSizeMedium))
            },
            minLines = 2,
            maxLines = 3,
            shape = RoundedCornerShape(dims.buttonCornerRadius),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        // ── Save Button ──────────────────────────────────────────────
        Button(
            onClick = {
                val pPrice = purchasePrice.toDoubleOrNull() ?: 0.0
                val cVal = currentValue.toDoubleOrNull() ?: pPrice
                formsVM.saveAsset(
                    name = assetName,
                    assetClass = selectedClass?.label ?: "Other",
                    purchasePrice = pPrice,
                    currentValue = cVal,
                    note = note.takeIf { it.isNotBlank() },
                    onSuccess = onSaveSuccess
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.searchBarHeight),
            shape = RoundedCornerShape(dims.buttonCornerRadius),
            enabled = assetName.isNotBlank() && selectedClass != null
        ) {
            Text(
                "Save Asset",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(dims.itemSpacingMedium))
    }
}
