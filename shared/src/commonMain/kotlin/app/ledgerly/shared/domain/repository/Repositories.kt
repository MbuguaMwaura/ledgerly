package app.ledgerly.shared.domain.repository

import app.ledgerly.shared.domain.model.Budget
import app.ledgerly.shared.domain.model.BudgetMonth
import app.ledgerly.shared.domain.model.Category
import app.ledgerly.shared.domain.model.Debt
import app.ledgerly.shared.domain.model.DebtPayment
import app.ledgerly.shared.domain.model.Transaction
import app.ledgerly.shared.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/*
 * The rest of the app talks only to these interfaces. v1 backs them with the local database
 * (app.ledgerly.shared.data). A remote source can be added later behind the same interfaces,
 * for example a repository that writes locally first and syncs in the background, without
 * touching use cases, ViewModels or either UI. Records carry UUID ids and updated_at times
 * for that reason.
 */

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
}

interface TransactionRepository {
    /** Entries whose logical date is in [from]..[to], newest first. */
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Transaction>>

    suspend fun get(id: String): Transaction?

    suspend fun save(transaction: Transaction)

    suspend fun delete(id: String)
}

interface LogRepository {
    /** Every logical day with an entry or a "Nothing spent today". Drives the streak. */
    fun observeLoggedDays(): Flow<Set<LocalDate>>

    suspend fun markNothingSpent(date: LocalDate, at: Instant)
}

interface BudgetRepository {
    /**
     * The budgets in force in [month]. A budget set in an earlier month carries over until it is
     * changed, so each returned [Budget] has [month] as its month.
     */
    fun observeMonth(month: BudgetMonth): Flow<List<Budget>>

    /** Sets the limit from [Budget.month] onwards. A zero limit removes the budget from that month on. */
    suspend fun save(budget: Budget, at: Instant)

    suspend fun delete(categoryId: String, month: BudgetMonth)
}

interface DebtRepository {
    fun observeAll(): Flow<List<Debt>>

    /** Saves the debt itself; payments go through [addPayment]. */
    suspend fun save(debt: Debt, at: Instant)

    suspend fun addPayment(payment: DebtPayment)

    suspend fun markReminded(debtId: String, at: Instant)

    suspend fun delete(debtId: String)
}

interface SettingsRepository {
    fun observe(): Flow<UserSettings>

    /** Applies [change] to the current settings and saves the result. */
    suspend fun update(change: (UserSettings) -> UserSettings)
}
