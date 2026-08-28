package sh.zeron.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import sh.zeron.android.config.EdgeConfig

/**
 * WorkOS paste-code sign-in — the same flow as iOS: open the edge sign-in page
 * on any device, paste the code it shows, the app exchanges it for tokens.
 */
@Composable
fun SignInScreen(onSignInWithCode: (String) -> Unit, isLoading: Boolean = false) {
    var code by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Zeron")
        Text("Edge: ${EdgeConfig.edgeBaseUrl}")
        Text("Open ${EdgeConfig.edgeBaseUrl}/auth/signin on any device, then paste the code below.")
        TextField(
            value = code,
            onValueChange = { code = it },
            enabled = !isLoading,
            label = { Text("Sign-in code") },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Sign-in code" },
        )
        Button(
            onClick = { onSignInWithCode(code) },
            enabled = !isLoading && code.isNotBlank(),
            modifier = Modifier.semantics { contentDescription = "Sign in" },
        ) { Text(if (isLoading) "Signing in…" else "Sign in") }
    }
}