package ru.chillimurk.data

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val email: String,
    val password: String,
    val username: String,
    val fullName: String
) {
    constructor(email: String, password: String, username: String, fullName: String)
            : this(userIdIncrement, email, password, username, fullName)
    init {
        if (!conversationStorage.containsKey(id)) {
            conversationStorage[id] = mutableSetOf()
        }
    }
}

var userIdIncrement: Long = 1
    get() = field++

val userStorage = mutableListOf<User>()
val pfpStorage = mutableMapOf<Long, ByteArray?>() // <id, pfp>

fun fillList() {
    userStorage.addAll(listOf(
        User(userIdIncrement, "email1@mail.com", "password1", "jonh", "John Johnson"),
        User(userIdIncrement, "email2@mail.com", "password2", "blake", "Doe Blake"),
        User(userIdIncrement, "email3@mail.com", "password3", "smith", "Smith Smith"),
        User(userIdIncrement, "email4@mail.com", "password4", "warden", "Smith Warden"),
        User(userIdIncrement, "email5@mail.com", "password5", "murk", "Ellina Murk"),
        User(userIdIncrement, "email6@mail.com", "password6", "mike", "Mike Mike"),
        User(userIdIncrement, "email7@mail.com", "password7", "boom", "Boom Boom"),
        User(userIdIncrement, "email8@mail.com", "password8", "bom", "Bom Bom"),
        User(userIdIncrement, "email9@mail.com", "password9", "bim", "Bim Bim"),
        User(userIdIncrement, "email10@mail.com", "password10", "destroyer228", "Destroyer Destroyer"),
        User(userIdIncrement, "email11@mail.com", "password11", "randomdude", "Random Dude"),
        User(userIdIncrement, "email12@mail.com", "password12", "nickname", "Nick Name"),
        User(userIdIncrement, "email13@mail.com", "password13", "blabla", "Bla Bla"),
        User(userIdIncrement, "email14@mail.com", "password14", "yadayada", "Ramzis Ramzis"),
        User(userIdIncrement, "email15@mail.com", "password15", "phony", "Phone Phony"),
        User(userIdIncrement, "email16@mail.com", "password16", "orangejuice", "Brandon White"),
        User(userIdIncrement, "email17@mail.com", "password17", "catlover", "Cat Lover")
    ))
}
