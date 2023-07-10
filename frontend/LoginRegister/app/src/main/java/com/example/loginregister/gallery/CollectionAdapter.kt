package com.example.loginregister.gallery

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.loginregister.R

class CollectionAdapter(private val collectionViewModel: CollectionViewModel,
                        var collections: MutableList<ImageCollection>,
                        private val clickListener: (ImageCollection) -> Unit
) : RecyclerView.Adapter<CollectionAdapter.CollectionViewHolder>() {

    inner class CollectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val collectionName: TextView = itemView.findViewById(R.id.collectionName)
        private val collectionImage: ImageView = itemView.findViewById(R.id.collectionCoverImage)
        private val collectionSize: TextView = itemView.findViewById(R.id.collectionImageCount)
        private val trashImageView: ImageView = itemView.findViewById(R.id.trashImage)

        fun bind(collection: ImageCollection, position: Int, clickListener: (ImageCollection) -> Unit) {
            collectionName.text = collection.name
            collectionSize.text = "${collection.image_count.toString()} Images"
            val imageUrl: String
            if(collection.image_count > 0) {
                imageUrl = collection.images[collection.image_count - 1].url
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .into(collectionImage)
            } else {
                collectionImage.setImageResource(R.drawable.no_images)
            }

            itemView.setOnClickListener{
                val galleryFragment = GalleryFragment().apply {
                    arguments = Bundle().apply {
                        putInt("COLLECTION_ID", collection.id)
                        putString("COLLECTION_NAME", collection.name)
                    }
                }

                if (itemView.context is AppCompatActivity) {
                    (itemView.context as AppCompatActivity).supportFragmentManager.beginTransaction().apply {
                        replace(R.id.fragment_container, galleryFragment)
                        addToBackStack(null)
                        commit()
                    }
                }
            }

            itemView.setOnLongClickListener {
                if (trashImageView.visibility == View.VISIBLE) {
                    trashImageView.visibility = View.INVISIBLE
                    collectionImage.alpha = 1f
                } else {
                    trashImageView.visibility = View.VISIBLE
                    collectionImage.alpha = 0.5f
                }
                true
            }

            trashImageView.setOnClickListener { it ->
                val builder = AlertDialog.Builder(it.context, R.style.RoundedDialog)
                val inflater = LayoutInflater.from(it.context)
                val dialogView = inflater.inflate(R.layout.dialog_layout_collections, null)

                builder.setView(dialogView)
                val alertDialog = builder.create()
                dialogView.findViewById<TextView>(R.id.dialogMessage).text = "Are you sure you want to delete this collection?"
                alertDialog.show()
                val yesButton = dialogView.findViewById<Button>(R.id.yesButton)
                yesButton.setOnClickListener {
                    alertDialog.dismiss()
                    val sharedPreferences = it.context.applicationContext.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
                    val jwt = sharedPreferences.getString("accessToken", null)
                    val header = "Bearer $jwt"
                    collectionViewModel.deleteCollection(header, collection.id)
                    trashImageView.visibility = View.INVISIBLE
                    collectionImage.alpha = 1f
                }

                val noButton = dialogView.findViewById<Button>(R.id.noButton)
                noButton.setOnClickListener {
                    alertDialog.dismiss()
                    trashImageView.visibility = View.INVISIBLE
                    collectionImage.alpha = 1f
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_collection, parent, false)
        return CollectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CollectionViewHolder, position: Int) {
        holder.bind(collections[position], position, clickListener)
    }

    override fun getItemCount(): Int {
        return collections.size
    }
}