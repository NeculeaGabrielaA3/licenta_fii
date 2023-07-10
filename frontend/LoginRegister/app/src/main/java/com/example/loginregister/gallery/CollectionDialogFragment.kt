package com.example.loginregister.gallery

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.example.loginregister.R
import com.example.loginregister.api.CollectionRequestBody
import com.example.loginregister.api.CollectionResponse
import com.example.loginregister.api.MyApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CollectionDialogFragment : DialogFragment() {

    private lateinit var collectionDialogAdapter: CollectionDialogAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var addNewCollectionButton: Button
    private lateinit var closeDialogButton: ImageButton
    private lateinit var saveButton: Button

    override fun onStart() {
        super.onStart()

        val d = dialog
        if (d != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            d.window!!.setLayout(width, height)
        }
    }

    private lateinit var onSaveClickListener: () -> Unit

    fun setOnSaveClickListener(listener: () -> Unit) {
        onSaveClickListener = listener
    }

    fun getSelectedCollection(): ImageCollection? {
        return collectionDialogAdapter.selectedCollection
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_collection_dialog, container, false)

        recyclerView = view.findViewById(R.id.collectionsRecyclerView)
        addNewCollectionButton = view.findViewById(R.id.addNewCollectionButton)
        closeDialogButton = view.findViewById(R.id.closeDialogButton)
        saveButton = view.findViewById(R.id.saveButton)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences =
            activity?.applicationContext?.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val jwt = sharedPreferences?.getString("accessToken", null)
        val userId = sharedPreferences?.getInt("userId", 0)

        collectionDialogAdapter = CollectionDialogAdapter(mutableListOf()) { collection ->
            //Toast.makeText(context, "Clicked: ${collection.name}", Toast.LENGTH_LONG).show()
        }

        recyclerView.adapter = collectionDialogAdapter

        val header = "Bearer $jwt"
        if (userId != null) {
            MyApi().getUserCollections(header).enqueue(object : Callback<List<CollectionResponse>> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(
                    call: Call<List<CollectionResponse>>,
                    response: Response<List<CollectionResponse>>
                ) {
                    if (response.isSuccessful) {
                        val collectionResponses = response.body() ?: return
                        val collections = collectionResponses.map { ImageCollection(it.id, it.name, it.image_count, mutableListOf()) }

                        activity?.runOnUiThread {
                            collectionDialogAdapter.collections.clear()
                            collectionDialogAdapter.collections.addAll(collections)
                            collectionDialogAdapter.notifyDataSetChanged()
                        }
                    } else {

                        Toast.makeText(context, "Error getting collections!", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<List<CollectionResponse>>, t: Throwable) {
                    Toast.makeText(context, "Error getting collections!", Toast.LENGTH_LONG).show()
                }
            })
        }

        saveButton.setOnClickListener {
            onSaveClickListener()
        }

        addNewCollectionButton.setOnClickListener {
            showAddCollectionDialog()
        }

        closeDialogButton.setOnClickListener {
            dismiss()
        }
    }

    private fun showAddCollectionDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("New Collection")

        val input = EditText(context)
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            val newCollectionName = input.text.toString()
            if (newCollectionName.isNotBlank()) {
                val sharedPreferences =
                    activity?.applicationContext?.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
                val jwt = sharedPreferences?.getString("accessToken", null)
                val collectionRequest = CollectionRequestBody(newCollectionName)
                val userId = sharedPreferences?.getInt("userId", 0) // replace with the actual user ID
                val header = "Bearer $jwt"

                if (userId != null) {
                    MyApi().addCollection(collectionRequest, header).enqueue(object : Callback<CollectionResponse> {
                        override fun onResponse(call: Call<CollectionResponse>, response: Response<CollectionResponse>) {
                            if (response.isSuccessful) {
                                val newCollection = response.body()
                                if (newCollection != null) {

                                    val collection = ImageCollection(newCollection.id, newCollection.name, 0, mutableListOf())
                                    collectionDialogAdapter.collections.add(collection)
                                    collectionDialogAdapter.notifyItemInserted(collectionDialogAdapter.collections.size - 1)
                                } else {
                                    Log.d("CollectionDialogFragment", "onResponse: newCollection is null")
                                }
                            } else {
                                Toast.makeText(context, "Error adding the collection!", Toast.LENGTH_LONG).show()
                                Log.d("CollectionDialogFragment", "onResponse: response is not successful")
                            }
                        }

                        override fun onFailure(call: Call<CollectionResponse>, t: Throwable) {
                            Toast.makeText(context, "Error adding the collection!", Toast.LENGTH_LONG).show()
                            Log.d("CollectionDialogFragment", "onFailure: ${t.message}")
                        }
                    })
                }
            } else {
                Toast.makeText(context, "Collection name cannot be empty", Toast.LENGTH_LONG).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }
}
