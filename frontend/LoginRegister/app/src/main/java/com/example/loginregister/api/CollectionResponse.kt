package com.example.loginregister.api

import com.example.loginregister.gallery.Image

data class CollectionResponse (
    val id: Int,
    val name: String,
    val image_count: Int,
    val images: List<Image>
)