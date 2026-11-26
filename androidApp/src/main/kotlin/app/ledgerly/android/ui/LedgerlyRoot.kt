package app.ledgerly.android.ui

import app.ledgerly.android.ui.settings.ChangeCurrencyRoute
import app.ledgerly.android.ui.settings.SettingsRoute
import app.ledgerly.shared.presentation.app.AppUiState
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
import androidx.compose.runtime.key
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
import app.ledgerly.android.ui.adddebt.AddDebtRoute
import app.ledgerly.android.ui.budgets.EditBudgetsRoute
import app.ledgerly.android.ui.components.ScopedViewModels
import app.ledgerly.android.ui.debts.DebtsRoute
import app.ledgerly.android.ui.home.HomeRoute
import app.ledgerly.android.ui.quicklog.QuickLogSheet
import app.ledgerly.android.ui.spend.SpendRoute
import app.ledgerly.android.ui.transaction.TransactionDetailRoute
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlyRadius
import app.ledgerly.android.ui.theme.tokens.LedgerlySpacing
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles
import app.ledgerly.shared.domain.model.DebtDirection
import app.ledgerly.shared.domain.model.EntryKind
import app.ledgerly.shared.presentation.debts.DebtsViewModel
import app.ledgerly.shared.presentation.home.HomeViewModel
import app.ledgerly.shared.presentation.quicklog.QuickLogViewModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

private enum class Tab { Home, Spend, Debts }

/** The full-screen page open over the tabs, kept as text so it survives rotation. */
private sealed interface Overlay {
    data class AddDebt(val direction: DebtDirection) : Overlay
    data object Budgets : Overlay
    data class Entry(val id: String) : Overlay
    data object Settings : Overlay
    data object Currency : Overlay

    fun save(): String = when (this) {
        is AddDebt -> "add-debt:${direction.name}"
        Budgets -> "budgets"
        is Entry -> "entry:$id"
        Settings -> "settings"
        Currency -> "currency"
    }

    companion object {
        fun restore(saved: String?): Overlay? = when {
            saved == null -> null
            saved == "budgets" -> Budgets
            saved == "settings" -> Settings
            saved == "currency" -> Currency
            saved.startsWith("add-debt:") -> AddDebt(DebtDirection.valueOf(saved.removePrefix("add-debt:")))
            saved.startsWith("entry:") -> Entry(saved.removePrefix("entry:"))
            else -> null
        }
    }
}

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
    app: AppUiState,
    versionName: String,
    onToggleAmounts: () -> Unit,
    homeViewModel: HomeViewModel = koinViewModel(),
    quickLogViewModel: QuickLogViewModel = koinViewModel(),
    debtsViewModel: DebtsViewModel = koinViewModel(),
) {
    val colors = LedgerlyTheme.colors
    val home by homeViewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    val tabs = TABS.filter { it.id != Tab.Debts || app.trackDebts }
    val shownTab = if (tab == Tab.Debts && !app.trackDebts) Tab.Home else tab
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var logging by rememberSaveable { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var overlaySaved by rememberSaveable { mutableStateOf<String?>(null) }
    val overlay = Overlay.restore(overlaySaved)

    fun open(page: Overlay) {
        menuOpen = false
        overlaySaved = page.save()
    }

    fun close(message: String? = null) {
        overlaySaved = null
        if (message != null) toast = message
    }

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
    BackHandler(enabled = !menuOpen && shownTab != Tab.Home) { tab = Tab.Home }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.canvas),
    ) {
        when (shownTab) {
            Tab.Home -> HomeRoute(
                onLogExpense = { openLog(EntryKind.Expense) },
                onOpenSpend = { tab = Tab.Spend },
                onOpenDebts = { tab = Tab.Debts },
                onOpenEntry = { open(Overlay.Entry(it)) },
                onEditBudgets = { open(Overlay.Budgets) },
                onOpenSettings = { open(Overlay.Settings) },
                amountsHidden = app.hideAmounts,
                onToggleAmounts = onToggleAmounts,
                firstName = app.firstName,
                trackBudgets = app.trackBudgets,
                trackDebts = app.trackDebts,
                viewModel = homeViewModel,
            )
            Tab.Spend -> SpendRoute(
                onOpenEntry = { open(Overlay.Entry(it)) },
                onEditBudgets = { open(Overlay.Budgets) },
                trackBudgets = app.trackBudgets,
            )
            Tab.Debts -> DebtsRoute(
                onAddDebt = { open(Overlay.AddDebt(it)) },
                viewModel = debtsViewModel,
            )
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
                items = tabs,
                selected = shownTab,
                onSelect = {
                    menuOpen = false
                    tab = it
                },
                logMenuOpen = menuOpen,
                onLogClick = { menuOpen = !menuOpen },
                nudge = !home.isLoading && !home.streak.loggedToday,
            )
        }

        overlay?.let { page ->
            key(page) {
                ScopedViewModels {
                    when (page) {
                        is Overlay.AddDebt -> AddDebtRoute(
                            initialDirection = page.direction,
                            onSaved = { message, direction ->
                                debtsViewModel.onSelectDirection(direction)
                                tab = Tab.Debts
                                close(message)
                            },
                            onClose = { close() },
                        )
                        Overlay.Budgets -> EditBudgetsRoute(onSaved = { close("Budgets saved.") }, onClose = { close() })
                        is Overlay.Entry -> TransactionDetailRoute(page.id, onDone = { close(it) }, onClose = { close() })
                        Overlay.Settings -> SettingsRoute(
                            versionName = versionName,
                            onEditBudgets = { open(Overlay.Budgets) },
                            onChangeCurrency = { open(Overlay.Currency) },
                            onClose = { close() },
                        )
                        Overlay.Currency -> ChangeCurrencyRoute(onDone = { close(it) }, onClose = { open(Overlay.Settings) })
                    }
                }
            }
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
