package ru.chillimurk

import com.auth0.jwt.JWT
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import ru.chillimurk.data.*
import ru.chillimurk.plugins.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSessions()
    configureSecurity()
    configureSerialization()
    configureRouting()
    fillList()
}

suspend fun ApplicationCall.getUserSessionOrRespond(): UserSession? {
    val userSession = this.getUserSession()
    if (userSession == null) {
        respond(status = HttpStatusCode.Unauthorized, message = ResponseDto("No logged in user. Delete cookies and log in again."))
    }
    return userSession
}

suspend fun ApplicationCall.getUserSession(): UserSession? {
    var userSession = sessions.get<UserSession>()
    if (userSession != null) {
        return userSession
    }

    val cookieToken = request.cookies["access_token"]
    if (cookieToken != null) {
        val decodedJWT = JWT.decode(cookieToken)
        val userId = decodedJWT.getClaim("id").asLong()
        userStorage.find { it.id == userId }?.let {
            userSession = UserSession(it)
        } ?: return null
    }

    val authHeader = request.header("Authorization")
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        val token = authHeader.removePrefix("Bearer ")

        val decodedJWT = JWT.decode(token)
        val userId = decodedJWT.getClaim("id").asLong()
        userStorage.find { it.id == userId }?.let {
            userSession = UserSession(it)
        } ?: return null
    }

    return userSession
}
