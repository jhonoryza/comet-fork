package sh.zeron.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun SignInScreen(onSignIn: () -> Unit, isLoading: Boolean = false) {
    Column {
        Button(
            onClick = onSignIn,
            enabled = !isLoading,
            modifier = Modifier.semantics { contentDescription = "Sign in" }
        ) { Text(if (isLoading) "Signing in…" else "Sign in") }
    }
}
