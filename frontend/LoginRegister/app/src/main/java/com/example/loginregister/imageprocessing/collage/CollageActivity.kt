package com.example.loginregister.imageprocessing.collage

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.loginregister.R
import com.example.loginregister.api.AddImageResponse
import com.example.loginregister.api.MyApi
import com.example.loginregister.api.UploadRequestBody
import com.example.loginregister.gallery.CollectionDialogFragment
import com.example.loginregister.gallery.CollectionViewModel
import com.example.loginregister.gallery.ImageCollection
import com.example.loginregister.imageprocessing.editor.PhotoEditingActivity
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import java.io.*

class CollageActivity : AppCompatActivity() {

    companion object {
        val LAYOUT_MAP = mapOf(
            "LAYOUT1" to R.layout.layout1,
            "LAYOUT2" to R.layout.layout8,
            "LAYOUT3" to R.layout.layout3,
            "LAYOUT4" to R.layout.layout4,
            "LAYOUT5" to R.layout.layout5,
            "LAYOUT6" to R.layout.layout6,
            "LAYOUT7" to R.layout.layout7,
            "LAYOUT8" to R.layout.layout2,
            "LAYOUT9" to R.layout.layout9,
            "LAYOUT10" to R.layout.layout10
        )
    }

    private var changesMade = false
    private var collectionId = -1
    private lateinit var collectionDialog: CollectionDialogFragment
    private lateinit var collectionViewModel: CollectionViewModel

    private var selectedImageView: PhotoView? = null
    private var previouslySelectedView: PhotoView? = null

    private var urisFromCollage = mutableListOf<Uri>()
    private var notEmptyPhotoViews = mutableListOf<PhotoView>()

    private lateinit var editImageActivityResultLauncher: ActivityResultLauncher<Intent>
    private val pickImageLauncher = registerForActivityResult( ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && selectedImageView != null) {
            handlePickedImage(uri, selectedImageView!!)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.collage_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                if (previouslySelectedView != null)
                    previouslySelectedView?.foreground = null
                val view = findViewById<View>(R.id.action_save)
                createPopUpMenu(view)

                true
            }
            android.R.id.home -> {
                val returnIntent = Intent()
                returnIntent.putExtra("changes_made", changesMade)
                setResult(Activity.RESULT_OK, returnIntent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createPopUpMenu(view: View?) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.your_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.option_1 -> {
                    createCollectionDialog()
                    true
                }
                R.id.option_2 -> {
                    val collageLayout: ConstraintLayout = findViewById(R.id.collageLayout)
                    val bitmap = getBitmapFromView(collageLayout)
                    saveImageToGallery(bitmap)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun createCollectionDialog() {
        collectionDialog = CollectionDialogFragment()
        collectionDialog.show(supportFragmentManager, "collectionDialog")

        collectionDialog.setOnSaveClickListener {
            val selectedCollection = collectionDialog.getSelectedCollection()
            if (selectedCollection == null) {
                Toast.makeText(this, "Please select a collection", Toast.LENGTH_LONG).show()
            } else {
                addImageToCollection(selectedCollection)
                changesMade = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val layoutName = intent.getStringExtra("selectedLayout")

        collectionViewModel = ViewModelProvider(this)[CollectionViewModel::class.java]

        LAYOUT_MAP[layoutName]?.let { setContentView(it) }

        val collageLayout = findViewById<ConstraintLayout>(R.id.collageLayout)
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.activity_collage, null)
        val layout = findViewById<FrameLayout>(R.id.frameLayout)
        layout.addView(view)

        supportActionBar?.title = "Collage"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        editImageActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val editedImageUri = result.data?.getStringExtra("EDITED_IMAGE_URI")
                if (editedImageUri != null) {
                    handlePickedImage(Uri.parse(editedImageUri), selectedImageView!!)
                }
            }
        }

        val fabEditButton = findViewById<ExtendedFloatingActionButton>(R.id.fab_edit)
        var selectionMode = false

        for (i in 0 until collageLayout.childCount) {
            val photoView = collageLayout.getChildAt(i)
            if (photoView is PhotoView) {
                photoView.setImageResource(R.drawable.gray)

                photoView.setOnViewTapListener { _, _, _ ->
                    if(!selectionMode) {
                        previouslySelectedView?.foreground = null
                        openImagePicker(photoView)
                    } else {
                        previouslySelectedView?.foreground = null
                        selectedImageView = photoView
                        previouslySelectedView = photoView
                        photoView.foreground = ContextCompat.getDrawable(this, R.drawable.selected_image_border)
                    }
                }
                photoView.setOnLongClickListener {
                    if (selectionMode) {
                        selectionMode = false
                        previouslySelectedView?.foreground = null
                        fabEditButton.visibility = View.GONE
                    } else {
                        selectionMode = true
                        fabEditButton.visibility = View.VISIBLE
                        previouslySelectedView?.foreground = null
                        photoView.foreground = ContextCompat.getDrawable(this, R.drawable.selected_image_border)

                        selectedImageView = photoView
                        previouslySelectedView = photoView
                    }

                    true
                }
            }
        }

        fabEditButton.setOnClickListener {
            if (notEmptyPhotoViews.contains(selectedImageView)) {
                val viewPositionInList = notEmptyPhotoViews.indexOf(selectedImageView)
                val uri = urisFromCollage[viewPositionInList]
                val editIntent = Intent(this, PhotoEditingActivity::class.java)

                editIntent.putExtra("fileName", uri.toString())
                editIntent.putExtra("fromCollageOrFS", true.toString())
                editImageActivityResultLauncher.launch(editIntent)
            } else {
                Toast.makeText(this, "There is no image to edit!", Toast.LENGTH_LONG).show()
            }

        }

        val callback = handleBackPress()
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun handleBackPress(): OnBackPressedCallback {
        val callback = object : OnBackPressedCallback(true /* enabled by default */) {
            override fun handleOnBackPressed() {
                val builder = AlertDialog.Builder(this@CollageActivity, R.style.RoundedDialog)

                val inflater1 = layoutInflater
                val dialogView = inflater1.inflate(R.layout.dialog_layout1, null)
                builder.setView(dialogView)
                val alertDialog = builder.create()

                val yesButton = dialogView.findViewById<Button>(R.id.yesButton)
                yesButton.setOnClickListener {
                    alertDialog.dismiss()

                    val returnIntent = Intent()
                    returnIntent.putExtra("changes_made", changesMade)
                    returnIntent.putExtra("collection_id", collectionId)

                    setResult(Activity.RESULT_OK, returnIntent)
                    finish()
                }

                val noButton = dialogView.findViewById<Button>(R.id.noButton)
                noButton.setOnClickListener {
                    alertDialog.dismiss()
                }

                alertDialog.show()
            }
        }
        return callback
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val focusedEditTexts = ArrayList<EditText>()
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is EditText && child.isFocused) {
                    focusedEditTexts.add(child)
                    child.clearFocus()
                }
            }
        }

        view.draw(canvas)

        for (editText in focusedEditTexts) {
            editText.requestFocus()
        }

        return bitmap
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

        val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            contentResolver.openOutputStream(it)?.let { outputStream ->
                if(!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                    throw IOException("Failed to save bitmap.")
                }

                Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show()

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { //this one
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
            } ?: throw IOException("Failed to create new MediaStore record.")
        } ?: throw IOException("Could not insert image into MediaStore")
    }

