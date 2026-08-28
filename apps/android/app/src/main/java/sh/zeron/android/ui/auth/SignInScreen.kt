package sh.zeron.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * WorkOS AuthKit sign-in — one button that opens the hosted login in a browser
 * tab and returns through `zeron://callback` (same flow as iOS SignInView).
 */
@Composable
fun SignInScreen(onLogIn: () -> Unit, isLoading: Boolean = false) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Zeron", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Your coding agents, from anywhere",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp, bottom = 32.dp),
        )
        Button(
            onClick = onLogIn,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Log in to Zeron" },
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.padding(2.dp))
            else Text("Log in to Zeron")
        }
    }
}
