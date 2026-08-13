package com.garam.whenwheremeet.domain.usecase

import com.garam.whenwheremeet.buildRoomJoinLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExtractRoomCodeUseCaseTest {
    private val extract = ExtractRoomCodeUseCase()

    @Test
    fun extractsRawCode() {
        assertEquals("A7B8C9", extract(" a7b8c9 "))
    }

    @Test
    fun extractsCodeFromWebInviteLink() {
        assertEquals("A7B8C9", extract("https://whenwheremeet.web.app/join/A7B8C9"))
    }

    @Test
    fun buildsOneCanonicalInviteLinkForEveryPlatform() {
        assertEquals(
            "https://whenwheremeet.web.app/join/A7B8C9",
            buildRoomJoinLink("a7b8c9"),
        )
    }

    @Test
    fun extractsCodeFromAppInviteLink() {
        assertEquals("A7B8C9", extract("whenwheremeet://room/A7B8C9"))
    }

    @Test
    fun returnsNullWhenClipboardHasNoRoomCode() {
        assertNull(extract("초대 링크가 아직 없어요"))
    }
}
