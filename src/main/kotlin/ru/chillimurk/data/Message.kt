@file:OptIn(ExperimentalSerializationApi::class)

package ru.chillimurk.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Message(
    val id: Long,
    val senderId: Long,
    val receiverId: Long,
    var text: String,
    var wasRead: Boolean = false,
    @Serializable(with = InstantSerializer::class) val sentAt: Instant
)

var messageIdIncrement: Long = 1
    get() = field++
val messageStorage = mutableListOf<Message>()
val conversationStorage = mutableMapOf<Long, MutableSet<Long>>() // sender, receivers