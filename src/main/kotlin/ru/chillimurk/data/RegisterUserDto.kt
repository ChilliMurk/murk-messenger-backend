package ru.chillimurk.data

import kotlinx.serialization.Serializable

@Serializable
data class RegisterUserDto(val email: String, val password: String, val username: String, val fullName: String)
