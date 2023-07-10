package com.example.loginregister.gallery

data class ImageCollection(
    var id: Int,
    var name: String,
    var image_count: Int,
    var images: MutableList<Image>
)

