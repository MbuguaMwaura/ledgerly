package app.ledgerly.android.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ledgerly.android.ui.components.LedgerlyButton
import app.ledgerly.android.ui.components.LedgerlyIcon
import app.ledgerly.android.ui.components.LedgerlyIcons
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlySpacing
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles

/** Covers the app while it's locked. The prompt opens by itself once; the button opens it again. */
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val colors = LedgerlyTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.canvas)
            // Nothing underneath can be tapped through the lock screen.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = LedgerlySpacing.space5, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cobalt),
            contentAlignment = Alignment.Center,
        ) { LedgerlyIcon(LedgerlyIcons.Lock, size = 30.dp, tint = colors.onAction) }
        Column(
            Modifier.padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Ledgerly is locked",
                style = LedgerlyTextStyles.title,
                color = colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "Unlock with your fingerprint, face or screen lock.",
                style = LedgerlyTextStyles.body,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.weight(1f))
        LedgerlyButton("Unlock", onUnlock, modifier = Modifier.fillMaxWidth())
    }
}
