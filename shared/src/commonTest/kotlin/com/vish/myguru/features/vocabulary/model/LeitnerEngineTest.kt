package com.vish.myguru.features.vocabulary.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LeitnerEngineTest {

    private val now = 1_700_000_000_000L
    private val oneDay = 24 * 60 * 60 * 1000L

    @Test
    fun correctAnswerInBoxOneMovesToBoxTwoDueInThreeDays() {
        val result = LeitnerEngine.evaluate(currentBox = 1, isCorrect = true, nowEpoch = now)

        assertEquals(2, result.nextBoxLevel)
        assertEquals(now + 3 * oneDay, result.nextReviewEpoch)
        assertFalse(result.isMastered)
    }

    @Test
    fun correctAnswerInBoxTwoMovesToBoxThreeDueInSevenDays() {
        val result = LeitnerEngine.evaluate(currentBox = 2, isCorrect = true, nowEpoch = now)

        assertEquals(3, result.nextBoxLevel)
        assertEquals(now + 7 * oneDay, result.nextReviewEpoch)
        assertFalse(result.isMastered)
    }

    @Test
    fun correctAnswerInBoxThreeMovesToBoxFourDueInFourteenDays() {
        val result = LeitnerEngine.evaluate(currentBox = 3, isCorrect = true, nowEpoch = now)

        assertEquals(4, result.nextBoxLevel)
        assertEquals(now + 14 * oneDay, result.nextReviewEpoch)
        assertFalse(result.isMastered)
    }

    @Test
    fun correctAnswerInBoxFourMovesToBoxFiveDueInThirtyDays() {
        val result = LeitnerEngine.evaluate(currentBox = 4, isCorrect = true, nowEpoch = now)

        assertEquals(5, result.nextBoxLevel)
        assertEquals(now + 30 * oneDay, result.nextReviewEpoch)
        assertFalse(result.isMastered)
    }

    @Test
    fun correctAnswerInBoxFiveMastersTheWord() {
        val result = LeitnerEngine.evaluate(currentBox = 5, isCorrect = true, nowEpoch = now)

        assertTrue(result.isMastered)
        assertEquals(5, result.nextBoxLevel)
        assertEquals(0L, result.nextReviewEpoch)
    }

    @Test
    fun wrongAnswerResetsEveryBoxToBoxOneDueImmediately() {
        for (box in 1..5) {
            val result = LeitnerEngine.evaluate(currentBox = box, isCorrect = false, nowEpoch = now)

            assertEquals(1, result.nextBoxLevel, "box $box should reset to 1")
            assertEquals(now, result.nextReviewEpoch, "box $box should be due immediately")
            assertFalse(result.isMastered, "box $box should not be mastered after a miss")
        }
    }

    @Test
    fun nextReviewIsScheduledRelativeToTheGivenTime() {
        val later = now + 12_345L

        val first = LeitnerEngine.evaluate(currentBox = 2, isCorrect = true, nowEpoch = now)
        val second = LeitnerEngine.evaluate(currentBox = 2, isCorrect = true, nowEpoch = later)

        assertEquals(12_345L, second.nextReviewEpoch - first.nextReviewEpoch)
    }

    @Test
    fun fiveCorrectAnswersInARowTakeANewWordToMastered() {
        var box = 1
        var mastered = false
        var answers = 0

        while (!mastered) {
            val result = LeitnerEngine.evaluate(currentBox = box, isCorrect = true, nowEpoch = now)
            box = result.nextBoxLevel
            mastered = result.isMastered
            answers++
            check(answers <= 10) { "word never reached mastered" }
        }

        assertEquals(5, answers)
    }
}
