package com.android.smartcurtainapp.models

import Sensors

data class Room(
    val room_id: String = "",
    val name: String = "",
    val created_at: String = "",
    val sensors: Sensors = Sensors(),
    val curtains: Map<String, Curtain> = emptyMap()
)

