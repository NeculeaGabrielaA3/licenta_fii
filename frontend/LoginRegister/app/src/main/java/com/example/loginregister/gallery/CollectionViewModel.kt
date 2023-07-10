package com.example.loginregister.gallery

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loginregister.api.CollectionRequestBody
import com.example.loginregister.api.MyApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CollectionViewModel : ViewModel() {
    private val _collections = MutableLiveData<List<ImageCollection>>()
    val collections: LiveData<List<ImageCollection>>
        get() = _collections

    val isCollectionDeleted: SingleEventLiveData<Boolean> = SingleEventLiveData()
    var collectionId: Int = -1

    val isImageEditedInCollection: SingleEventLiveData<Boolean> = SingleEventLiveData()
    var imageUrl: String = ""
    var imagePosition: Int = -1

    var originalCollections: MutableList<ImageCollection> = mutableListOf()

    fun deleteImageFromCollection(imageId: Int, collectionId: Int) {

        val collection = _collections.value?.firstOrNull { it.id == collectionId }
        collection?.images?.removeIf { it.id == imageId }

        _collections.value = _collections.value
        originalCollections = (_collections.value as MutableList<ImageCollection>?)!!
    }

    fun deleteCollection(header: String, collectionId: Int) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    MyApi().deleteCollection(collectionId, header).execute()
                }
                if (response.isSuccessful) {
                    _collections.value = _collections.value?.filter { it.id != collectionId }
                    originalCollections = (_collections.value as MutableList<ImageCollection>?)!!
                    Log.d("CollectionViewModel", "Collection deleted successfully!")
                } else {
                    Log.d("CollectionViewModel", "Failed to delete collection: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.d("CollectionViewModel", "Failed to delete collection: ${e.message}")
            }
        }
    }

    fun addCollection(userId: Int, name: String, header: String) {
        val collection = CollectionRequestBody(name)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    MyApi().addCollection(collection, header).execute()
                }
                if (response.isSuccessful) {
                    val newCollection = response.body()
                    if(newCollection != null) {
                        val newCollection2 = ImageCollection(response.body()!!.id, response.body()!!.name, 0, mutableListOf())
                        _collections.value = _collections.value?.plus(newCollection2)
                        originalCollections = (_collections.value as MutableList<ImageCollection>?)!!
                    }
                } else {
                    Log.d("CollectionViewModel", "Failed to add collection: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.d("CollectionViewModel", "Failed to add collection: ${e.message}")
            }
        }
    }

    fun fetchCollections(userid: Int, header: String) {
        viewModelScope.launch {
            try {
                val collections = withContext(Dispatchers.IO) {
                    val response = MyApi().getUserCollections(header).execute()
                    if (response.isSuccessful) {
                        response.body()
                    } else {
                        null
                    }
                }

                if (collections != null) {
                    try {
                        val newCollections = collections.map {
                            ImageCollection(it.id, it.name, it.image_count, it.images.toMutableList())
                        }
                        _collections.value = newCollections
                        originalCollections = (_collections.value as MutableList<ImageCollection>?)!!
                    } catch (e: Exception) {
                        Log.e("CollectionViewModel", "Error during mapping: ", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("CollectionViewModel", "Error during fetching collections: ", e)
            }
        }
    }
}

