package com.example.loginregister.gallery

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.loginregister.R


class CollectionFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var collectionAdapter: CollectionAdapter
    private lateinit var collectionViewModel: CollectionViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var header: String

    private val sharedPreferences by lazy {
        requireActivity().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
    }
    private val jwt by lazy {
        sharedPreferences.getString("accessToken", null)
    }
    private val userId by lazy {
        sharedPreferences.getInt("userId", 0)
    }

    fun refreshCollections() {
        collectionViewModel.fetchCollections(userId, header)
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Your Collections"
        val searchView = view?.findViewById<SearchView>(R.id.search_view)
        searchView?.setQuery("", false)
        searchView?.clearFocus()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? { return inflater.inflate(R.layout.fragment_collection, container, false)
    }

    private fun filterCollections(query: String) {
        val filteredList = if (query.isEmpty()) {
            // If the query is empty, use the original list
            collectionViewModel.originalCollections
        } else {
            // Otherwise, filter the original list
            collectionViewModel.originalCollections.filter { it.name.contains(query, ignoreCase = true) }
        }

        collectionAdapter.collections.clear()
        collectionAdapter.collections.addAll(filteredList)
        collectionAdapter.notifyDataSetChanged()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchContainer = view.findViewById<FrameLayout>(R.id.search_container)
        val searchView = view.findViewById<SearchView>(R.id.search_view)

        searchContainer.setOnClickListener {
            searchView.isIconified = false
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterCollections(newText.orEmpty())
                return true
            }
        })

        collectionViewModel = ViewModelProvider(requireActivity())[CollectionViewModel::class.java]
        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        recyclerView = view.findViewById(R.id.collectionsRecyclerView)

        recyclerView.layoutManager = GridLayoutManager(context, 2)
        collectionAdapter = CollectionAdapter(collectionViewModel, mutableListOf()) {}
        recyclerView.adapter = collectionAdapter

        collectionViewModel.collections.observe(viewLifecycleOwner) { new_collections ->
            collectionAdapter.collections.clear()
            collectionAdapter.collections.addAll(new_collections)
            collectionAdapter.notifyDataSetChanged()
        }

        collectionViewModel.isCollectionDeleted.observe(viewLifecycleOwner) { isChanged ->
            if(isChanged) {
                val position = collectionAdapter.collections.indexOfFirst { it.id == collectionViewModel.collectionId }
                collectionAdapter.collections.removeAt(position)
                collectionAdapter.notifyItemRemoved(position)
            }
        }

        collectionViewModel.isImageEditedInCollection.observe(viewLifecycleOwner) { isChanged ->
            if(isChanged) {
                val collectionId = collectionViewModel.collectionId
                val imageUrl = collectionViewModel.imageUrl
                val position = collectionAdapter.collections.indexOfFirst { it.id == collectionId }
                val imagePosition = collectionViewModel.imagePosition
                Log.d("CollectionFragment", "imagePosition: $imagePosition")
                Log.d("CollectionFragment", "collectionPositionInAdapter: $position")
                collectionAdapter.collections[position].images[imagePosition].url = imageUrl
                collectionAdapter.notifyItemChanged(position)
            }
        }

        sharedViewModel.isImageDeletedInCollection.observe(viewLifecycleOwner) { isChanged ->
            if(isChanged) {
                val collectionId = sharedViewModel.collectionId
                val position = collectionAdapter.collections.indexOfFirst { it.id == collectionId }
                collectionAdapter.collections[position].image_count = collectionAdapter.collections[position].image_count - 1

                collectionAdapter.collections[position].images.removeAt(sharedViewModel.selectedImagePosition)
                collectionAdapter.notifyItemChanged(position)
            }
        }

        sharedViewModel.selectionDeleted.observe(viewLifecycleOwner) { isChanged ->
            if(isChanged) {
                val collectionId = sharedViewModel.collectionId
                for(image in sharedViewModel.selectedImages) {
                    val position = collectionAdapter.collections.indexOfFirst { it.id == collectionId }
                    collectionAdapter.collections[position].image_count = collectionAdapter.collections[position].image_count - 1

                    collectionAdapter.collections[position].images.remove(image)
                    collectionAdapter.notifyItemChanged(position)
                }
            }
        }

        sharedViewModel.isImageAddedInCollection.observe(viewLifecycleOwner) { isChanged ->
            if(isChanged) {
                val collectionId = collectionAdapter.collections.indexOfFirst { it.id == sharedViewModel.collectionId }

                for(image in sharedViewModel.imageAdded) {
                    collectionAdapter.collections[collectionId].images.add(image)
                    collectionAdapter.collections[collectionId].image_count = collectionAdapter.collections[collectionId].image_count + 1
                    collectionAdapter.notifyItemInserted(collectionAdapter.collections[collectionId].images.size - 1)
                }
                sharedViewModel.imageAdded.clear()
            }
        }

        sharedViewModel.areImagesMoved.observe(viewLifecycleOwner) {isChanged ->
            if (isChanged) {
                val images = sharedViewModel.getImagesToMove()
                val collections = sharedViewModel.getCollectionsToMove()
                val oldCollectionPositionInAdapter = collectionAdapter.collections.indexOfFirst { it.id == sharedViewModel.imagesMovedFrom }

                for (i in images.indices) {
                    val image = images[i]

                    val collectionPositionInAdapter = collectionAdapter.collections.indexOfFirst { it.id == collections[i] }
                    collectionAdapter.collections[collectionPositionInAdapter].images.add(image)
                    collectionAdapter.collections[collectionPositionInAdapter].image_count = collectionAdapter.collections[collectionPositionInAdapter].image_count + 1
                    collectionAdapter.notifyItemInserted(collectionAdapter.collections[collectionPositionInAdapter].images.size - 1)

                    collectionAdapter.collections[oldCollectionPositionInAdapter].images.remove(image)
                    collectionAdapter.collections[oldCollectionPositionInAdapter].image_count = collectionAdapter.collections[oldCollectionPositionInAdapter].image_count - 1
                    collectionAdapter.notifyItemRemoved(oldCollectionPositionInAdapter)
                }
                sharedViewModel.clearImagesToMove()
            }
        }

        header = "Bearer $jwt"
        if (collectionViewModel.collections.value.isNullOrEmpty()) {
            collectionViewModel.fetchCollections(userId, header)
        }
    }
}
