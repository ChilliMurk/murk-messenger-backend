package ru.chillimurk.plugins

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import ru.chillimurk.data.UserSession

fun Application.configureSessions() {
    install(Sessions) {
        val secretEncryptKey = hex("00112233445566778899aabbccddeeff")
        val secretSignKey = hex("6819b57a326945c1968f45236589")
        cookie<UserSession>("user_session") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 86400
            transform(SessionTransportTransformerEncrypt(secretEncryptKey, secretSignKey))
        }
    }
}
