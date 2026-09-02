package com.ivor.ivormusic.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChunkedStreamDataSourceVkTest {
    @Test
    fun identifiesVkAudioCdnHosts() {
        assertTrue(ChunkedStreamDataSource.isVkAudioHost("cs1.vkuseraudio.net"))
        assertTrue(ChunkedStreamDataSource.isVkAudioHost("cs2.vkuseraudio.com"))
        assertTrue(ChunkedStreamDataSource.isVkAudioHost("cs3.vkuseraudio.ru"))
        assertTrue(ChunkedStreamDataSource.isVkAudioHost("sun1.userapi.com"))
        assertTrue(ChunkedStreamDataSource.isVkAudioHost("vk.ru"))
        assertFalse(ChunkedStreamDataSource.isVkAudioHost("example.com"))
        assertFalse(ChunkedStreamDataSource.isVkAudioHost("fakevkuseraudio.net.example.com"))
    }
}
