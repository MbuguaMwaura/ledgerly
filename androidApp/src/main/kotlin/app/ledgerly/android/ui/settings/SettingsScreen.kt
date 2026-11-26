package app.ledgerly.android.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ledgerly.android.lock.rememberCanUseAppLock
import app.ledgerly.android.lock.rememberAuthenticator
import app.ledgerly.android.reminders.rememberNotificationPermission
import app.ledgerly.android.ui.components.Chip
import app.ledgerly.android.ui.components.Field
import app.ledgerly.android.ui.components.GroupTitle
import app.ledgerly.android.ui.components.LedgerlyCard
import app.ledgerly.android.ui.components.LedgerlyTextField
import app.ledgerly.android.ui.components.LetterTile
import app.ledgerly.android.ui.components.Page
import app.ledgerly.android.ui.components.RowsCard
import app.ledgerly.android.ui.components.SegmentedControl
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles
import app.ledgerly.shared.domain.model.ThemeChoice
import app.ledgerly.shared.presentation.settings.SettingsUiState
import app.ledgerly.shared.presentation.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsRoute(
    versionName: String,
    onEditBudgets: () -> Unit,
    onClose: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var notificationsBlocked by rememberSaveable { mutableStateOf(false) }
    val askForNotifications = rememberNotificationPermission { granted ->
        notificationsBlocked = !granted
        if (granted) viewModel.onReminderChange(true)
    }
    val authenticate = rememberAuthenticator()
    SettingsScreen(
        state = state,
        canUseAppLock = rememberCanUseAppLock(),
        onAppLockChange = { on ->
            if (on) authenticate("Turn on app lock") { ok -> if (ok) viewModel.onAppLockChange(true) }
            else viewModel.onAppLockChange(false)
        },
        versionName = versionName,
        onNameChange = viewModel::onNameChange,
        onThemeChange = viewModel::onThemeChange,
        onTrackBudgetsChange = viewModel::onTrackBudgetsChange,
        onTrackDebtsChange = viewModel::onTrackDebtsChange,
        notificationsBlocked = notificationsBlocked,
        onReminderChange = { on -> if (on) askForNotifications() else viewModel.onReminderChange(false) },
        onReminderTimeChange = viewModel::onReminderTimeChange,
        onEditBudgets = onEditBudgets,
        onClose = onClose,
    )
}

