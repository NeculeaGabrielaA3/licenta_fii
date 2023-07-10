package com.example.loginregister.gallery

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.loginregister.R

class GalleryAdapter(
    private val collectionId: Int,
    var images: MutableList<Image>,
    private val listener: (Image) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

    private val selectedImages = mutableSetOf<Image>()
    private val selectedIds = mutableSetOf<Int>()
    private var isSelectionMode = false

    fun isInSelectionMode(): Boolean {
        return isSelectionMode
    }

    fun getSelectedImages(): MutableSet<Image> {
        return selectedImages
    }

    inner class GalleryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.galleryImage)
        private val checkView: ImageView = itemView.findViewById(R.id.checkIcon)

        fun bind(image: Image, position: Int, listener: (Image) -> Unit) {
            Glide.with(itemView.context)
                .load(image.url)
                .into(imageView)

            itemView.setBackgroundColor(
                if (image in selectedImages) {Color.GRAY }else Color.TRANSPARENT
            )

            checkView.visibility = if (image in selectedImages) View.VISIBLE else View.GONE
            itemView.findViewById<ImageView>(R.id.galleryImage).alpha = if (image in selectedImages) { 0.5f } else 1.0f

            itemView.setOnClickListener{
                if (isSelectionMode) {
                    toggleSelection(image, itemView)
                } else {
                    val fullImageScreenFragment = FullScreenImageFragment().apply {
                        Log.d("GalleryAdapter", "images: ${images.size}")
                        arguments = Bundle().apply {
                            putString("IMAGE_URL", image.url)
                            putInt("IMAGE_ID", image.id)
                            putInt("IMAGE_POSITION", position)
                            putInt("COLLECTION_ID", collectionId)
                        }
                    }

                    if (itemView.context is AppCompatActivity) {
                        (itemView.context as AppCompatActivity).supportFragmentManager.beginTransaction()
                            .apply {
                                replace(R.id.fragment_container, fullImageScreenFragment)
                                addToBackStack(null)
                                commit()
                            }
                    }
                }
            }

            itemView.setOnLongClickListener {
                if (!isSelectionMode) {
                    isSelectionMode = true
                    toggleSelection(image, itemView)
                }
                else {
                    isSelectionMode = false
                    deselectAll()
                }
                true
            }

        }
    }

    private fun deselectAll() {
        selectedImages.clear()
        notifyDataSetChanged()
    }

    private fun toggleSelection(image: Image, itemView: View) {
        if (image in selectedImages) {
            selectedImages.remove(image)
            selectedIds.remove(image.id)
            itemView.setBackgroundColor(Color.TRANSPARENT)
            itemView.findViewById<ImageView>(R.id.galleryImage).alpha = 1.0f
            itemView.findViewById<ImageView>(R.id.checkIcon).visibility = View.GONE
        } else {
            selectedImages.add(image)
            selectedIds.add(image.id)
            itemView.setBackgroundColor(Color.GRAY)
            itemView.findViewById<ImageView>(R.id.galleryImage).alpha = 0.5f
            itemView.findViewById<ImageView>(R.id.checkIcon).visibility = View.VISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.item_gallery, parent, false)
        return GalleryViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        holder.bind(images[position], position, listener)
    }

    override fun getItemCount() = images.size
}
