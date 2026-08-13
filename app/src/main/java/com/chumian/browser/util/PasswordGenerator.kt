package com.chumian.browser.util

import java.security.SecureRandom

object PasswordGenerator {
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"
    private const val ALL_CHARS = LOWERCASE + UPPERCASE + DIGITS + SYMBOLS
    private val random = SecureRandom()

    fun generate(length: Int = 32): String {
        if (length < 8) return generate(8)
        val password = StringBuilder(length)

        password.append(LOWERCASE[random.nextInt(LOWERCASE.length)])
        password.append(UPPERCASE[random.nextInt(UPPERCASE.length)])
        password.append(DIGITS[random.nextInt(DIGITS.length)])
        password.append(SYMBOLS[random.nextInt(SYMBOLS.length)])

        for (i in 4 until length) {
            password.append(ALL_CHARS[random.nextInt(ALL_CHARS.length)])
        }

        return password.toString().toList().shuffled(random).joinToString("")
    }
}
