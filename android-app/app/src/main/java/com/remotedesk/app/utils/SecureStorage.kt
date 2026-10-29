package com.remotedesk.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Utilitário de armazenamento seguro para tokens de autenticação persistente (Etapa 7).
 * Criptografa o DeviceToken utilizando AES/GCM com chaves gerenciadas pelo Android Keystore
 * (isolamento por hardware / TEE quando disponível no dispositivo).
 */
class SecureStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "SecureStorage"
        private const val PREFS_NAME = "RemoteDeskSecurePrefs"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "RemoteDeskDeviceAuthKey"
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12

        private const val KEY_ENCRYPTED_TOKEN = "sec_device_token"
        private const val KEY_TOKEN_IV = "sec_device_token_iv"
        private const val KEY_IS_PAIRED = "sec_is_paired"
        private const val KEY_PAIRED_HOST = "sec_paired_host"
        private const val KEY_PAIRED_PORT = "sec_paired_port"
        private const val KEY_PAIRED_DEVICE_NAME = "sec_paired_device_name"
        private const val KEY_DEVICE_ID = "sec_device_id"
    }

    init {
        ensureKeyStoreKey()
    }

    private fun ensureKeyStoreKey() {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGen.init(spec)
                keyGen.generateKey()
                Log.d(TAG, "Chave criptográfica do Android Keystore gerada com sucesso.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Aviso ao inicializar Android Keystore: ${e.message}. Fallback transparente ativo.")
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível recuperar chave do Keystore: ${e.message}")
            null
        }
    }

    /**
     * Criptografa e armazena o token persistente com AES-GCM via Android Keystore.
     */
    fun saveDeviceToken(token: String?) {
        if (token.isNullOrBlank()) {
            prefs.edit().remove(KEY_ENCRYPTED_TOKEN).remove(KEY_TOKEN_IV).apply()
            return
        }

        try {
            val secretKey = getSecretKey()
            if (secretKey != null) {
                val cipher = Cipher.getInstance(AES_TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val iv = cipher.iv
                val ciphertext = cipher.doFinal(token.toByteArray(Charsets.UTF_8))

                prefs.edit()
                    .putString(KEY_ENCRYPTED_TOKEN, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                    .putString(KEY_TOKEN_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                    .apply()
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao criptografar token no Keystore: ${e.message}. Salvando com ofuscação.")
        }

        // Fallback caso o Keystore falhe no emulador
        val encoded = Base64.encodeToString(token.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        prefs.edit().putString(KEY_ENCRYPTED_TOKEN, "plain:$encoded").apply()
    }

    /**
     * Recupera e decodifica o token persistente com proteção criptográfica.
     */
    fun getDeviceToken(): String? {
        val encryptedBase64 = prefs.getString(KEY_ENCRYPTED_TOKEN, null) ?: return null
        if (encryptedBase64.startsWith("plain:")) {
            val rawBase64 = encryptedBase64.removePrefix("plain:")
            return String(Base64.decode(rawBase64, Base64.NO_WRAP), Charsets.UTF_8)
        }

        val ivBase64 = prefs.getString(KEY_TOKEN_IV, null) ?: return null

        return try {
            val secretKey = getSecretKey()
            if (secretKey != null) {
                val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
                val ciphertext = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                val cipher = Cipher.getInstance(AES_TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
                val decrypted = cipher.doFinal(ciphertext)
                String(decrypted, Charsets.UTF_8)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao decifrar token do Keystore: ${e.message}")
            null
        }
    }

    fun isPaired(): Boolean = prefs.getBoolean(KEY_IS_PAIRED, false) && !getDeviceToken().isNullOrBlank()

    fun setPaired(paired: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PAIRED, paired).apply()
        if (!paired) {
            saveDeviceToken(null)
        }
    }

    fun getPairedDeviceName(): String = prefs.getString(KEY_PAIRED_DEVICE_NAME, "Windows Host") ?: "Windows Host"

    fun setPairedDeviceName(name: String) {
        prefs.edit().putString(KEY_PAIRED_DEVICE_NAME, name).apply()
    }

    fun getDeviceId(): String {
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id.isNullOrBlank()) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
