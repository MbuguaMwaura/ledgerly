package app.ledgerly.shared.presentation

import app.cash.turbine.ReceiveTurbine
import app.ledgerly.shared.data.LocalBudgetRepository
import app.ledgerly.shared.data.LocalCategoryRepository
import app.ledgerly.shared.data.LocalDebtRepository
import app.ledgerly.shared.data.LocalLogRepository
import app.ledgerly.shared.data.LocalSettingsRepository
import app.ledgerly.shared.data.LocalTransactionRepository
import app.ledgerly.shared.db.LedgerlyDatabase
import app.ledgerly.shared.money.CurrencyCode
import app.ledgerly.shared.money.Money
import app.ledgerly.shared.testing.FakeClock
import app.ledgerly.shared.testing.createTestDriver
import app.ledgerly.shared.testing.nairobi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * A real in-memory database behind every ViewModel test, with `Dispatchers.Main` pointed at the
 * test dispatcher so `viewModelScope` runs under the test's virtual time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelTest {
    protected val dispatcher = UnconfinedTestDispatcher()
    protected val clock = FakeClock(nairobi("2026-11-25T18:00"))
    private val db = LedgerlyDatabase(createTestDriver())

    protected val categories = LocalCategoryRepository(db, dispatcher)
    protected val transactions = LocalTransactionRepository(db, dispatcher)
    protected val logs = LocalLogRepository(db, dispatcher)
    protected val budgets = LocalBudgetRepository(db, dispatcher)
    protected val debts = LocalDebtRepository(db, dispatcher)
    protected val settings = LocalSettingsRepository(db, dispatcher)

    @BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()

    protected fun kes(major: Long) = Money.ofMajor(major, CurrencyCode.KES)

    protected fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }
}

/** Skips states until one matches, so tests don't depend on how many intermediate emissions happen. */
suspend fun <T> ReceiveTurbine<T>.awaitState(matches: (T) -> Boolean): T {
    while (true) {
        val next = awaitItem()
        if (matches(next)) return next
    }
}
