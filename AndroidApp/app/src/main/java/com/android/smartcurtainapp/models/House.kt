package com.android.smartcurtainapp.models

data class House(
    val house_id: String = "",
    val house_name: String = "",
    val created_at: String = "",
    val rooms: Map<String, Room> = emptyMap()
)