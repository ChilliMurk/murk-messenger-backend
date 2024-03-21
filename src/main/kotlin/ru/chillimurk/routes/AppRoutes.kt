package ru.chillimurk.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import ru.chillimurk.data.*
import ru.chillimurk.getUserSession
import ru.chillimurk.getUserSessionOrRespond
import java.time.Instant

const val PAGE_SIZE = 15

fun Route.userRouting() {

    val secret = environment!!.config.property("jwt.secret").getString()
    val issuer = environment!!.config.property("jwt.issuer").getString()
    val audience = environment!!.config.property("jwt.audience").getString()

    route("/validate") {
        get {
            val userSession = call.getUserSession()
            if (userSession != null) {
                call.respond(status = HttpStatusCode.OK, ResponseDto("User is logged in"))
            } else {
                call.respond(status = HttpStatusCode.Unauthorized, ResponseDto("User is not logged in"))
            }
        }
    }
    route("/user") {
        get {
            if (userStorage.isEmpty()) return@get call.respond(status = HttpStatusCode.NotFound, ResponseDto("No users found"))
            if (call.parameters.isEmpty()) {
                call.respond(ResponseDto(userStorage.map { UserDto(it.id, it.email, it.username, it.fullName) }))
            } else {
                val users = mutableListOf<UserDto>()
                call.parameters["fullName"]?.let { param ->
                    users.addAll(userStorage
                        .filter { it.fullName == param }
                        .map { UserDto(it.id, it.email, it.username, it.fullName) }
                    )
                }
                call.parameters["username"]?.let { param ->
                    users.addAll(userStorage
                        .filter { it.username == param }
                        .map { UserDto(it.id, it.email, it.username, it.fullName) }
                    )
                }
                call.respond(ResponseDto(users))
            }
        }
        get ("/page") {
            if (userStorage.isEmpty()) return@get call.respond(status = HttpStatusCode.NotFound, ResponseDto("No users found"))
            val page = call.parameters["page"]?.toIntOrNull() ?: 1
            val users = userStorage
                .drop((page - 1) * PAGE_SIZE)
                .take(PAGE_SIZE)
                .map { UserDto(it.id, it.email, it.username, it.fullName) }
            call.respond(ResponseDto(users))
        }
        get("/{id}") {
            val id = call.parameters["id"]?.toLong() ?: return@get call.respond(
                status = HttpStatusCode.BadRequest,
                ResponseDto("Missing id")
            )
            val user =
                userStorage.find { it.id == id } ?: return@get call.respond(
                    status = HttpStatusCode.NotFound,
                    ResponseDto("No user with id $id")
                )
            val userDto = UserDto(user.id, user.email, user.username, user.fullName)
            call.respond(ResponseDto(userDto))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toLong() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (userStorage.removeIf { it.id == id }) {
                call.respond(
                    status = HttpStatusCode.OK,
                    ResponseDto("User removed correctly")
                )
            } else {
                call.respond(
                    status = HttpStatusCode.NotFound,
                    ResponseDto("Not Found")
                )
            }
        }
        post ("/pfp") {
            val userSession = call.getUserSessionOrRespond() ?: return@post
            val user = userStorage.find { it.id == userSession.user.id }
                ?: return@post call.respond(status = HttpStatusCode.BadRequest, ResponseDto("Error: Incorrect login or password"))
            val multipart = call.receiveMultipart()
            multipart.forEachPart {
                if (it is PartData.FileItem) {
                    val bytes = it.streamProvider().readBytes()
                    pfpStorage[user.id] = bytes
                } else {
                    call.respond(status = HttpStatusCode.BadRequest, ResponseDto("Incorrect multipart file."))
                }
                it.dispose()
            }
            call.respond(ResponseDto("Profile picture updated successfully"))
        }
        get ("/pfp") {
            val userSession = call.getUserSessionOrRespond() ?: return@get
            pfpStorage[userSession.user.id]
                ?.let { call.respondBytes(it, ContentType.Image.JPEG) }
                ?: call.respond(HttpStatusCode.NotFound, ResponseDto("Profile picture not found"))
        }
        get ("/{id}/pfp") {
            val id = call.parameters["id"]?.toLong() ?: return@get call.respond(HttpStatusCode.BadRequest)
            pfpStorage[id]
                ?.let { call.respondBytes(it, ContentType.Image.JPEG) }
                ?: call.respond(HttpStatusCode.NotFound, ResponseDto("Profile picture not found"))
        }
    }

    route("/message") {
        get ("/conversations") {
            val userSession = call.getUserSessionOrRespond() ?: return@get
            val users = conversationStorage[userSession.user.id]
                ?.mapNotNull { id -> userStorage.find { it.id == id } }
                ?.map { UserDto(it.id, it.email, it.username, it.fullName) }
                ?: emptyList()
            call.respond(ResponseDto(users))
        }
        post {
            val userSession = call.getUserSessionOrRespond() ?: return@post
            val userSender = userStorage.find { it.id == userSession.user.id }
                ?: return@post call.respond(status = HttpStatusCode.BadRequest, ResponseDto("Error: Incorrect login or password"))
            val dto = call.receive<MessageDto>()
            val userReceiver = userStorage.find { it.id == dto.receiverId }
                ?: return@post call.respond(
                    status = HttpStatusCode.NotFound,
                    ResponseDto("No user with id ${dto.receiverId}")
                )
            val msg = Message(messageIdIncrement, userSender.id, dto.receiverId, dto.text, sentAt = Instant.now())
            messageStorage.add(msg)

            conversationStorage[userSender.id]?.add(userReceiver.id)
            conversationStorage[userReceiver.id]?.add(userSender.id)

            call.respond(
                status = HttpStatusCode.OK,
                ResponseDto(msg)
            )
        }
        get ("/unread") {
            val userSession = call.getUserSessionOrRespond() ?: return@get
            val user = userSession.user
            val unreadMessages = messageStorage
                .filter { it.receiverId == user.id && !it.wasRead }
                .sortedBy { it.sentAt }
                .onEach { it.wasRead = true }
            call.respond(ResponseDto(unreadMessages))
        }
        get ("/all") {
            val userSession = call.getUserSessionOrRespond() ?: return@get
            val user = userSession.user
            val allMessages = messageStorage
                .filter { it.receiverId == user.id || it.senderId == user.id }
                .sortedBy { it.sentAt }
            call.respond(ResponseDto(allMessages))
        }
        get ("/id/{id}") {
            val userSession = call.getUserSessionOrRespond() ?: return@get
            val id = call.parameters["id"]?.toLong() ?: return@get call.respond(
                status = HttpStatusCode.BadRequest,
                ResponseDto("Missing id")
            )
            val msg = messageStorage
                .find { it.id == id }
                ?.takeIf { it.senderId == userSession.user.id || it.receiverId == userSession.user.id }
                ?: return@get call.respond(status = HttpStatusCode.NotFound, ResponseDto("Error: Message not found"))
            call.respond(ResponseDto(msg))
        }
        get ("/user/{id}") {
            val userSession = call.getUserSessionOrRespond() ?: return@get
            val id = call.parameters["id"]?.toLong() ?: return@get call.respond(
                status = HttpStatusCode.BadRequest,
                ResponseDto("Missing id")
            )
            userStorage.find { it.id == id } ?: return@get call.respond(status = HttpStatusCode.NotFound, ResponseDto("Error: User not found"))
            val msgs = messageStorage
                .filter { it.senderId == userSession.user.id && it.receiverId == id
                        || it.receiverId == userSession.user.id && it.senderId == id }
                .sortedBy { it.sentAt }
            call.respond(ResponseDto(msgs))
        }
        put ("/id/{id}") {
            val userSession = call.getUserSessionOrRespond() ?: return@put
            val id = call.parameters["id"]?.toLong() ?: return@put call.respond(
                status = HttpStatusCode.BadRequest,
                ResponseDto("Missing id")
            )
            val dto = call.receive<MessagePutDto>()
            val msg = messageStorage
                .find { it.id == id }
                ?.apply { text = dto.text }
                ?.takeIf { it.senderId == userSession.user.id }
                ?: return@put call.respond(status = HttpStatusCode.NotFound, ResponseDto("Error: Message not found"))
            call.respond(ResponseDto(msg))
        }
        delete ("/id/{id}") {
            val userSession = call.getUserSessionOrRespond() ?: return@delete
            val id = call.parameters["id"]?.toLong() ?: return@delete call.respond(
                status = HttpStatusCode.BadRequest,
                ResponseDto("Missing id")
            )
            messageStorage
                .find { it.id == id }
                ?.takeIf { it.senderId == userSession.user.id }
                ?.let { messageStorage.remove(it) }
                ?: return@delete call.respond(status = HttpStatusCode.NotFound, ResponseDto("Error: Message not found"))
            call.respond(ResponseDto("Message deleted successfully"))
        }
    }

    route("/profile") {
        get {
            val userSession = call.getUserSessionOrRespond() ?: return@get
            val user = userSession.user
            val userDto = UserDto(user.id, user.email,  user.username, user.fullName)
            call.respond(ResponseDto(userDto))
        }
    }

    route("/register") {
        post {
            val userSession = call.getUserSession()
            if (userSession != null) {
                return@post call.respond(ResponseDto("Already logged in!"))
            }
            val dto = call.receive<RegisterUserDto>()
            val user = User(
                dto.email,
                dto.password,
                dto.username,
                dto.fullName
            )
            if (userStorage.map(User::email).contains(user.email)) {
                return@post call.respond(status = HttpStatusCode.BadRequest, ResponseDto("Error: Login already exists"))
            }
            if (userStorage.map(User::username).contains(user.username)) {
                return@post call.respond(status = HttpStatusCode.BadRequest, ResponseDto("Error: Username already exists"))
            }
            userStorage.add(user)
            conversationStorage[user.id] = mutableSetOf()
            call.respond(status = HttpStatusCode.Created, ResponseDto("User registered successfully"))
        }
    }

    route("/login") {
        post {
            val userSession = call.getUserSession()
            if (userSession != null) {
                return@post call.respond(ResponseDto("Already logged in!"))
            }
            val credentials = call.receive<LoginDto>()
            val user = userStorage.find { it.email == credentials.email && it.password == credentials.password }
                ?: return@post call.respond(status = HttpStatusCode.BadRequest, ResponseDto("Error: Incorrect login or password"))

            val token = JWT.create()
                .withAudience(audience)
                .withIssuer(issuer)
                .withClaim("id", user.id)
                .withClaim("email", user.email)
                .withClaim("username", user.username)
                .withClaim("fullName", user.fullName)
                .withExpiresAt(Instant.now().plusSeconds(86400))
                .sign(Algorithm.HMAC256(secret))

            call.sessions.set(UserSession(user))
            call.response.cookies.append(Cookie("access_token", token, path = "/", maxAge = 86400))
            call.respond(status = HttpStatusCode.OK,
                ResponseDto(hashMapOf("data" to "Logged in successfully", "token" to token)))
        }
    }

    route("/logout") {
        post {
            call.sessions.clear<UserSession>()
            call.response.cookies.append(Cookie("access_token", "", maxAge = 1))
            call.response.cookies.append(Cookie("user_session", "", maxAge = 1))
            call.respond(status = HttpStatusCode.OK, ResponseDto("Logged out successfully"))
        }
    }


}
