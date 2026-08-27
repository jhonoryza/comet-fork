package sh.zeron.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import sh.zeron.android.auth.AuthOrg

@Composable
fun OrgPickerScreen(orgs: List<AuthOrg>, onSelect: (AuthOrg) -> Unit) {
    Column {
        orgs.forEach { org ->
            Button(onClick = { onSelect(org) }, modifier = Modifier.semantics { contentDescription = "Select org ${org.name}" }) {
                Text(org.name)
            }
        }
    }
}