private data class SettingItem(
    val key: String,
    val title: String,
    val sub: String? = null,
    val value: String? = null,
    val checked: Boolean? = null,
    val enabled: Boolean = true,
    val onCheckedChange: (Boolean) -> Unit = {},
    val onClick: (() -> Unit)? = null,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    canUseAppLock: Boolean,
    onAppLockChange: (Boolean) -> Unit,
    versionName: String,
    onNameChange: (String) -> Unit,
    onThemeChange: (ThemeChoice) -> Unit,
    onTrackBudgetsChange: (Boolean) -> Unit,
    onTrackDebtsChange: (Boolean) -> Unit,
    notificationsBlocked: Boolean,
    onReminderChange: (Boolean) -> Unit,
    onReminderTimeChange: (Int) -> Unit,
    onEditBudgets: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = LedgerlyTheme.colors
    var name by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.isLoading) { if (!state.isLoading && name == null) name = state.name }

    Page(title = "Settings", onClose = onClose, closeLabel = "Back", back = true) {
        if (state.isLoading) return@Page
        item("profile") {
            LedgerlyCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    LetterTile(state.initials.ifEmpty { "?" }, colors.cobaltSoft, colors.cobalt)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(state.name.ifEmpty { "You" }, style = LedgerlyTextStyles.heading, color = colors.ink)
                        if (state.profileLine.isNotEmpty()) {
                            Text(state.profileLine, style = LedgerlyTextStyles.caption, color = colors.inkMuted)
                        }
                    }
                }
                Field("Your name", Modifier.padding(top = 16.dp)) {
                    LedgerlyTextField(name.orEmpty(), { name = it; onNameChange(it) }, placeholder = "What should we call you?")
                }
            }
        }

        item("appearance-head") { GroupTitle("Appearance") }
        item("appearance") {
            SegmentedControl(
                options = ThemeChoice.entries,
                selected = state.theme,
                label = {
                    when (it) {
                        ThemeChoice.System -> "Match phone"
                        ThemeChoice.Light -> "Light"
                        ThemeChoice.Dark -> "Dark"
                    }
                },
                onSelect = onThemeChange,
            )
        }

        item("money-head") { GroupTitle("Money") }
        item("money") {
            SettingRows(
                buildList {
                    if (state.trackBudgets) add(SettingItem("budgets", "Budgets", sub = state.budgetsText, onClick = onEditBudgets))
                    add(SettingItem("currency", "Currency", sub = "Chosen when you started", value = state.currencyText))
                },
            )
        }

        item("habit-head") { GroupTitle("Daily habit") }
        item("habit") {
            SettingRows(
                listOf(
                    SettingItem("reminder", "Daily reminder", sub = state.reminderText, checked = state.reminderOn, onCheckedChange = onReminderChange),
                    SettingItem("freezes", "Streak freezes", sub = "Each one covers a day you miss", value = state.freezesText),
                ),
            )
        }
        if (state.reminderOn) {
            item("reminder-time") {
                FlowRow(
                    Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.reminderOptions.forEach { option ->
                        Chip(option.label, selected = option.minutes == state.reminderMinutes, onClick = { onReminderTimeChange(option.minutes) })
                    }
                }
            }
        } else if (notificationsBlocked) {
            item("reminder-blocked") {
                Text(
                    "Notifications are off for Ledgerly. Turn them on in your phone's settings to get the reminder.",
                    style = LedgerlyTextStyles.caption,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
            }
        }

        item("privacy-head") { GroupTitle("Privacy") }
        item("privacy") {
            SettingRows(
                listOf(
                    SettingItem(
                        "app-lock",
                        "App lock",
                        sub = if (canUseAppLock || state.appLock) {
                            "Ask for your fingerprint, face or screen lock when Ledgerly opens"
                        } else {
                            "Set up a screen lock on your phone to use this"
                        },
                        checked = state.appLock,
                        enabled = canUseAppLock || state.appLock,
                        onCheckedChange = onAppLockChange,
                    ),
                ),
            )
        }

        item("track-head") { GroupTitle("What you track") }
        item("track") {
            SettingRows(
                listOf(
                    SettingItem("spending", "Spending", sub = "Always on. It's what your streak counts.", checked = true, enabled = false),
                    SettingItem("track-budgets", "Budgets", checked = state.trackBudgets, onCheckedChange = onTrackBudgetsChange),
                    SettingItem("track-debts", "Debts", checked = state.trackDebts, onCheckedChange = onTrackDebtsChange),
                ),
            )
        }
        item("track-note") {
            Text(
                "Turning one off hides its tab and cards. Nothing is deleted.",
                style = LedgerlyTextStyles.caption,
                color = colors.inkMuted,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }

        item("about-head") { GroupTitle("About") }
        item("about") {
            SettingRows(
                listOf(
                    SettingItem("version", "Version", value = versionName),
                    SettingItem("data", "Your data", sub = "Stored only on this phone"),
                ),
            )
        }
    }
}

@Composable
private fun SettingRows(items: List<SettingItem>) {
    val colors = LedgerlyTheme.colors
    RowsCard(items) { item ->
        val toggle = item.checked
        Row(
            Modifier
                .fillMaxWidth()
                .then(
                    when {
                        toggle != null -> Modifier.clickable(enabled = item.enabled, role = Role.Switch) { item.onCheckedChange(!toggle) }
                        item.onClick != null -> Modifier.clickable(role = Role.Button, onClick = item.onClick)
                        else -> Modifier
                    },
                )
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.title, style = LedgerlyTextStyles.body, color = colors.ink)
                item.sub?.let { Text(it, style = LedgerlyTextStyles.caption, color = colors.inkMuted) }
            }
            item.value?.let { Text(it, style = LedgerlyTextStyles.label, color = colors.inkMuted) }
            if (toggle != null) {
                Switch(
                    checked = toggle,
                    onCheckedChange = null,
                    enabled = item.enabled,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = colors.cobalt,
                        checkedThumbColor = colors.surface,
                        uncheckedTrackColor = colors.surfaceSunk,
                        uncheckedBorderColor = colors.line,
                    ),
                )
            } else if (item.onClick != null) {
                Text("›", style = LedgerlyTextStyles.heading, color = colors.inkFaint)
            }
        }
    }
}
