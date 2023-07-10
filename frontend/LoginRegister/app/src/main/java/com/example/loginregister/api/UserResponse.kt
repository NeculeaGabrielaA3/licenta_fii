package com.example.loginregister.api

data class UserResponse(
    val id: Int,
    val email: String,
    val first_name: String,
    val last_name: String,
    val username: String,
    val profile_picture: String,
    val bio: String,
    val gender: String,
    val images_count: Int,
    val collections_count: Int
)
