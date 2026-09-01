package com.ivor.ivormusic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRepositoryTest {
    private val repository = UpdateRepository()

    @Test
    fun comparesReleaseVersionsNumerically() {
        assertTrue(repository.isNewerVersion("1.10.0", "1.9.9"))
        assertTrue(repository.isNewerVersion("v2.0", "1.99.99"))
        assertFalse(repository.isNewerVersion("1.0.1", "1.0.1"))
        assertFalse(repository.isNewerVersion("1.0", "1.0.1"))
    }

    @Test
    fun selectsExactDeviceAbiBeforeUniversal() {
        val universal = ApkAsset("Koda-universal.apk", "https://github.com/universal", 1)
        val arm64 = ApkAsset("Koda-arm64-v8a.apk", "https://github.com/arm64", 1)
        val arm32 = ApkAsset("Koda-armeabi-v7a.apk", "https://github.com/arm32", 1)
        val assets = listOf(universal, arm32, arm64)

        assertEquals(arm64, UpdateRepository.findBestApk(assets, "arm64-v8a"))
        assertEquals(arm32, UpdateRepository.findBestApk(assets, "armeabi-v7a"))
    }
}
