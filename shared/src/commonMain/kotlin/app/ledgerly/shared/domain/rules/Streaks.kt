package app.ledgerly.shared.domain.rules

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Streak milestones that get a celebration. Nothing moves on ordinary days. */
val STREAK_MILESTONES = listOf(3, 7, 14, 30, 60, 100, 365)

const val FREEZE_EARNED_EVERY_DAYS = 7
const val MAX_FREEZES = 2

data class StreakState(
    /** Consecutive logged days, ending today or yesterday. Days saved by a freeze keep it alive but don't add to it. */
    val current: Int,
    val longest: Int,
    val loggedToday: Boolean,
    val freezesHeld: Int,
    /** Days a freeze covered, oldest first, so the UI can say "A streak freeze saved it". */
    val frozenDays: List<LocalDate>,
    /** The milestone reached today, if today's log completed one. */
    val milestoneToday: Int?,
)

/**
 * Works out the streak from the days the user logged anything: an expense, an income or
 * "Nothing spent today". [loggedDays] are logical dates (after the 03:00 cutoff).
 *
 * - One freeze is earned for every 7 logged days in a row, up to [MAX_FREEZES].
 * - A missed day spends a freeze automatically if one is held; otherwise the streak resets.
 * - Today is never counted as missed: the user still has until the cutoff to log.
 */
fun calculateStreak(loggedDays: Set<LocalDate>, today: LocalDate): StreakState {
    val first = loggedDays.filter { it <= today }.minOrNull()
        ?: return StreakState(0, 0, loggedToday = false, freezesHeld = 0, frozenDays = emptyList(), milestoneToday = null)

    var current = 0
    var longest = 0
    var runTowardFreeze = 0
    var freezes = 0
    var frozenDays = mutableListOf<LocalDate>()
    val loggedToday = today in loggedDays
    val lastDayToJudge = if (loggedToday) today else today.minus(1, DateTimeUnit.DAY)

    var day = first
    while (day <= lastDayToJudge) {
        if (day in loggedDays) {
            current++
            runTowardFreeze++
            if (runTowardFreeze == FREEZE_EARNED_EVERY_DAYS) {
                freezes = minOf(MAX_FREEZES, freezes + 1)
                runTowardFreeze = 0
            }
        } else if (current > 0 && freezes > 0) {
            freezes--
            frozenDays.add(day)
        } else {
            current = 0
            runTowardFreeze = 0
            frozenDays = mutableListOf()
        }
        longest = maxOf(longest, current)
        day = day.plus(1, DateTimeUnit.DAY)
    }

    return StreakState(
        current = current,
        longest = longest,
        loggedToday = loggedToday,
        freezesHeld = freezes,
        frozenDays = frozenDays,
        milestoneToday = current.takeIf { loggedToday && it in STREAK_MILESTONES },
    )
}
