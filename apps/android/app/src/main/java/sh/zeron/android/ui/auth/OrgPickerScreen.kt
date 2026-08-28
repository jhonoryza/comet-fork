package sh.zeron.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import sh.zeron.android.auth.AuthOrg
import sh.zeron.android.ui.theme.ZeronColors

@Composable
fun OrgPickerScreen(orgs: List<AuthOrg>, onSelect: (AuthOrg) -> Unit) {
    Box(Modifier.fillMaxSize().background(ZeronColors.bg)) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Choose a workspace",
                style = MaterialTheme.typography.titleMedium,
                color = ZeronColors.text,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            orgs.forEach { org ->
                Row(org, onSelect)
            }
        }
    }
}

@Composable
private fun Row(org: AuthOrg, onSelect: (AuthOrg) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ZeronColors.surface)
            .clickable { onSelect(org) }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { contentDescription = "Select workspace ${org.name}" },
    ) {
        Text(org.name, style = MaterialTheme.typography.bodyLarge, color = ZeronColors.text)
        Text(org.organizationId, style = MaterialTheme.typography.labelSmall, color = ZeronColors.textFaint)
    }
}
