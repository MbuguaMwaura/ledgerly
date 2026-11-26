package app.ledgerly.shared.presentation

import app.cash.turbine.test
import app.ledgerly.shared.domain.model.EntryKind
import app.ledgerly.shared.domain.usecase.LogEntry
import app.ledgerly.shared.money.CurrencyCode
import app.ledgerly.shared.money.Money
import app.ledgerly.shared.presentation.settings.ChangeCurrencyViewModel
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChangeCurrencyViewModelTest : ViewModelTest() {
    @Test
    fun switchesAtTheGivenRate() = test {
        LogEntry(transactions, clock)(EntryKind.Expense, kes(1_000), "food")
        val vm = ChangeCurrencyViewModel(settings, clock)
        vm.uiState.test {
            val start = awaitState { !it.isLoading }
            assertEquals("Kenyan shilling (KES)", start.fromText)
            assertFalse(start.options.any { it.code == CurrencyCode.KES })
            assertFalse(start.canSave)
            assertEquals("Choose a currency", start.saveLabel)

            vm.onCurrencySelect(CurrencyCode.USD)
            assertEquals("KES 1,000 becomes USD 1,000.", awaitState { it.selected == CurrencyCode.USD }.example)

            vm.onRateChange("0.0077")
            val rated = awaitState { it.rateText == "0.0077" }
            assertEquals("KES 1,000 becomes USD 7.70.", rated.example)
            assertEquals("Switch to USD", rated.saveLabel)
            assertTrue(rated.canSave)

            vm.onRateChange("0")
            assertFalse(awaitState { it.rateText == "0" }.canSave)
            vm.onRateChange("0.0077")
            awaitState { it.canSave }

            vm.onSave()
            assertEquals("Switched to USD.", awaitState { it.isDone }.doneMessage)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(CurrencyCode.USD, settings.observe().first().currency)
        val entry = transactions.observeBetween(LocalDate(2026, 11, 25), LocalDate(2026, 11, 25)).first().single()
        assertEquals(Money(770, CurrencyCode.USD), entry.amount)
    }
}
