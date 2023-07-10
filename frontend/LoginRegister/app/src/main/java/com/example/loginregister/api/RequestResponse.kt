package com.example.loginregister.api

data class RequestResponse(
    val message: String,
    val user_id: Int,
    val access_token: String,
    val refresh_token: String
)
