package com.example.loginregister.api

import com.example.loginregister.gallery.Image
import com.example.loginregister.util.AppConfig
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface MyApi {

    @Multipart
    @PUT("/users/profile-picture")
    fun addImageProfile(
        @Part image: MultipartBody.Part,
        @Header("Authorization") apiKey: String
    ): Call<RequestResponse>

    @Multipart
    @POST("/collections/{collectionId}/images/{imageId}")
    fun editImageInCollection(
        @Path("collectionId") collectionId: Int,
        @Path("imageId") imageId: Int,
        @Part image: MultipartBody.Part,
        @Header("Authorization") apiKey: String
    ): Call<RequestResponse>

    @DELETE("/collections/{collectionId}/images/{imageId}")
    fun deleteImage(
        @Path("collectionId") collectionId: Int,
        @Path("imageId") imageId: Int,
        @Header("Authorization") apiKey: String
    ): Call<Void>

    @DELETE("/collections/{collectionId}")
    fun deleteCollection(
        @Path("collectionId") collectionId: Int,
        @Header("Authorization") apiKey: String
    ): Call<Void>

    @Multipart
    @POST("/collections/{collectionId}/images")
    fun addImageToCollection(
        @Path("collectionId") collectionId: Int,
        @Part image: MultipartBody.Part,
        @Part("desc") desc: RequestBody,
        @Header("Authorization") apiKey: String
    ): Call<AddImageResponse>

    @GET("collections/{collection_id}/images")
    fun getImagesFromCollection(
        @Header("Authorization") token: String,
        @Path("collection_id") collectionId: Int
    ): Call<List<Image>>

    @POST("/users/collections")
    fun addCollection(
        @Body collection: CollectionRequestBody,
        @Header("Authorization") apiKey: String
    ): Call<CollectionResponse>

    @GET("/users/collections")
    fun getUserCollections(
        @Header("Authorization") apiKey: String): Call<List<CollectionResponse>>

    @POST("/move_image")
    fun moveImage(
        @Body moveImageRequestBody: MoveImageRequestBody,
        @Header("Authorization") apiKey: String
    ): Call<AddImageResponse>

    @POST("/signup")
    fun signup(
        @Body signupRequestBody: SignupRequestBody
    ): Call<RequestResponse>

    @POST("/login")
    fun login(
        @Body loginRequestBody: LoginRequestBody
    ): Call<RequestResponse>

    @GET("/logout")
    fun logout(@Header("Authorization") apiKey: String): Call<RequestResponse>

    @GET("/user")
    fun getUserInfo(
        @Header("Authorization") apiKey: String
    ): Call<UserResponse>

    @DELETE("/user")
    fun deleteUser(
        @Header("Authorization") apiKey: String
    ): Call<RequestResponse>


    companion object {
        operator fun invoke(): MyApi{
            return Retrofit.Builder()
                .baseUrl(AppConfig.NGROK_ADDRESS)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MyApi::class.java)
        }
    }
}