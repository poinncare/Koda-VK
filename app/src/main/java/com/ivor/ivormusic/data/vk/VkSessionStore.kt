package com.ivor.ivormusic.data.vk

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class VkSessionStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "vk_music_session",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun read(): VkSession? {
        val token = preferences.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return null
        val cookieP = preferences.getString(KEY_COOKIE_P, null)?.takeIf { it.isNotBlank() } ?: return null
        val remixSid = preferences.getString(KEY_REMIX_SID, null)?.takeIf { it.isNotBlank() } ?: return null
        val storedExpiry = preferences.getLong(KEY_EXPIRES, 0L)
        return VkSession(
            accessToken = token,
            // Older builds stored VK's expiresIn duration as an epoch.
            expiresAtSeconds = storedExpiry.takeIf { it >= MIN_VALID_EPOCH_SECONDS } ?: 0L,
            cookieP = cookieP,
            remixSid = remixSid,
        )
    }

    fun write(session: VkSession) {
        val saved = preferences.edit()
            .putString(KEY_TOKEN, session.accessToken)
            .putLong(KEY_EXPIRES, session.expiresAtSeconds)
            .putString(KEY_COOKIE_P, session.cookieP)
            .putString(KEY_REMIX_SID, session.remixSid)
            .commit()
        check(saved) { "VK session could not be saved" }
        notifySessionChanged()
    }

    fun clear() {
        val cleared = preferences.edit().clear().commit()
        check(cleared) { "VK session could not be cleared" }
        notifySessionChanged()
    }

    companion object {
        private val _sessionRevision = MutableStateFlow(0L)
        val sessionRevision = _sessionRevision.asStateFlow()

        internal fun notifySessionChanged() {
            _sessionRevision.value += 1L
        }

        private const val KEY_TOKEN = "access_token"
        private const val KEY_EXPIRES = "expires_at_seconds"
        private const val KEY_COOKIE_P = "cookie_p"
        private const val KEY_REMIX_SID = "remix_sid"
        private const val MIN_VALID_EPOCH_SECONDS = 1_577_836_800L
    }
}
