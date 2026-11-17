package com.ignatrozhko.governedworkflow.data.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerUrlsTest {

    @Test
    fun normalize_shouldAddSchemeAndStripPath() {
        assertEquals("http://192.168.1.5:5111", ServerUrls.normalize(" 192.168.1.5:5111/ws/x?token=y#z "))
    }

    @Test
    fun normalize_shouldKeepHttps_andDefaultPort() {
        assertEquals("https://panel.example", ServerUrls.normalize("https://panel.example/"))
    }

    @Test
    fun normalize_shouldReturnNull_whenNotAUrl() {
        assertNull(ServerUrls.normalize("not a url"))
        assertNull(ServerUrls.normalize("   "))
    }

    @Test
    fun parseList_shouldSplitOnLinesAndCommas_andDropDuplicates() {
        val urls = ServerUrls.parseList("192.168.1.5:5111\nhttp://100.64.0.2:5111, 192.168.1.5:5111/")

        assertEquals(listOf("http://192.168.1.5:5111", "http://100.64.0.2:5111"), urls)
    }

    @Test
    fun displayHost_shouldShowHostAndPort() {
        assertEquals("10.0.2.2:5111", ServerUrls.displayHost("http://10.0.2.2:5111"))
    }
}
