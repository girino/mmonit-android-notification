package org.girino.mmonit.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MMonitStatusParserTest {
    @Test
    fun `critical failure has priority over every other status`() {
        val status = MMonitStatusParser.parse(
            """
            {
              "status": [
                {"label": "ok", "data": 12},
                {"label": "some services failed/unmonitored", "data": 2},
                {"label": "failed", "data": 1}
              ]
            }
            """.trimIndent(),
            checkedAt = 123L,
        )

        assertEquals(MMonitLevel.RED, status.level)
        assertEquals(123L, status.checkedAt)
    }

    @Test
    fun `service failure and warning are distinguished`() {
        val failed = MMonitStatusParser.parse(
            """{"status":[{"label":"services failed","data":"2"}]}""",
        )
        val warning = MMonitStatusParser.parse(
            """{"status":[{"label":"some services failed/unmonitored","data":1}]}""",
        )

        assertEquals(MMonitLevel.YELLOW, failed.level)
        assertEquals(MMonitLevel.ORANGE, warning.level)
    }

    @Test
    fun `inactive and ignored hosts are gray`() {
        val status = MMonitStatusParser.parse(
            """{"status":[{"label":"inactive","data":1},{"label":"ignored","data":2}]}""",
        )

        assertEquals(MMonitLevel.GRAY, status.level)
    }

    @Test
    fun `only healthy hosts are green`() {
        val status = MMonitStatusParser.parse(
            """{"status":[{"label":"ok","data":13}]}""",
        )

        assertEquals(MMonitLevel.GREEN, status.level)
        assertEquals("13 host(s) estão OK", status.detail)
    }

    @Test
    fun `payload without known statuses is rejected`() {
        assertThrows(InvalidMMonitPayloadException::class.java) {
            MMonitStatusParser.parse("{\"status\":[{\"label\":\"new state\",\"data\":1}]}")
        }
    }
}
