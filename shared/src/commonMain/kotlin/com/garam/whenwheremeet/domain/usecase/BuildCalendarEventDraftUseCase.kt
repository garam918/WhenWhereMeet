package com.garam.whenwheremeet.domain.usecase

import com.garam.whenwheremeet.domain.model.CalendarEventDraft
import com.garam.whenwheremeet.domain.model.MeetingRoom

class BuildCalendarEventDraftUseCase {
    operator fun invoke(room: MeetingRoom, joinLink: String? = null): CalendarEventDraft? {
        val date = room.confirmedDate ?: return null
        val place = room.confirmedPlace
        val address = place?.roadAddress ?: place?.address
        val location = listOfNotNull(place?.name, address)
            .distinct()
            .joinToString(" · ")
            .ifBlank { null }
        val notes = listOfNotNull(
            room.description?.takeIf { it.isNotBlank() },
            "언제어디에서 확정한 약속입니다.",
            joinLink?.takeIf { it.isNotBlank() }?.let { "약속 확인: $it" },
        ).joinToString("\n")

        return CalendarEventDraft(
            title = room.title,
            date = date,
            location = location,
            notes = notes,
        )
    }
}
