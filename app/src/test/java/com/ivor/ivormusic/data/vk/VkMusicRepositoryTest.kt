package com.ivor.ivormusic.data.vk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject

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

    @Test
    fun parsesVkAccountProfile() {
        val profile = VkMusicRepository.parseProfile(
            JSONObject()
                .put("id", 42)
                .put("first_name", "Иван")
                .put("last_name", "Иванов")
                .put("photo_200", "https://vk.example/avatar.jpg"),
        )

        assertEquals(42L, profile?.id)
        assertEquals("Иван Иванов", profile?.name)
        assertEquals("https://vk.example/avatar.jpg", profile?.avatarUrl)
    }

    @Test
    fun sessionChangeRevisionNotifiesExistingViewModels() {
        val before = VkSessionStore.sessionRevision.value
        VkSessionStore.notifySessionChanged()
        assertTrue(VkSessionStore.sessionRevision.value > before)
    }
}
