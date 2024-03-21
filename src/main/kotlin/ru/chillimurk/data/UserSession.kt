package ru.chillimurk.data

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(val user: User)
