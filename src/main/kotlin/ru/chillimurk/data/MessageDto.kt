package ru.chillimurk.data

import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(val receiverId: Long, val text: String)
