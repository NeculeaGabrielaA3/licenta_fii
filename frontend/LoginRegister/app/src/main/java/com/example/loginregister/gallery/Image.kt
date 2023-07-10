package com.example.loginregister.gallery

import com.google.gson.annotations.SerializedName

data class Image(
    @SerializedName("id") var id: Int,
    @SerializedName("s3_url") var url: String,
    @SerializedName("filename") var filename: String,
    @SerializedName("file_size") var size: Int,
    @SerializedName("uploaded_at") var uploadTime: String
)

