package com.ignatrozhko.governedworkflow.data.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class QrPayloadParserTest {

    @Test
    fun parse_shouldNormalizeUrls_andTrimToken() {
        val payload = QrPayloadParser.parse("""{"urls":["192.168.1.5:5111","http://100.64.0.2:5111/"],"token":" abc ","extra":1}""")

        assertEquals(QrPayload(null, listOf("http://192.168.1.5:5111", "http://100.64.0.2:5111"), "abc"), payload)
    }

    @Test
    fun parse_shouldKeepName_whenPresent() {
        assertEquals("Mac", QrPayloadParser.parse("""{"name":"Mac","urls":["h:1"],"token":"t"}""").name)
    }

    @Test
    fun parse_shouldReject_whenTokenMissing() {
        assertThrows(InvalidQrPayloadException::class.java) { QrPayloadParser.parse("""{"urls":["h:1"]}""") }
    }

    @Test
    fun parse_shouldReject_whenNoValidUrl() {
        assertThrows(InvalidQrPayloadException::class.java) { QrPayloadParser.parse("""{"urls":["::"],"token":"t"}""") }
    }

    @Test
    fun parse_shouldReject_whenNotJson() {
        assertThrows(InvalidQrPayloadException::class.java) { QrPayloadParser.parse("https://example.com") }
    }
}