    private fun addImageToCollection(selectedCollection: ImageCollection) {
        val collageLayout: ConstraintLayout = findViewById(R.id.collageLayout)
        collectionId = selectedCollection.id
        val bitmap = getBitmapFromView(collageLayout)
        val file2 = getCollageFile(this)
        saveBitmapToFile(bitmap, file2)

        val file = File.createTempFile("image", null, cacheDir)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.flush()
        outputStream.close()

        val body = UploadRequestBody(file, "image")
        val sharedPreferences =
            applicationContext.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val jwt = sharedPreferences.getString("accessToken", null)

        val header = "Bearer $jwt"

        MyApi().addImageToCollection(
            collectionId,
            MultipartBody.Part.createFormData(
                "image",
                file.name,
                body
            ),
            "json".toRequestBody("multipart/form-data".toMediaTypeOrNull()),
            header
        ).enqueue(object : Callback<AddImageResponse> {
            override fun onResponse(
                call: Call<AddImageResponse>,
                response: retrofit2.Response<AddImageResponse>

            ) {
                if(response.body()?.message == "Image uploaded successfully!") {
                    Toast.makeText(baseContext, "Image uploaded successfully!", Toast.LENGTH_SHORT)
                        .show()
                    val userId = sharedPreferences.getInt("userId", 0)
                    collectionViewModel.fetchCollections(userId, header)
                }else
                    Toast.makeText(baseContext, "Error uploading image!", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(call: Call<AddImageResponse>, t: Throwable) {
                Toast.makeText(baseContext, "Error uploading image!", Toast.LENGTH_SHORT).show()
            }

        })
    }

    @Throws(IOException::class)
    fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        val fos = FileOutputStream(file)
        fos.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
    }

    private fun getCollageFile(context: Context): File {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File(directory, "collage.png")
    }

    private fun handlePickedImage(uri: Uri, photoView: PhotoView) {
        if(!notEmptyPhotoViews.contains(photoView)){
            notEmptyPhotoViews.add(photoView)
            urisFromCollage.add(uri)
        }
        else {
            val viewPositionInList = notEmptyPhotoViews.indexOf(photoView)
            urisFromCollage[viewPositionInList] = uri
        }
        photoView.setImageURI(uri)
    }

    private fun openImagePicker(photoView: PhotoView) {
        selectedImageView = photoView
        pickImageLauncher.launch("image/*")
    }
}