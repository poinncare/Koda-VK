package com.ivor.ivormusic.data.vk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VkMusicRepositoryTest {
    @Test
    fun songIdRoundTripsWithoutAccessKey() {
        val id = VkMusicRepository.songId(-42, 123, null)
        assertEquals(Triple(-42L, 123L, null), VkMusicRepository.parseSongId(id))
    }

    @Test
    fun songIdRoundTripsWithAccessKey() {
        val id = VkMusicRepository.songId(42, 123, "secret_value")
        assertEquals(Triple(42L, 123L, "secret_value"), VkMusicRepository.parseSongId(id))
    }

    @Test
    fun foreignIdIsRejected() {
        assertNull(VkMusicRepository.parseSongId("youtube-id"))
    }
}
