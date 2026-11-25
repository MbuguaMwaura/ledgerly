package app.ledgerly.shared.domain.usecase

import app.ledgerly.shared.domain.model.Debt
import app.ledgerly.shared.domain.model.DebtDirection
import app.ledgerly.shared.domain.model.DebtPayment
import app.ledgerly.shared.domain.model.newId
import app.ledgerly.shared.domain.repository.DebtRepository
import app.ledgerly.shared.domain.time.AppClock
import app.ledgerly.shared.money.Money
import kotlinx.datetime.LocalDate

class AddDebt(
    private val debts: DebtRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(
        direction: DebtDirection,
        counterparty: String,
        principal: Money,
        dueDate: LocalDate?,
        note: String? = null,
    ): Debt {
        require(counterparty.isNotBlank()) { "Who is it with?" }
        require(principal.minorUnits > 0) { "Amount must be positive" }
        val now = clock.now()
        val debt = Debt(
            id = newId(),
            direction = direction,
            counterparty = counterparty.trim(),
            principal = principal,
            dueDate = dueDate,
            note = note?.trim()?.ifEmpty { null },
            createdAt = now,
            lastRemindedAt = null,
            payments = emptyList(),
        )
        debts.save(debt, now)
        return debt
    }
}

class RecordDebtPayment(
    private val debts: DebtRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(debtId: String, amount: Money) {
        require(amount.minorUnits > 0) { "Amount must be positive" }
        debts.addPayment(DebtPayment(newId(), debtId, amount, clock.now()))
    }
}

/** Called after the user sends a reminder from the share sheet. */
class MarkReminded(
    private val debts: DebtRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(debtId: String) = debts.markReminded(debtId, clock.now())
}
