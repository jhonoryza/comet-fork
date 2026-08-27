package sh.zeron.android.ui

import androidx.compose.runtime.Composable
import sh.zeron.android.sync.AppState

@Composable
fun AppRoot(state: AppState, onSignIn: () -> Unit, onOrgSelect: (String) -> Unit) {
    when (state) {
        is AppState.SignedOut -> SignInScreen(onSignIn)
        is AppState.SelectingOrg -> OrgPickerScreen(state.orgs, onOrgSelect)
        is AppState.Ready -> WorkspaceScreenPlaceholder()
        is AppState.Fatal -> FatalScreen(state.message)
        else -> LoadingScreen()
    }
}

@Composable private fun LoadingScreen() {}
@Composable private fun FatalScreen(msg: String) {}
@Composable private fun WorkspaceScreenPlaceholder() {}
