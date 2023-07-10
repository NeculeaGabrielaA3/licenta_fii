package com.example.loginregister.api

import com.example.loginregister.gallery.Image

data class AddImageResponse (
    val image: Image,
    val message: String
)