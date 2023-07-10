package com.example.loginregister.gallery

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loginregister.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class GalleryViewModel : ViewModel() {

    interface ModifyImageCallback {
        fun onSuccess(newUrl: String)
        fun onFailure(message: String)
    }

    interface AddImageCallback {
        fun onSuccess(newImage: Image)
        fun onFailure(message: String)
    }

    private val _images = MutableLiveData<List<Image>>()
    val images: LiveData<List<Image>>
        get() = _images
    var previousCollectionId: Int = -1

    fun fetchImages(header: String, collectionId: Int) {
        viewModelScope.launch {
            try {
                val images = withContext(Dispatchers.IO) {
                    val response = MyApi().getImagesFromCollection(header, collectionId).execute()
                    if (response.isSuccessful) {
                        response.body()
                    } else {
                        null
                    }
                }
                _images.value = images ?: emptyList()
            } catch (e: Exception) {
                Log.d("GalleryViewModel", "Error: ${e.message}")
            }
        }
    }

    fun modifyImageInCollection(collectionId: Int, imageId: Int, header: String, uri: Uri, callback: ModifyImageCallback) {
        val file = uri.path?.let { File(it) }
        val requestFile = file?.let { UploadRequestBody(it, "image") }
        val body = requestFile?.let { MultipartBody.Part.createFormData("image", file.name, it) }

        MyApi().editImageInCollection(collectionId, imageId, body!!, header).enqueue(object :
            Callback<RequestResponse> {
            override fun onResponse(
                call: Call<RequestResponse>,
                response: Response<RequestResponse>
            ) {
                if (response.isSuccessful) {
                    callback.onSuccess(response.body()?.message.toString())
                } else {
                    callback.onFailure("Error: ${response.code()}")
                    Log.d("GalleryViewModel", "Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<RequestResponse>, t: Throwable) {
                Log.d("GalleryViewModel", "Error: ${t.message}")
            }
        })
    }

    fun saveImageToCollection(uri: Uri, header: String, collectionId: Int,  callback: AddImageCallback) {
        val file = uri.path?.let { File(it) }
        val requestFile = file?.let { UploadRequestBody(it, "image") }

        val body = requestFile?.let { MultipartBody.Part.createFormData("image", file.name, it) }

        val desc =
            "description for this image".toRequestBody("text/plain".toMediaTypeOrNull())
        if (body != null) {
            MyApi().addImageToCollection(collectionId, body, desc, header).enqueue(object :
                Callback<AddImageResponse> {
                override fun onResponse(
                    call: Call<AddImageResponse>,
                    response: Response<AddImageResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.image.let {
                            if (it != null) {
                                Log.d("GalleryViewModel", "Image added to collection! $it")
                                callback.onSuccess(it)
                                val newImage = response.body()?.image!!
                                val currentList = _images.value ?: emptyList()
                                val updatedList = currentList + newImage
                                _images.value = updatedList
                            }
                        }

                    } else {
                        callback.onFailure("Error: ${response.code()}")
                        Log.d("GalleryViewModel", "Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<AddImageResponse>, t: Throwable) {
                    Log.d("GalleryViewModel", "Error: ${t.message}")
                }
            })
        }
    }

    fun deleteImageFromCollection(collectionId: Int, imageId: Int, header: String) {
        viewModelScope.launch {
            try {
                MyApi().deleteImage(collectionId, imageId, header).enqueue(object :
                    Callback<Void> {
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        // Handle error
                        Log.d("GalleryViewModel", "Error: ${t.message}")
                    }

                    override fun onResponse(
                        call: Call<Void>,
                        response: Response<Void>
                    ) {
                        if (response.isSuccessful) {
                            // Image deleted successfully, refresh the gallery
                            _images.value = _images.value?.filter { it.id != imageId }
                            Log.d("GalleryViewModel", "Image deleted from collection!")
                        } else {
                            // Handle error
                            Log.d("GalleryViewModel", "Error: ${response.code()}")
                        }
                    }
                })
            } catch (e: Exception) {
                // Handle error
                Log.d("GalleryViewModel", "Error: ${e.message}")
            }
        }
    }

    fun moveImageToCollection(header: String, imageId: Int, oldCollectionId: Int, newCollectionId: Int,  callback: AddImageCallback) {
        val moveImageRequestBody = MoveImageRequestBody(imageId, oldCollectionId, newCollectionId)
        MyApi().moveImage(moveImageRequestBody, header).enqueue(object :
            Callback<AddImageResponse> {
            override fun onResponse(
                call: Call<AddImageResponse>,
                response: Response<AddImageResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.image.let { it2 ->
                        if (it2 != null) {
                            Log.d("GalleryViewModel", "Image moved to collection! $it2")
                            callback.onSuccess(it2)
                            _images.value = _images.value?.filter { it.id != imageId }
                        }
                    }
                } else {
                    Log.d("GalleryViewModel", "Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<AddImageResponse>, t: Throwable) {
                Log.d("GalleryViewModel", "Error: ${t.message}")
            }
        })

    }
}