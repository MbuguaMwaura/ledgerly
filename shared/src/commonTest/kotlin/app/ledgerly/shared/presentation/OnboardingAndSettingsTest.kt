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
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingAndSettingsTest : ViewModelTest() {
    @Test
    fun onboardingWalksTheStepsAndSaves() = test {
        val app = AppViewModel(settings)
        val vm = OnboardingViewModel(settings, clock)
        assertFalse(app.uiState.first { !it.isLoading }.isOnboarded)
        vm.uiState.test {
            assertEquals(OnboardingStep.Welcome, awaitItem().step)
            vm.onNext()
            val about = awaitState { it.step == OnboardingStep.AboutYou }
            assertFalse(about.canContinue)
            assertEquals(1, about.progress)
            assertEquals(3, about.progressCount)

            vm.onNameChange("Wanjiru Kamau")
            vm.onCurrencyChange(CurrencyCode.UGX)
            assertTrue(awaitState { it.currency == CurrencyCode.UGX && it.canContinue }.canContinue)
            vm.onNext()
            awaitState { it.step == OnboardingStep.Track }

            vm.onTrackBudgetsChange(false)
            assertEquals(2, awaitState { !it.trackBudgets }.progressCount)
            vm.onNext()
            awaitState { it.step == OnboardingStep.Done }
            vm.onBack()
            awaitState { it.step == OnboardingStep.Track }
            vm.onNext()
            awaitState { it.step == OnboardingStep.Done }

            vm.onFinish()
            awaitState { it.isFinished }
        }
        val saved = settings.observe().first()
        assertEquals("Wanjiru Kamau", saved.name)
        assertEquals(CurrencyCode.UGX, saved.currency)
        assertFalse(saved.trackBudgets)
        assertEquals(LocalDate(2026, 11, 25), saved.startedOn)

        val shell = app.uiState.first { it.isOnboarded }
        assertEquals("Wanjiru", shell.firstName)
        assertFalse(shell.trackBudgets)
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
            cancelAndIgnoreRemainingEvents()
        }
    }
}
