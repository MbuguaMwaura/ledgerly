package app.ledgerly.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ledgerly.android.ui.components.LedgerlyIcons
import app.ledgerly.android.ui.components.LogMenuPill
import app.ledgerly.android.ui.components.TabBar
import app.ledgerly.android.ui.components.TabItem
import app.ledgerly.android.ui.components.floatShadow
import app.ledgerly.android.ui.debts.DebtsRoute
import app.ledgerly.android.ui.home.HomeRoute
import app.ledgerly.android.ui.quicklog.QuickLogSheet
import app.ledgerly.android.ui.spend.SpendRoute
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlyRadius
import app.ledgerly.android.ui.theme.tokens.LedgerlySpacing
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles
import app.ledgerly.shared.domain.model.EntryKind
import app.ledgerly.shared.presentation.home.HomeViewModel
import app.ledgerly.shared.presentation.quicklog.QuickLogViewModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

private enum class Tab { Home, Spend, Debts }

private val TABS = listOf(
    TabItem(Tab.Home, "Home", LedgerlyIcons.Home),
    TabItem(Tab.Spend, "Spend", LedgerlyIcons.Wallet),
    TabItem(Tab.Debts, "Debts", LedgerlyIcons.People),
)

/**
 * The app shell: three tabs under the floating tab bar, the + menu, the Quick log sheet and the
 * confirmation toast. Every ViewModel here is the activity's, so Home and the + share one.
 */
@Composable
fun LedgerlyRoot(
    homeViewModel: HomeViewModel = koinViewModel(),
    quickLogViewModel: QuickLogViewModel = koinViewModel(),
) {
    val colors = LedgerlyTheme.colors
    val home by homeViewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var logging by rememberSaveable { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    fun openLog(kind: EntryKind) {
        menuOpen = false
        quickLogViewModel.reset()
        quickLogViewModel.onKindChange(kind)
        logging = true
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(3_000)
            toast = null
        }
    }
    BackHandler(enabled = menuOpen) { menuOpen = false }
    BackHandler(enabled = !menuOpen && tab != Tab.Home) { tab = Tab.Home }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.canvas),
    ) {
        when (tab) {
            Tab.Home -> HomeRoute(
                onLogExpense = { openLog(EntryKind.Expense) },
                onOpenSpend = { tab = Tab.Spend },
                onOpenDebts = { tab = Tab.Debts },
                viewModel = homeViewModel,
            )
            Tab.Spend -> SpendRoute()
            Tab.Debts -> DebtsRoute()
        }

        if (menuOpen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x660D0D12))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { menuOpen = false },
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = LedgerlySpacing.space5, end = LedgerlySpacing.space5, bottom = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedVisibility(menuOpen, enter = fadeIn() + slideInVertically { it / 3 }, exit = fadeOut() + slideOutVertically { it / 3 }) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(LedgerlySpacing.space2)) {
                    LogMenuPill("Expense", LedgerlyIcons.Up, { openLog(EntryKind.Expense) })
                    LogMenuPill("Income", LedgerlyIcons.Down, { openLog(EntryKind.Income) })
                }
            }
            toast?.let { message ->
                Text(
                    message,
                    style = LedgerlyTextStyles.label,
                    color = colors.onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .floatShadow()
                        .clip(RoundedCornerShape(LedgerlyRadius.radiusMd))
                        .background(colors.action)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            TabBar(
                items = TABS,
                selected = tab,
                onSelect = {
                    menuOpen = false
                    tab = it
                },
                logMenuOpen = menuOpen,
                onLogClick = { menuOpen = !menuOpen },
                nudge = !home.isLoading && !home.streak.loggedToday,
            )
        }
    }

    if (logging) {
        QuickLogSheet(
            viewModel = quickLogViewModel,
            showNothingSpent = !home.streak.loggedToday,
            onNothingSpent = {
                homeViewModel.onNothingSpentToday()
                logging = false
                toast = "Nothing spent today. Your streak is safe."
            },
            onSaved = { message ->
                logging = false
                toast = message
            },
            onDismiss = { logging = false },
        )
    }
}
