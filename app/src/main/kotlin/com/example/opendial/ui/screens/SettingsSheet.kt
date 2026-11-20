package com.example.opendial.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.opendial.R
import com.example.opendial.ui.AdUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(ads: AdUiState, onPrivacyOptions: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(8.dp))
            SettingRow(
                Icons.Filled.Star,
                title = if (ads.adFree) stringResource(R.string.ad_free_active, ads.minutesLeft) else "Ads on",
                body = "Watch a rewarded ad on the player screen for an hour without ads.",
            )
            // Google requires a way back to the consent choices wherever the consent form applied.
            if (ads.privacyOptionsRequired) {
                SettingRow(
                    Icons.Filled.Lock,
                    title = stringResource(R.string.privacy_options),
                    body = stringResource(R.string.privacy_options_body),
                    onClick = onPrivacyOptions,
                )
            }
            SettingRow(Icons.Filled.Info, title = "About", body = stringResource(R.string.about_data))
        }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, body: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
