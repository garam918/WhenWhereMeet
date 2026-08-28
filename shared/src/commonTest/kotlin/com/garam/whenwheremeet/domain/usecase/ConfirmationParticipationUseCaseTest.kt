package com.garam.whenwheremeet.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfirmationParticipationUseCaseTest {
    private val evaluate = EvaluateConfirmationParticipationUseCase()

    @Test
    fun sevenOfTenVotesMeetsSeventyPercentThreshold() {
        val participants = (1..10).map { "p$it" }

        val result = evaluate(participants, participants.take(7))

        assertEquals(7, result.requiredVoterCount)
        assertEquals(70, result.percentage)
        assertTrue(result.meetsThreshold)
    }

    @Test
    fun requiredVoteCountRoundsUpForSmallRooms() {
        val participants = listOf("p1", "p2", "p3")

        val twoVotes = evaluate(participants, participants.take(2))
        val threeVotes = evaluate(participants, participants)

        assertEquals(3, twoVotes.requiredVoterCount)
        assertFalse(twoVotes.meetsThreshold)
        assertTrue(threeVotes.meetsThreshold)
    }

    @Test
    fun duplicateAndUnknownVotersAreNotCounted() {
        val result = evaluate(
            participantIds = listOf("p1", "p2"),
            voterParticipantIds = listOf("p1", "p1", "outsider"),
        )

        assertEquals(1, result.voterCount)
        assertFalse(result.meetsThreshold)
    }
}
