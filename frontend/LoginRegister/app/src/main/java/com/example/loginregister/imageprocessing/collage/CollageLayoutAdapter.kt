package com.example.loginregister.imageprocessing.collage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.loginregister.R

class CollageLayoutAdapter(private val layouts: List<CollageLayout>, private val onLayoutSelected: (CollageLayout) -> Unit) :
    RecyclerView.Adapter<CollageLayoutAdapter.ViewHolder>() {

    var selectedPos = RecyclerView.NO_POSITION

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.layoutImageView)

        fun bind(layout: CollageLayout) {
            imageView.setImageResource(layout.thumbnailResId)

            itemView.setOnClickListener {
                notifyItemChanged(selectedPos)
                selectedPos = layoutPosition
                notifyItemChanged(selectedPos)
                onLayoutSelected(layout)
            }

            itemView.isSelected = (layoutPosition == selectedPos)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(layouts[position])
    }

    override fun getItemCount() = layouts.size
}
