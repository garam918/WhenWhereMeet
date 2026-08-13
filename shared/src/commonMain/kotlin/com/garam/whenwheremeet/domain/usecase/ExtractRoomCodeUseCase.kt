package com.garam.whenwheremeet.domain.usecase

class ExtractRoomCodeUseCase {
    operator fun invoke(input: String): String? {
        val normalized = input.trim().uppercase()
        if (normalized.matches(RoomCodeRegex)) return normalized

        QueryCodeRegex.find(normalized)?.groupValues?.getOrNull(1)?.let { return it }
        JoinPathRegex.find(normalized)?.groupValues?.getOrNull(1)?.let { return it }
        RoomPathRegex.find(normalized)?.groupValues?.getOrNull(1)?.let { return it }
        return StandaloneCodeRegex.findAll(normalized)
            .map { it.value }
            .lastOrNull()
    }

    private companion object {
        val RoomCodeRegex = Regex("^[A-Z0-9]{6}$")
        val QueryCodeRegex = Regex("(?:ROOM|CODE)=([A-Z0-9]{6})(?:[^A-Z0-9]|$)")
        val JoinPathRegex = Regex("/JOIN/([A-Z0-9]{6})(?:[^A-Z0-9]|$)")
        val RoomPathRegex = Regex("/ROOM/([A-Z0-9]{6})(?:[^A-Z0-9]|$)")
        val StandaloneCodeRegex = Regex("(?<![A-Z0-9])[A-Z0-9]{6}(?![A-Z0-9])")
    }
}
