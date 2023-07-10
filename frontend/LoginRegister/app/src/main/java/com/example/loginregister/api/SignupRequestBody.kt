package com.example.loginregister.api

data class SignupRequestBody (
    val email: String,
    val password: String,
    val first_name: String,
    val last_name: String,
    val username: String,
    val gender: String,
)