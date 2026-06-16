package com.garam.whenwheremeet.domain.usecase

import com.garam.whenwheremeet.domain.model.AreaType
import com.garam.whenwheremeet.domain.model.MeetingAreaCandidate
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.ParticipantTravelTime
import com.garam.whenwheremeet.domain.model.TransportMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AreaRecommendationCalculatorTest {
    private val calculator = AreaRecommendationCalculator()
    private val neutralCandidate = candidate("중립역", emptyList())

    @Test
    fun calculatesAverageAndMaximumTravelTime() {
        val result = calculator.calculate(neutralCandidate, times(20, 30, 40), MeetingType.OTHER)

        assertEquals(30, result.averageTravelMinutes)
        assertEquals(40, result.maxTravelMinutes)
        assertEquals(20, result.minTravelMinutes)
    }

    @Test
    fun calculatesPopulationVariance() {
        val result = calculator.calculate(neutralCandidate, times(20, 30, 40), MeetingType.OTHER)

        assertEquals(66.666, result.travelTimeVariance, absoluteTolerance = 0.01)
    }

    @Test
    fun fairnessScoreSortsBalancedCandidateFirst() {
        val balanced = calculator.calculate(candidate("균형역", emptyList()), times(30, 32, 34), MeetingType.OTHER)
        val slower = calculator.calculate(candidate("느린역", emptyList()), times(40, 42, 44), MeetingType.OTHER)

        assertTrue(listOf(slower, balanced).sortedByDescending { it.fairnessScore }.first() == balanced)
    }

    @Test
    fun penalizesCandidateWithOneExcessiveTravelTime() {
        val balanced = calculator.calculate(neutralCandidate, times(35, 35, 35), MeetingType.OTHER)
        val outlier = calculator.calculate(neutralCandidate, times(20, 20, 80), MeetingType.OTHER)

        assertTrue(outlier.fairnessScore < balanced.fairnessScore)
    }

    @Test
    fun matchingMeetingTypeTagAddsBonus() {
        val matching = calculator.calculate(candidate("식사역", listOf("식사")), times(30, 30), MeetingType.MEAL)
        val neutral = calculator.calculate(candidate("중립역", emptyList()), times(30, 30), MeetingType.MEAL)

        assertEquals(8.0, matching.fairnessScore - neutral.fairnessScore, absoluteTolerance = 0.001)
    }

    @Test
    fun maximumTravelTimeAffectsScore() {
        val lowerMax = calculator.calculate(neutralCandidate, times(30, 30, 30), MeetingType.OTHER)
        val higherMax = calculator.calculate(neutralCandidate, times(25, 25, 40), MeetingType.OTHER)

        assertTrue(lowerMax.fairnessScore > higherMax.fairnessScore)
    }

    private fun times(vararg values: Int) = values.mapIndexed { index, minutes ->
        ParticipantTravelTime("p$index", "참여자$index", TransportMode.PUBLIC_TRANSIT, minutes)
    }

    private fun candidate(name: String, tags: List<String>) = MeetingAreaCandidate(
        id = name,
        name = name,
        displayName = name,
        latitude = 37.5,
        longitude = 127.0,
        areaType = AreaType.STATION,
        tags = tags,
    )
}
