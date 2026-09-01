package com.ivor.ivormusic.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChunkedStreamDataSourceVkTest {
    @Test
    fun identifiesVkAudioCdnHosts() {
        assertTrue(ChunkedStreamDataSource.isVkAudioHost("cs1.vkuseraudio.net"))
        assertTrue(ChunkedStreamDataSource.isVkAudioHost("vk.ru"))
        assertFalse(ChunkedStreamDataSource.isVkAudioHost("example.com"))
    }
}
