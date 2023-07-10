package com.example.loginregister.imageprocessing.editor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.example.loginregister.R

class FilterAdapter(private val filters: List<String>, private val listener: OnFilterClickListener) :
    RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    interface OnFilterClickListener {
        fun onFilterClick(filter: String)
    }

    class FilterViewHolder(itemView: View, private val listener: OnFilterClickListener) :
        RecyclerView.ViewHolder(itemView) {
        private val filterButton: Button = itemView.findViewById(R.id.filter_button)

        fun bind(filter: String) {
            filterButton.text = filter
            filterButton.setOnClickListener { listener.onFilterClick(filter) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.filter_item, parent, false)
        return FilterViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bind(filters[position])
    }

    override fun getItemCount() = filters.size
}
