package com.android.smartcurtainapp.models

data class House(
    val house_id: String = "",
    val house_name: String = "",
    val rooms: Map<String, Room> = emptyMap()
)