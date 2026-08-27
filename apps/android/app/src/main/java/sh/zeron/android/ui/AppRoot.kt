package sh.zeron.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import sh.zeron.android.data.ChatRow
import sh.zeron.android.sync.AppState
import sh.zeron.android.ui.workspace.WorkspaceScreen

@Composable
fun AppRoot(state: AppState, onSignIn: () -> Unit, onOrgSelect: (String) -> Unit) {
    when (state) {
        is AppState.SignedOut -> SignInScreen(onSignIn)
        is AppState.SelectingOrg -> OrgPickerScreen(state.orgs, onOrgSelect)
        is AppState.Ready -> WorkspaceScreen(
            chats = listOf(
                ChatRow("1", "Demo session — hello", archived = false, spaceId = null),
                ChatRow("2", "Archived example", archived = true, spaceId = null),
            ),
            onOpen = {},
            onArchive = {},
        )
        is AppState.Fatal -> FatalScreen(state.message)
        else -> LoadingScreen()
    }
}

@Composable private fun LoadingScreen() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Connecting…") } }
@Composable private fun FatalScreen(msg: String) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Error: $msg") } }
