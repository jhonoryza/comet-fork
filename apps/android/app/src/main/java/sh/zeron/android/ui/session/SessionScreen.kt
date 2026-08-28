package sh.zeron.android.ui.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sh.zeron.android.R
import sh.zeron.android.ui.SessionStatus
import sh.zeron.android.ui.theme.ZeronColors
import sh.zeron.android.ui.theme.ZeronSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    title: String,
    status: SessionStatus,
    isArchived: Boolean,
    onBack: () -> Unit,
    transcript: @Composable (PaddingValues) -> Unit,
    composer: @Composable () -> Unit,
) {
    // Children own their insets: the app bar takes the status bar, the composer
    // takes the navigation bar and the IME.
    Scaffold(
        containerColor = ZeronColors.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.session_back),
                                tint = ZeronColors.text,
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                title,
                                style = MaterialTheme.typography.titleMedium,
                                color = ZeronColors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            StatusChip(status, isArchived)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ZeronColors.surface,
                        titleContentColor = ZeronColors.text,
                    ),
                )
                HorizontalDivider(color = ZeronColors.divider)
            }
        },
        bottomBar = { composer() },
    ) { padding ->
        Box(Modifier.fillMaxSize()) { transcript(padding) }
    }
}

/**
 * The connection state, as a short human label. The raw sync string lives in
 * [SessionStatus.Failed.detail] and is revealed by tapping — it used to be
 * printed straight into the app bar, where a message like "history compacted —
 * older messages need checkpoint fetch (not implemented)" wrapped across two
 * lines under the session title.
 */
@Composable
fun StatusChip(status: SessionStatus, isArchived: Boolean = false) {
    val tone: Color = when {
        isArchived -> ZeronColors.textFaint
        status is SessionStatus.Failed -> ZeronColors.danger
        status is SessionStatus.Connected -> ZeronColors.completed
        status is SessionStatus.HistoryTrimmed -> ZeronColors.warning
        else -> ZeronColors.textMuted
    }
    val label =
        if (isArchived) stringResource(R.string.workspace_archived_label)
        else stringResource(status.labelRes)
    val detail = status.detailOrNull.takeUnless { isArchived }

    var showDetail by rememberSaveable { mutableStateOf(false) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ZeronSpacing.xs + 2.dp),
            modifier = if (detail != null) {
                Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable { showDetail = !showDetail }
            } else Modifier,
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(tone))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = tone,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AnimatedVisibility(visible = showDetail && detail != null) {
            Text(
                detail.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = ZeronColors.textFaint,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            )
        }
    }
}
