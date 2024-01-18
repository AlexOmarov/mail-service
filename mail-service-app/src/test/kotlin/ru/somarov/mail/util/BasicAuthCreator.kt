package ru.somarov.mail.util

import java.util.Base64

object BasicAuthCreator {
    fun createBasicAuthString(user: String, password: String): String {
        val auth = "$user:$password"
        val encodedAuth = Base64.getEncoder().encode(auth.encodeToByteArray())
        return "Basic " + String(encodedAuth)
    }
}
