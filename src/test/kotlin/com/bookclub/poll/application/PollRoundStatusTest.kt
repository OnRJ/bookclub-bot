package com.bookclub.poll.application

import com.bookclub.poll.domain.PollRoundStatus

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PollRoundStatusTest {
    @Test
    fun `parses persisted status`() {
        assertEquals(PollRoundStatus.ACTIVE, PollRoundStatus.fromPersisted("ACTIVE"))
        assertEquals(PollRoundStatus.ADMIN_DECISION, PollRoundStatus.fromPersisted("ADMIN_DECISION"))
    }

    @Test
    fun `rejects unknown persisted status`() {
        assertThrows(IllegalArgumentException::class.java) {
            PollRoundStatus.fromPersisted("BROKEN")
        }
    }
}
