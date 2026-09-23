package com.gymcompanion.app.data.backup

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

fun deriveKey(password: String): SecretKey {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
    return SecretKeySpec(bytes.copyOf(16), "AES")
}

fun encrypt(data: ByteArray, password: String): ByteArray {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val key = deriveKey(password)
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val iv = cipher.iv
    val encrypted = cipher.doFinal(data)
    return iv + encrypted
}

fun decrypt(data: ByteArray, password: String): ByteArray {
    val iv = data.copyOfRange(0, 16)
    val encrypted = data.copyOfRange(16, data.size)
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val key = deriveKey(password)
    cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
    return cipher.doFinal(encrypted)
}
