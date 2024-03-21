package ru.chillimurk.data

import kotlinx.serialization.Serializable

@Serializable
data class LoginDto(val email: String, val password: String)
