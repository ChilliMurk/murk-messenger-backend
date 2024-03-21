package ru.chillimurk.data

import kotlinx.serialization.Serializable

@Serializable
data class ResponseDto<T>(val data: T)
