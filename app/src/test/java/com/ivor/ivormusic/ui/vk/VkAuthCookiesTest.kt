package com.ivor.ivormusic.ui.vk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VkAuthCookiesTest {
    @Test
    fun readsCookiesAcrossVkDomains() {
        assertEquals(
            VkAuthSession("login-token", "session-token"),
            parseVkAuthCookies(
                listOf(
                    "remixlang=0; p=login-token",
                    "remixsid=session-token; remixstid=42",
                ),
            ),
        )
    }

    @Test
    fun ignoresIncompleteSessions() {
        assertNull(parseVkAuthCookies(listOf("p=login-token; remixlang=0")))
        assertNull(parseVkAuthCookies(listOf("remixsid=session-token")))
    }

    @Test
    fun keepsEqualsCharactersInsideCookieValues() {
        assertEquals(
            VkAuthSession("a=b=c", "session-token"),
            parseVkAuthCookies(listOf("p=a=b=c; remixsid=session-token")),
        )
    }
}
