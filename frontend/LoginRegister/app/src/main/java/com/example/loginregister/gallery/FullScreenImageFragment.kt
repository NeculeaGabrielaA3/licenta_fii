package com.example.loginregister.gallery

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.example.loginregister.R
import com.example.loginregister.imageprocessing.editor.PhotoEditingActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import okhttp3.*
import java.io.File
import java.io.IOException

class FullScreenImageFragment : Fragment() {

    interface NavigationVisibility {
        fun showNavigation()
        fun hideNavigation()
    }

    private var navigationVisibility: NavigationVisibility? = null
    private var imageUrl: String? = null
    private lateinit var galleryViewModel: GalleryViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private var imageId: Int = 0
    private var collectionId: Int = 0
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var imageView: ImageView
    private var imagePosition: Int = 0
    private lateinit var imagePagerAdapter: ImagePagerAdapter
    private lateinit var collectionDialog: CollectionDialogFragment

    override fun onResume() {
        super.onResume()
        navigationVisibility?.hideNavigation()
    }

    override fun onStop() {
        super.onStop()
        navigationVisibility?.showNavigation()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is NavigationVisibility) {
            navigationVisibility = context
        } else {
            throw ClassCastException("$context must implement NavigationVisibility")
        }
    }

    private lateinit var editImageActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var collectionViewModel: CollectionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectionViewModel = ViewModelProvider(requireActivity())[CollectionViewModel::class.java]
        galleryViewModel = ViewModelProvider(requireActivity())[GalleryViewModel::class.java]
        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        editImageActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val editedImageUri = result.data?.getStringExtra("EDITED_IMAGE_URI")

                imagePagerAdapter.updateImageUri(imagePosition, editedImageUri)

                Glide.with(this)
                    .load(editedImageUri)
                    .into(imageView)

                sharedViewModel.isImageEdited.value = true
                sharedViewModel.selectedImagePosition = imagePosition
                sharedViewModel.collectionId = collectionId
                sharedViewModel.updateImageUri(imageId, editedImageUri?.let { Uri.parse(it) }!!)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.full_screen_image_fragment, container, false)

        imageUrl = arguments?.getString("IMAGE_URL")
        imageId = arguments?.getInt("IMAGE_ID")!!
        collectionId = arguments?.getInt("COLLECTION_ID")!!
        imagePosition = arguments?.getInt("IMAGE_POSITION")!!

        imageView = view.findViewById(R.id.full_screen_image_view)
        bottomNavigation = view.findViewById(R.id.bottom_navigation)

        Glide.with(this)
            .load(imageUrl)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.e("FullScreenImageFragment", "Image loading failed", e)
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Drawable>?,
                    dataSource: com.bumptech.glide.load.DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d("FullScreenImageFragment", "Image loading succeeded")
                    return false
                }
            })
            .into(imageView)

        galleryViewModel.images.observe(viewLifecycleOwner) { imageList ->
            val viewPager = view.findViewById<ViewPager>(R.id.viewPager)
            imagePagerAdapter = ImagePagerAdapter(requireContext(), imageList)
            viewPager.adapter = imagePagerAdapter

            viewPager.currentItem = imagePosition
        }

        bottomNavigation.menu.setGroupCheckable(0, false, true)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_delete -> {
                    val builder = AlertDialog.Builder(requireContext(), R.style.RoundedDialog)

                    val inflater = layoutInflater
                    val dialogView = inflater.inflate(R.layout.dialog_layout_delete, null)
                    builder.setView(dialogView)
                    val alertDialog = builder.create()

                    val yesButton = dialogView.findViewById<Button>(R.id.yesButton)
                    yesButton.setOnClickListener {
                        alertDialog.dismiss()

                        sharedViewModel.selectedImagePosition = imagePosition
                        sharedViewModel.isImageDeleted.value = true
                        sharedViewModel.collectionId = collectionId
                        parentFragmentManager.popBackStack()
                    }

                    val noButton = dialogView.findViewById<Button>(R.id.noButton)
                    noButton.setOnClickListener {
                        alertDialog.dismiss()
                    }

                    alertDialog.show()
                    true
                }
                R.id.nav_edit -> {
                    val targetFileName =
                        "target_file_name.jpg"

                    val client = OkHttpClient()
                    val request = imageUrl?.let { Request.Builder().url(it).build() }

                    if (request != null) {
                        client.newCall(request).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                // handle failure
                            }

                            override fun onResponse(call: Call, response: Response) {
                                response.body?.byteStream()?.let { inputStream ->
                                    val file = File(context?.cacheDir, targetFileName)
                                    file.outputStream().use { fileOut ->
                                        inputStream.copyTo(fileOut)
                                    }

                                    val imageUri = Uri.fromFile(file)

                                    val intent = Intent(context, PhotoEditingActivity::class.java)
                                    intent.putExtra("fileName", imageUri.toString())
                                    intent.putExtra("fromCollageOrFS", true.toString())
                                    editImageActivityResultLauncher.launch(intent)
                                }
                            }
                        })
                    }
                    true
                }
                R.id.nav_move -> {
                    collectionDialog = CollectionDialogFragment()
                    collectionDialog.show(
                        requireActivity().supportFragmentManager,
                        "collectionDialog"
                    )

                    collectionDialog.setOnSaveClickListener {
                        val selectedCollection = collectionDialog.getSelectedCollection()
                        if (selectedCollection == null) {
                            Toast.makeText(requireContext(), "Please select a collection",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            val sharedPreferences =
                                requireActivity().applicationContext.getSharedPreferences(
                                    "myPrefs",
                                    Context.MODE_PRIVATE
                                )
                            val jwt = sharedPreferences?.getString("accessToken", null)
                            sharedPreferences?.getInt("userId", 1)
                            val header = "Bearer $jwt"

                            galleryViewModel.moveImageToCollection(
                                header,
                                imageId,
                                collectionId,
                                selectedCollection.id,
                                object :
                                    GalleryViewModel.AddImageCallback {
                                    override fun onSuccess(newImage: Image) {
                                        sharedViewModel.areImagesMoved.value = true
                                        sharedViewModel.addNewImageToMove(
                                            newImage,
                                            selectedCollection.id
                                        )
                                        sharedViewModel.imagesMovedFrom = collectionId
                                        collectionDialog.dismiss()
                                        parentFragmentManager.popBackStack()
                                    }

                                    override fun onFailure(message: String) {
                                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG
                                        ).show()
                                    }
                                })
                        }
                    }
                    true
                }
                R.id.nav_save -> {

                    val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.RoundedDialog)

                    val inflater = LayoutInflater.from(requireContext())
                    val dialogView = inflater.inflate(R.layout.dialog_layout1, null)

                    builder.setView(dialogView)
                    val alertDialog = builder.create()
                    dialogView.findViewById<TextView>(R.id.dialogMessage).text = "Do you want to save this image to your device gallery?"
                    alertDialog.show()
                    val yesButton = dialogView.findViewById<Button>(R.id.yesButton)
                    yesButton.setOnClickListener {
                        alertDialog.dismiss()
                        Glide.with(this)
                            .asBitmap()
                            .load(imageUrl)
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    saveImageToGallery(resource)
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                }
                            })
                    }
                    val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
                    cancelButton.setOnClickListener {
                        alertDialog.dismiss()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Image.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { //this one
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri: Uri? = requireActivity().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            requireActivity().contentResolver.openOutputStream(it)?.let { outputStream ->
                if(!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                    throw IOException("Failed to save bitmap.")
                }
                Toast.makeText(requireContext(), "Image saved successfully", Toast.LENGTH_SHORT).show()

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    requireActivity().contentResolver.update(uri, contentValues, null, null)
                }
            } ?: throw IOException("Failed to create new MediaStore record.")
        } ?: throw IOException("Could not insert image into MediaStore")
    }
}