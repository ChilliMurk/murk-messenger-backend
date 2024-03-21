package ru.chillimurk.data

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(val id: Long, val email: String, val username: String, val fullName: String)
