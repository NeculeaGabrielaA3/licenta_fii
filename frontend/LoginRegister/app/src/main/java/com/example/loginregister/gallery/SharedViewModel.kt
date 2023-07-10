package com.example.loginregister.gallery

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    data class MoveStructure(val image: Image, val collectionId: Int)

    val isImageDeleted: SingleEventLiveData<Boolean> = SingleEventLiveData()
    var selectedImagePosition: Int = -1

    val isImageEdited: SingleEventLiveData<Boolean> = SingleEventLiveData()
    var newImageUri: Uri = Uri.EMPTY
    private val imageId: MutableLiveData<Int> = MutableLiveData<Int>()

    val isImageDeletedInCollection: SingleEventLiveData<Boolean> = SingleEventLiveData()
    var collectionId: Int = -1
    
    val isImageAddedInCollection: SingleEventLiveData<Boolean> = SingleEventLiveData()
    var imageAdded: MutableList<Image> = mutableListOf()

    val areImagesMoved: SingleEventLiveData<Boolean> = SingleEventLiveData()
    private var imagesMoved: MutableList<MoveStructure> = mutableListOf()
    var imagesMovedFrom: Int = -1

    fun addNewImageToMove(image: Image, collectionId: Int) {
        imagesMoved.add(MoveStructure(image, collectionId))
    }

    fun getImagesToMove(): MutableList<Image> {
        val images = mutableListOf<Image>()
        for (i in imagesMoved) {
            images.add(i.image)
        }
        return images
    }

    fun getCollectionsToMove(): MutableList<Int> {
        val collections = mutableListOf<Int>()
        for (i in imagesMoved) {
            collections.add(i.collectionId)
        }
        return collections
    }

    val selectionDeleted: SingleEventLiveData<Boolean> = SingleEventLiveData()
    var selectedImages: MutableSet<Image> = mutableSetOf()

    fun deleteSelection(images: MutableSet<Image>) {
        selectedImages = images
        selectionDeleted.value = true
    }

    fun updateImageUri(id: Int ,uri: Uri) {
        imageId.value = id
        newImageUri = uri
    }

    fun clearImagesToMove() {
        imagesMoved.clear()
    }
}
