package app.ledgerly.shared.domain.model

import app.ledgerly.shared.money.CurrencyCode
import kotlinx.datetime.LocalDate

/** Light, dark, or whatever the phone uses. */
enum class ThemeChoice { System, Light, Dark }

/** The user's preferences. A fresh install has the defaults below until onboarding finishes. */
data class UserSettings(
    /** What Home greets the user by. Empty until they give one. */
    val name: String = "",
    /** The currency every amount is logged in. Chosen once, during onboarding. */
    val currency: CurrencyCode = CurrencyCode.KES,
    val theme: ThemeChoice = ThemeChoice.System,
    /** Off hides budget cards and the Budgets view. Nothing is deleted. */
    val trackBudgets: Boolean = true,
    /** Off hides the Debts tab and debt cards. Nothing is deleted. */
    val trackDebts: Boolean = true,
    /** One nudge a day, only on days nothing is logged yet. */
    val reminderOn: Boolean = false,
    /** When the nudge comes, in minutes after midnight. 20:00 by default. */
    val reminderMinutes: Int = 20 * 60,
    /** Ask for Face ID, fingerprint or the phone's passcode whenever Ledgerly opens. */
    val appLock: Boolean = false,
    /** The day onboarding finished, or null while it hasn't. */
    val startedOn: LocalDate? = null,
) {
    val isOnboarded: Boolean get() = startedOn != null
}
