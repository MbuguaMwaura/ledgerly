package app.ledgerly.shared.presentation

import app.cash.turbine.test
import app.ledgerly.shared.domain.model.Budget
import app.ledgerly.shared.domain.model.BudgetMonth
import app.ledgerly.shared.domain.model.ThemeChoice
import app.ledgerly.shared.domain.usecase.LogNothingSpent
import app.ledgerly.shared.money.CurrencyCode
import app.ledgerly.shared.presentation.app.AppViewModel
import app.ledgerly.shared.presentation.onboarding.OnboardingStep
import app.ledgerly.shared.presentation.onboarding.OnboardingViewModel
import app.ledgerly.shared.presentation.settings.SettingsViewModel
import app.ledgerly.shared.testing.nairobi
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingAndSettingsTest : ViewModelTest() {
    @Test
    fun onboardingWalksTheStepsAndSaves() = test {
        val app = AppViewModel(settings, logs, clock)
        val vm = OnboardingViewModel(settings, clock)
        assertFalse(app.uiState.first { !it.isLoading }.isOnboarded)
        vm.uiState.test {
            assertEquals(OnboardingStep.Welcome, awaitItem().step)
            vm.onNext()
            val about = awaitState { it.step == OnboardingStep.AboutYou }
            assertFalse(about.canContinue)
            assertEquals(1, about.progress)
            assertEquals(4, about.progressCount)

            vm.onNameChange("Wanjiru Kamau")
            vm.onCurrencyChange(CurrencyCode.UGX)
            assertTrue(awaitState { it.currency == CurrencyCode.UGX && it.canContinue }.canContinue)
            vm.onNext()
            awaitState { it.step == OnboardingStep.Track }

            vm.onTrackBudgetsChange(false)
            assertEquals(3, awaitState { !it.trackBudgets }.progressCount)
            vm.onNext()
            awaitState { it.step == OnboardingStep.Reminder }
            vm.onBack()
            awaitState { it.step == OnboardingStep.Track }
            vm.onNext()
            assertEquals("Remind me at 20:00", awaitState { it.step == OnboardingStep.Reminder }.reminderButton)
            vm.onReminderTimeChange(21 * 60)
            assertEquals("Remind me at 21:00", awaitState { it.reminderMinutes == 21 * 60 }.reminderButton)
            vm.onReminderAnswer(true)
            awaitState { it.step == OnboardingStep.Done }
            vm.onBack()
            awaitState { it.step == OnboardingStep.Reminder }
            vm.onReminderAnswer(true)
            awaitState { it.step == OnboardingStep.Done }

            vm.onFinish()
            awaitState { it.isFinished }
        }
        val saved = settings.observe().first()
        assertEquals("Wanjiru Kamau", saved.name)
        assertEquals(CurrencyCode.UGX, saved.currency)
        assertFalse(saved.trackBudgets)
        assertEquals(LocalDate(2026, 11, 25), saved.startedOn)
        assertTrue(saved.reminderOn)
        assertEquals(21 * 60, saved.reminderMinutes)

        val shell = app.uiState.first { it.isOnboarded }
        assertEquals("Wanjiru", shell.firstName)
        assertFalse(shell.trackBudgets)
        // 18:00 now, nothing logged: today's 21:00 is the first of a week of reminders.
        assertEquals(7, shell.reminderTimes.size)
        assertEquals(nairobi("2026-11-25T21:00").toEpochMilliseconds(), shell.reminderTimes.first())
    }

    @Test
    fun appLockLocksOnStartAndWhenHidden() = test {
        settings.update { it.copy(name = "Otieno", startedOn = LocalDate(2026, 11, 20)) }
        val app = AppViewModel(settings, logs, clock)
        assertFalse(app.uiState.first { !it.isLoading }.isLocked)

        // Hiding the app with the lock off doesn't lock it once the lock is turned on.
        app.onAppHidden()
        settings.update { it.copy(appLock = true) }
        val on = app.uiState.first { it.appLock }
        assertFalse(on.isLocked)

        app.onAppHidden()
        assertTrue(app.uiState.first { it.isLocked }.isLocked)
        app.onUnlocked()
        assertFalse(app.uiState.first { !it.isLocked }.isLocked)

        // A fresh start with the lock on begins locked.
        val fresh = AppViewModel(settings, logs, clock)
        assertTrue(fresh.uiState.first { !it.isLoading }.isLocked)
    }

    @Test
    fun homeTogglesHiddenAmounts() = test {
        val app = AppViewModel(settings, logs, clock)
        assertFalse(app.uiState.first { !it.isLoading }.hideAmounts)
        app.onHideAmountsChange(true)
        assertTrue(app.uiState.first { it.hideAmounts }.hideAmounts)
        assertTrue(settings.observe().first().hideAmounts)
    }

    @Test
    fun loggingTodayDropsTodaysReminder() = test {
        settings.update { it.copy(name = "Otieno", startedOn = LocalDate(2026, 11, 20), reminderOn = true) }
        val app = AppViewModel(settings, logs, clock)
        assertEquals(nairobi("2026-11-25T20:00").toEpochMilliseconds(), app.uiState.first { it.reminderTimes.isNotEmpty() }.reminderTimes.first())
        LogNothingSpent(logs, clock)()
        assertEquals(nairobi("2026-11-26T20:00").toEpochMilliseconds(), app.uiState.first { it.reminderTimes.firstOrNull() != nairobi("2026-11-25T20:00").toEpochMilliseconds() }.reminderTimes.first())
        settings.update { it.copy(reminderOn = false) }
        assertTrue(app.uiState.first { it.reminderTimes.isEmpty() }.reminderTimes.isEmpty())
    }

    @Test
    fun budgetsStepSkipsToDoneOnceSaved() = test {
        val vm = OnboardingViewModel(settings, clock)
        vm.uiState.test {
            vm.onNext()
            vm.onNameChange("Otieno")
            awaitState { it.canContinue && it.step == OnboardingStep.AboutYou }
            vm.onNext()
            vm.onNext()
            awaitState { it.step == OnboardingStep.Budgets }
            vm.onBudgetsSaved()
            awaitState { it.step == OnboardingStep.Reminder }
            vm.onReminderAnswer(false)
            assertEquals("Your budgets are set. Home shows what's left each day.", awaitState { it.step == OnboardingStep.Done }.doneText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun settingsShowsTheProfileAndSavesChoices() = test {
        settings.update { it.copy(name = "Wanjiru Kamau", startedOn = LocalDate(2026, 11, 2)) }
        budgets.save(Budget("food", BudgetMonth(2026, 11), kes(10_000)), clock.now())
        budgets.save(Budget("rent", BudgetMonth(2026, 11), kes(20_000)), clock.now())
        LogNothingSpent(logs, clock)()
        val vm = SettingsViewModel(settings, budgets, logs, clock)
        vm.uiState.test {
            val state = awaitState { !it.isLoading && it.name.isNotEmpty() }
            assertEquals("WK", state.initials)
            assertEquals("Logging since Nov 2026 · 1-day streak", state.profileLine)
            assertEquals("Kenyan shilling (KES)", state.currencyText)
            assertEquals("KES 30,000 a month across 2 categories", state.budgetsText)

            vm.onThemeChange(ThemeChoice.Dark)
            vm.onTrackDebtsChange(false)
            val changed = awaitState { it.theme == ThemeChoice.Dark && !it.trackDebts }
            assertTrue(changed.trackBudgets)
            assertEquals("Off", changed.reminderText)
            assertFalse(changed.appLock)
            vm.onAppLockChange(true)
            assertTrue(awaitState { it.appLock }.appLock)

            vm.onHideAmountsChange(true)
            assertEquals("KES •••• a month across 2 categories", awaitState { it.hideAmounts }.budgetsText)

            vm.onReminderChange(true)
            vm.onReminderTimeChange(19 * 60 + 30)
            val reminder = awaitState { it.reminderOn && it.reminderMinutes == 19 * 60 + 30 }
            assertEquals("Every day at 19:30 if nothing is logged yet", reminder.reminderText)
            assertEquals(listOf("08:00", "13:00", "19:00", "19:30", "20:00", "21:00"), reminder.reminderOptions.map { it.label })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
