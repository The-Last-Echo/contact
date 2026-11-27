package com.thelastecho.contact.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreDatabasePassphrase(context: Context) {
    private val encryptedPassphraseFile =
        File(context.noBackupFilesDir, "database-passphrase.enc")

    fun getOrCreate(): ByteArray {
        val wrappingKey = getOrCreateWrappingKey()
        if (encryptedPassphraseFile.exists()) {
            val contents = encryptedPassphraseFile.readBytes()
            require(contents.size > GCM_IV_LENGTH + GCM_TAG_LENGTH_BYTES) {
                "The encrypted database passphrase is incomplete."
            }
            val buffer = ByteBuffer.wrap(contents)
            val iv = ByteArray(GCM_IV_LENGTH).also(buffer::get)
            val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
            return Cipher.getInstance(TRANSFORMATION).run {
                init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
                doFinal(ciphertext)
            }
        }

        val passphrase = ByteArray(PASSPHRASE_LENGTH).also { java.security.SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, wrappingKey)
        }
        val ciphertext = cipher.doFinal(passphrase)
        val encryptedContents = ByteBuffer.allocate(cipher.iv.size + ciphertext.size)
            .put(cipher.iv)
            .put(ciphertext)
            .array()
        val temporaryFile = File(encryptedPassphraseFile.parentFile, "$PASSPHRASE_FILE_NAME.tmp")
        try {
            temporaryFile.writeBytes(encryptedContents)
            Files.move(
                temporaryFile.toPath(),
                encryptedPassphraseFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
            )
        } finally {
            Files.deleteIfExists(temporaryFile.toPath())
        }
        return passphrase
    }

    private fun getOrCreateWrappingKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey
        check(!encryptedPassphraseFile.exists()) {
            "The database passphrase exists, but its Android Keystore key is unavailable."
        }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "contact.database.passphrase"
        const val PASSPHRASE_FILE_NAME = "database-passphrase.enc"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_LENGTH_BITS = 128
        const val GCM_TAG_LENGTH_BYTES = GCM_TAG_LENGTH_BITS / 8
        const val PASSPHRASE_LENGTH = 32
    }
}
