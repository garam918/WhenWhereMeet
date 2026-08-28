package com.garam.whenwheremeet.domain.usecase

import kotlin.math.ceil

data class ConfirmationParticipation(
    val voterCount: Int,
    val participantCount: Int,
    val requiredVoterCount: Int,
) {
    val meetsThreshold: Boolean = voterCount >= requiredVoterCount
    val percentage: Int = if (participantCount == 0) 0 else voterCount * 100 / participantCount
}

class EvaluateConfirmationParticipationUseCase(
    private val requiredRatio: Double = DEFAULT_REQUIRED_RATIO,
) {
    init {
        require(requiredRatio in 0.0..1.0) { "requiredRatio must be between 0 and 1" }
    }

    operator fun invoke(
        participantIds: Collection<String>,
        voterParticipantIds: Collection<String>,
    ): ConfirmationParticipation {
        val participants = participantIds.toSet()
        val voterCount = voterParticipantIds.toSet().count { it in participants }
        val requiredVoterCount = ceil(participants.size * requiredRatio).toInt()
        return ConfirmationParticipation(
            voterCount = voterCount,
            participantCount = participants.size,
            requiredVoterCount = requiredVoterCount,
        )
    }

    private companion object {
        const val DEFAULT_REQUIRED_RATIO = 0.7
    }
}
