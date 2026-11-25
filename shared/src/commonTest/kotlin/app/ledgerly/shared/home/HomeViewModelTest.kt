package app.ledgerly.shared.home

import app.cash.turbine.test
import app.ledgerly.shared.money.CurrencyCode
import app.ledgerly.shared.money.Money
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeViewModelTest {
    @Test
    fun startsEmpty() = runTest {
        HomeViewModel().uiState.test {
            assertEquals(HomeUiState(Money.zero(CurrencyCode.KES), streakDays = 0), awaitItem())
        }
    }
}
