package com.vish.myguru.features.vacabulary.model

object LeitnerEngine {
    private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L

    // Spaced intervals per box
    private val INTERVALS = mapOf(
        1 to ONE_DAY_MS * 1,   // Box 1 -> 1 day
        2 to ONE_DAY_MS * 3,   // Box 2 -> 3 days
        3 to ONE_DAY_MS * 7,   // Box 3 -> 7 days
        4 to ONE_DAY_MS * 14,  // Box 4 -> 14 days
        5 to ONE_DAY_MS * 30   // Box 5 -> 30 days
    )

    data class Evaluation(
        val nextBoxLevel: Int,
        val nextReviewEpoch: Long,
        val isMastered: Boolean
    )

    fun evaluate(currentBox: Int, isCorrect: Boolean, nowEpoch: Long): Evaluation {
        return if (isCorrect) {
            val nextBox = currentBox + 1
            if (nextBox > 5) {
                // Completed Box 5 -> Mastered
                Evaluation(nextBoxLevel = 5, nextReviewEpoch = 0L, isMastered = true)
            } else {
                val delay = INTERVALS[nextBox] ?: ONE_DAY_MS
                Evaluation(
                    nextBoxLevel = nextBox,
                    nextReviewEpoch = nowEpoch + delay,
                    isMastered = false
                )
            }
        } else {
            // Failed recall -> Reset to Box 1, due today
            Evaluation(
                nextBoxLevel = 1,
                nextReviewEpoch = nowEpoch,
                isMastered = false
            )
        }
    }
}