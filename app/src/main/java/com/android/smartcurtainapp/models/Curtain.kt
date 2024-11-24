package com.android.smartcurtainapp.models

data class Curtain(
    val id: String = "",
    val name: String = "",
    val status: Boolean = false,
    val control: Control = Control()
)
