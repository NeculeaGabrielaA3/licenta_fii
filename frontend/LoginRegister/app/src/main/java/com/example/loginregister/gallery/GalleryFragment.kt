package com.example.loginregister.gallery

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.loginregister.R
import com.github.drjacky.imagepicker.ImagePicker
import com.github.drjacky.imagepicker.constant.ImageProvider
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class GalleryFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var galleryAdapter: GalleryAdapter
    private var collectionId: Int = -1
    private var collectionName: String = ""

    private lateinit var fabMain : FloatingActionButton
    private lateinit var fabAddImage : ExtendedFloatingActionButton
    private lateinit var fabDeleteCollection : ExtendedFloatingActionButton
    private lateinit var fabDeleteSelection: ExtendedFloatingActionButton
    private lateinit var fabMoveSelection: ExtendedFloatingActionButton

    private lateinit var galleryViewModel: GalleryViewModel
    private lateinit var collectionViewModel: CollectionViewModel
    private lateinit var sharedViewModel: SharedViewModel

    private lateinit var collectionDialog: CollectionDialogFragment
    private lateinit var header: String

    override fun onResume() {
        super.onResume()
        Log.d("GalleryFragment", "images: ${galleryAdapter.images}")
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val selectedImageUri = it.data?.data!!
                galleryViewModel.saveImageToCollection(selectedImageUri, header, collectionId, object :
                    GalleryViewModel.AddImageCallback {
                    override fun onSuccess(newImage: Image) {
                        galleryAdapter.images.add(newImage)
                        sharedViewModel.isImageAddedInCollection.value = true
                        sharedViewModel.imageAdded.add(newImage)
                        sharedViewModel.collectionId = collectionId
                        galleryAdapter.notifyItemInserted(galleryAdapter.images.size - 1)
                    }

                    override fun onFailure(message: String) {
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                })
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        collectionId = arguments?.getInt("COLLECTION_ID") ?: -1
        collectionName = arguments?.getString("COLLECTION_NAME") ?: ""
        galleryAdapter = GalleryAdapter(collectionId, mutableListOf()) {}

        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        galleryViewModel = ViewModelProvider(requireActivity())[GalleryViewModel::class.java]
        collectionViewModel = ViewModelProvider(requireActivity())[CollectionViewModel::class.java]
        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        val sharedPreferences =
            activity?.applicationContext?.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val jwt = sharedPreferences?.getString("accessToken", null)
        header = "Bearer $jwt"

        galleryViewModel.images.observe(viewLifecycleOwner) { images ->
            galleryAdapter.images.clear()
            galleryAdapter.images.addAll(images)
            galleryAdapter.notifyDataSetChanged()
            handleVisibility(images.isNotEmpty())
        }

        handleRecyclerView(view)
        if (collectionId != galleryViewModel.previousCollectionId)
            galleryViewModel.fetchImages(header, collectionId)

        galleryViewModel.previousCollectionId = collectionId

        initFabButtons(view)
        initViewModels()

        val toolbar = (requireActivity() as AppCompatActivity).supportActionBar
        toolbar?.title = collectionName
    }

    private fun initViewModels() {

        sharedViewModel.isImageEdited.observe(viewLifecycleOwner) { isEdited ->
            if(isEdited) {
                val position = sharedViewModel.selectedImagePosition
                val imageId = galleryAdapter.images[position].id
                val imageUri = sharedViewModel.newImageUri

                galleryViewModel.modifyImageInCollection(collectionId, imageId, header, imageUri, object :
                    GalleryViewModel.ModifyImageCallback {
                    override fun onSuccess(newUrl: String) {
                        galleryAdapter.images[position].url = newUrl
                        collectionViewModel.isImageEditedInCollection.value = true
                        collectionViewModel.collectionId = collectionId
                        collectionViewModel.imagePosition = position
                        Log.d("GalleryFragment", "Image edited position: $position")
                        collectionViewModel.imageUrl = newUrl
                    }

                    override fun onFailure(message: String) {
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                })
            }
        }

        sharedViewModel.isImageDeleted.observe(viewLifecycleOwner) { isDeleted ->
            if (isDeleted) {
                val position = sharedViewModel.selectedImagePosition
                val imageId = galleryAdapter.images[position].id
                sharedViewModel.isImageDeletedInCollection.value = true
                sharedViewModel.collectionId = collectionId
                galleryAdapter.images.removeAt(position)
                galleryAdapter.notifyItemRemoved(position)
                galleryViewModel.deleteImageFromCollection(collectionId, imageId, header)
            }
        }
    }

    private fun handleRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.galleryRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 3)
        recyclerView.adapter = galleryAdapter
    }

    private fun handleVisibility(hasImages: Boolean) {
        val visibility = if (hasImages) View.VISIBLE else View.GONE
        val emptyVisibility = if (hasImages) View.GONE else View.VISIBLE

        recyclerView.visibility = visibility
        val emptyView: View = view?.findViewById(R.id.emptyGalleryView) ?: return
        emptyView.visibility = emptyVisibility
    }

    private fun initFabButtons(view: View) {
        fabMain = view.findViewById(R.id.fab_main)
        fabAddImage = view.findViewById(R.id.fab_add_image)
        fabDeleteCollection = view.findViewById(R.id.fab_delete_collection)
        fabDeleteSelection = view.findViewById(R.id.fab_delete_selection)
        fabMoveSelection = view.findViewById(R.id.fab_move_selection)

        fabMain.setOnClickListener {
            if (fabAddImage.visibility == View.VISIBLE || fabDeleteCollection.visibility == View.VISIBLE || fabDeleteSelection.visibility == View.VISIBLE) {
                fabAddImage.visibility = View.GONE
                fabDeleteCollection.visibility = View.GONE
                fabDeleteSelection.visibility = View.GONE
                fabMoveSelection.visibility = View.GONE
            }
            else {
                if (galleryAdapter.isInSelectionMode()) {
                    if (galleryAdapter.images.isEmpty()) {
                        fabAddImage.visibility = View.VISIBLE
                        fabDeleteCollection.visibility = View.VISIBLE
                    } else {
                        fabDeleteSelection.visibility = View.VISIBLE
                        fabMoveSelection.visibility = View.VISIBLE
                    }
                } else {
                    fabAddImage.visibility = View.VISIBLE
                    fabDeleteCollection.visibility = View.VISIBLE
                }
            }
        }

        fabAddImage.setOnClickListener {
            ImagePicker.with(requireActivity())
                .crop()
                .provider(ImageProvider.BOTH)
                .createIntentFromDialog { launcher.launch(it) }
            fabAddImage.visibility = View.GONE
            fabDeleteCollection.visibility = View.GONE
        }

        fabDeleteCollection.setOnClickListener {
            val sharedPreferences =
                activity?.applicationContext?.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
            val userId = sharedPreferences?.getInt("userId", -1)
            showAlertDialog(userId)
        }

        fabDeleteSelection.setOnClickListener {
            val builder = AlertDialog.Builder(it.context, R.style.RoundedDialog)

            val inflater = LayoutInflater.from(it.context)
            val dialogView = inflater.inflate(R.layout.dialog_layout1, null)

            builder.setView(dialogView)
            val alertDialog = builder.create()
            dialogView.findViewById<TextView>(R.id.dialogMessage).text = "Are you sure you want to delete this images?"
            alertDialog.show()
            val yesButton = dialogView.findViewById<Button>(R.id.yesButton)
            yesButton.setOnClickListener {
                alertDialog.dismiss()
                val selectedImages = galleryAdapter.getSelectedImages()

                fabDeleteSelection.visibility = View.GONE
                fabMoveSelection.visibility = View.GONE
                for(image in selectedImages){
                    galleryViewModel.deleteImageFromCollection(collectionId, image.id, header)
                    collectionViewModel.deleteImageFromCollection(collectionId, image.id)
                }
                sharedViewModel.deleteSelection(selectedImages)
                sharedViewModel.collectionId = collectionId
            }

            val noButton = dialogView.findViewById<Button>(R.id.noButton)
            noButton.setOnClickListener {
                alertDialog.dismiss()
                fabDeleteSelection.visibility = View.GONE
                fabMoveSelection.visibility = View.GONE
            }
        }

        fabMoveSelection.setOnClickListener {
            collectionDialog = CollectionDialogFragment()
            collectionDialog.show(requireActivity().supportFragmentManager, "collectionDialog")

            collectionDialog.setOnSaveClickListener {
                val selectedCollection = collectionDialog.getSelectedCollection()
                if (selectedCollection == null) {
                    Toast.makeText(requireContext(), "Please select a collection", Toast.LENGTH_LONG).show()
                } else {
                    val sharedPreferences =
                        requireActivity().applicationContext.getSharedPreferences(
                            "myPrefs",
                            Context.MODE_PRIVATE
                        )
                    val jwt = sharedPreferences?.getString("accessToken", null)
                    sharedPreferences?.getInt("userId", 1)
                    val header = "Bearer $jwt"

                    val selectedImages = galleryAdapter.getSelectedImages()
                    for (image in selectedImages) {
                        val imageId = image.id
                        galleryViewModel.moveImageToCollection(header, imageId, collectionId, selectedCollection.id, object :
                            GalleryViewModel.AddImageCallback {
                            override fun onSuccess(newImage: Image) {
                                sharedViewModel.areImagesMoved.value = true
                                sharedViewModel.addNewImageToMove(newImage, selectedCollection.id)
                                sharedViewModel.imagesMovedFrom = collectionId

                                collectionDialog.dismiss()
                                //parentFragmentManager.popBackStack()
                                fabDeleteSelection.visibility = View.GONE
                                fabMoveSelection.visibility = View.GONE
                            }

                            override fun onFailure(message: String) {
                                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                            }
                        })
                    }
                }
            }
        }
    }

    private fun showAlertDialog(userId: Int?) {
        val builder = AlertDialog.Builder(requireContext(), R.style.RoundedDialog)

        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_layout_delete_collection, null)
        builder.setView(dialogView)
        val alertDialog = builder.create()

        val yesButton = dialogView.findViewById<Button>(R.id.yesButton)
        yesButton.setOnClickListener {

            alertDialog.dismiss()
            if (userId != null) {
                collectionViewModel.isCollectionDeleted.value = true
                collectionViewModel.collectionId = collectionId
                collectionViewModel.deleteCollection(header, collectionId)
            }
            fabAddImage.visibility = View.GONE
            fabDeleteCollection.visibility = View.GONE
            parentFragmentManager.popBackStack()
        }

        val noButton = dialogView.findViewById<Button>(R.id.noButton)
        noButton.setOnClickListener {
            alertDialog.dismiss()
            fabAddImage.visibility = View.GONE
            fabDeleteCollection.visibility = View.GONE
        }
        alertDialog.show()
    }
}
