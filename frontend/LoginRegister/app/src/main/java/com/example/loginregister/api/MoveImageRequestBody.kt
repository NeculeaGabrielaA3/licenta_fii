package com.example.loginregister.api

import com.google.gson.annotations.SerializedName

data class MoveImageRequestBody(
    @SerializedName("image_id") val imageId: Int,
    @SerializedName("current_collection_id") val currentCollectionId: Int,
    @SerializedName("new_collection_id") val newCollectionId: Int
)
