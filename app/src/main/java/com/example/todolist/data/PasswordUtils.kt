package com.example.todolist.data

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordUtils {

    private const val ITERACIONES = 120000
    private const val LONGITUD_CLAVE = 256

    fun hash(contrasena: String): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val spec = PBEKeySpec(
            contrasena.toCharArray(),
            salt,
            ITERACIONES,
            LONGITUD_CLAVE
        )

        val clave = SecretKeyFactory
            .getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded

        spec.clearPassword()

        val saltTexto = Base64.encodeToString(salt, Base64.NO_WRAP)
        val claveTexto = Base64.encodeToString(clave, Base64.NO_WRAP)

        return "$saltTexto:$claveTexto"
    }

    fun verificar(contrasena: String, almacenada: String): Boolean {
        val partes = almacenada.split(":")

        if (partes.size != 2) return false

        val salt = Base64.decode(partes[0], Base64.NO_WRAP)
        val claveOriginal = Base64.decode(partes[1], Base64.NO_WRAP)

        val spec = PBEKeySpec(
            contrasena.toCharArray(),
            salt,
            ITERACIONES,
            LONGITUD_CLAVE
        )

        val claveNueva = SecretKeyFactory
            .getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded

        spec.clearPassword()

        return java.security.MessageDigest.isEqual(
            claveOriginal,
            claveNueva
        )
    }
}
