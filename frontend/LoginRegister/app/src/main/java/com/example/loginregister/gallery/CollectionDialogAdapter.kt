package com.example.loginregister.gallery

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.loginregister.R


class CollectionDialogAdapter(
    var collections: MutableList<ImageCollection>,
    private val clickListener: (ImageCollection) -> Unit
) : RecyclerView.Adapter<CollectionDialogAdapter.CollectionViewHolder>() {

    var selectedItem = -1
    var selectedCollection: ImageCollection? = null

    inner class CollectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val collectionImage: ImageView = itemView.findViewById(R.id.collection_image)
        private val collectionName: TextView = itemView.findViewById(R.id.collection_name)

        fun bind(collection: ImageCollection, position: Int, clickListener: (ImageCollection) -> Unit) {

            collectionName.text = collection.name
            collectionImage.setImageResource(R.drawable.logo4)

            itemView.setBackgroundColor(
                if (position == selectedItem)
                    Color.parseColor("#ADD8E6")
                else
                    Color.parseColor("#FFFFFF")
            )

            itemView.setOnClickListener {
                notifyItemChanged(selectedItem)
                selectedItem = position
                selectedCollection = collection
                notifyItemChanged(selectedItem)

                clickListener(collection)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_collection_dialog, parent, false)
        return CollectionViewHolder(view)
    }

    override fun getItemCount() = collections.size

    override fun onBindViewHolder(holder: CollectionViewHolder, position: Int) {
        holder.bind(collections[position], position, clickListener)
    }
}
