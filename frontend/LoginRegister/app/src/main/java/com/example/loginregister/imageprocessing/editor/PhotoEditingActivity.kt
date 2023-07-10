package com.example.loginregister.imageprocessing.editor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.loginregister.R
import com.example.loginregister.api.AddImageResponse
import com.example.loginregister.api.MyApi
import com.example.loginregister.api.UploadRequestBody
import com.example.loginregister.gallery.CollectionDialogFragment
import com.example.loginregister.gallery.ImageCollection
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yalantis.ucrop.UCrop
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class PhotoEditingActivity : AppCompatActivity() {
    private var selectedImageUri: Uri? = null
    private lateinit var imageView: ImageView

    private val imageVersions: MutableList<Bitmap> = mutableListOf()
    private val imageFilters: MutableList<String> = mutableListOf()
    private val imageBrightnessPercentage: MutableList<Int> = mutableListOf()
    private val imageSaturationPercentage: MutableList<Int> = mutableListOf()
    private val imageBlurPercentage: MutableList<Int> = mutableListOf()

    private lateinit var lastImageWithoutBrightness: Bitmap
    private lateinit var draggableTextView: DraggableTextView
    private lateinit var currentImage: Bitmap
    private var lastBrightnessPercentage: Int = 50
    private lateinit var collectionDialog: CollectionDialogFragment

    private var changesMade = false

    private val cropActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = result.data?.let { UCrop.getOutput(it) }
            imageView.setImageURI(resultUri)
            addImageVersion((imageView.drawable as BitmapDrawable).bitmap)
            addImageFilter("none")
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            result.data?.let { UCrop.getError(it) }
        }
    }

    fun addImageVersion(bitmap: Bitmap) {
        imageVersions.add(bitmap)
    }

    fun setLastImageWithoutBrightness(bitmap: Bitmap) {
        lastImageWithoutBrightness = bitmap
    }

    fun addImageBlurPercentage(percentage: Int) {
        imageBlurPercentage.add(percentage)
    }

    fun addImageSaturationPercentage(percentage: Int) {
        imageSaturationPercentage.add(percentage)
    }

    fun addImageFilter(filter: String) {
        imageFilters.add(filter)
    }

    fun addImageBrightnessPercentage(percentage: Int) {
        imageBrightnessPercentage.add(percentage)
    }

    fun getLastBitmap(): Bitmap {
        return imageVersions.last()
    }

    fun getLastFilter(): String {
        return imageFilters.last()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_editing)

        val uri = intent.getStringExtra("fileName")
        imageView = findViewById(R.id.imageView)
        selectedImageUri = Uri.parse(uri)
        imageView.setImageURI(selectedImageUri)

        val imageDrawable = imageView.drawable
        currentImage = (imageDrawable as BitmapDrawable).bitmap

        addImageVersion(currentImage)
        addImageFilter("None")
        addImageBrightnessPercentage(50)
        addImageSaturationPercentage(100)
        addImageBlurPercentage(0)
        lastImageWithoutBrightness = currentImage

        val filtersButton: Button = findViewById(R.id.filtersButton)
        val brightnessButton: Button = findViewById(R.id.brightnessButton)
        val saturationButton: Button = findViewById(R.id.saturationButton)
        val blurButton: Button = findViewById(R.id.blurButton)
        val addTextButton : Button = findViewById(R.id.addTextButton)
        val cropButton: Button = findViewById(R.id.cropButton)

        var filtersFragment: Fragment? = null
        var brightnessFragment: Fragment? = null
        var currentFragment: Fragment? = null
        var saturationFragment: Fragment? = null
        var blurFragment: Fragment? = null

        val bundle = Bundle()
        bundle.putString("imageUri", selectedImageUri.toString())

        cropButton.setOnClickListener {
            val sourceUri: Uri? = selectedImageUri// source image Uri
            val destinationFileName = "CroppedImage.jpg"
            val file = File(getExternalFilesDir(null), destinationFileName)
            val destinationUri = Uri.fromFile(file)

            val sourceUri2 = bitmapToUri(imageVersions.last(), this)

            val options = UCrop.Options()

            val uCrop = UCrop.of(sourceUri2, destinationUri!!)
                .withOptions(options)
                .getIntent(this)
            cropActivityResultLauncher.launch(uCrop)
        }

        addTextButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val inflater = layoutInflater

            builder.setTitle("Enter your text")

            val dialogLayout = inflater.inflate(R.layout.dialog_text_input, null)
            val editText = dialogLayout.findViewById<EditText>(R.id.edit_text)
            val colorPickerButton = dialogLayout.findViewById<Button>(R.id.color_picker_button)

            draggableTextView = DraggableTextView(this)

            draggableTextView.draggableClickListener = object : DraggableTextView.DraggableClickListener {
                override fun onClick(view: DraggableTextView) {
                    showUpdateDialog(view)
                }
            }

            var textColor = Color.BLACK

            colorPickerButton.setOnClickListener {
                ColorPickerDialogBuilder
                    .with(this)
                    .setTitle("Choose color")
                    .initialColor(Color.RED)
                    .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                    .density(12)
                    .setOnColorSelectedListener { selectedColor ->
                        textColor = selectedColor
                        editText.setTextColor(selectedColor)
                    }
                    .setPositiveButton("ok"
                    ) { _, selectedColor, _ ->
                        textColor = selectedColor
                        editText.setTextColor(selectedColor)
                    }
                    .setNegativeButton(
                        "cancel"
                    ) { _, _ -> }
                    .build()
                    .show()
            }

            val layout: FrameLayout = findViewById(R.id.image_text_container)
            builder.setView(dialogLayout)
            builder.setPositiveButton("OK") { _, _ ->
                val text = editText.text.toString()

                layout.post {
                    val layoutWidth = layout.width
                    val layoutHeight = layout.height

                    draggableTextView.text = text
                    draggableTextView.measure(0, 0)

                    val centerX = layoutWidth / 2 - draggableTextView.measuredWidth / 2
                    val centerY = layoutHeight / 2 - draggableTextView.measuredHeight / 2

                    val layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.setMargins(centerX, centerY, 0, 0)

                    draggableTextView.layoutParams = layoutParams
                    draggableTextView.setTextColor(textColor)
                    layout.addView(draggableTextView)
                }
            }
            builder.setNegativeButton("Cancel") { _, _ ->
                // Do nothing
            }
            builder.show()
        }

        filtersButton.setOnClickListener {
            val transaction = supportFragmentManager.beginTransaction()
            supportFragmentManager.fragments.forEach { transaction.hide(it) }
            if (filtersFragment == null) {
                filtersFragment = FiltersFragment()
                (filtersFragment as FiltersFragment).arguments = bundle
                transaction.add(R.id.fragment_container, filtersFragment!!, "FILTERS_FRAGMENT")
                currentFragment = filtersFragment
            } else {
                currentFragment = if (currentFragment == filtersFragment) {
                    transaction.hide(filtersFragment!!)
                    null
                } else {
                    supportFragmentManager.fragments.forEach { transaction.hide(it) }
                    transaction.show(filtersFragment!!)
                    filtersFragment
                }
            }

            transaction.commit()
        }

        brightnessButton.setOnClickListener {
            val transaction = supportFragmentManager.beginTransaction()
            supportFragmentManager.fragments.forEach { transaction.hide(it) }
            if (brightnessFragment == null) {
                brightnessFragment = BrightnessControlFragment()
                transaction.add(R.id.fragment_container, brightnessFragment!!, "BRIGHTNESS_FRAGMENT")
                currentFragment = brightnessFragment
            } else {
                currentFragment = if (currentFragment == brightnessFragment) {
                    transaction.hide(brightnessFragment!!)
                    null
                } else {
                    supportFragmentManager.fragments.forEach { transaction.hide(it) }
                    transaction.show(brightnessFragment!!)
                    brightnessFragment
                }
            }

            transaction.commit()
        }

        saturationButton.setOnClickListener {
            val transaction = supportFragmentManager.beginTransaction()
            supportFragmentManager.fragments.forEach { transaction.hide(it) }
            if (saturationFragment == null) {
                saturationFragment = SaturationControlFragment()
                transaction.add(R.id.fragment_container, saturationFragment!!, "SATURATION_FRAGMENT")
                currentFragment = saturationFragment
            } else {
                currentFragment = if (currentFragment == saturationFragment) {
                    transaction.hide(saturationFragment!!)
                    null
                } else {
                    supportFragmentManager.fragments.forEach { transaction.hide(it) }
                    transaction.show(saturationFragment!!)
                    saturationFragment
                }
            }

            transaction.commit()
        }

        blurButton.setOnClickListener {
            val transaction = supportFragmentManager.beginTransaction()
            supportFragmentManager.fragments.forEach { transaction.hide(it) }
            if (blurFragment == null) {
                blurFragment = BlurControlFragment()
                transaction.add(R.id.fragment_container, blurFragment!!, "BLUR_FRAGMENT")
                currentFragment = blurFragment
            } else {
                currentFragment = if (currentFragment == blurFragment) {
                    transaction.hide(blurFragment!!)
                    null
                } else {
                    supportFragmentManager.fragments.forEach { transaction.hide(it) }
                    transaction.show(blurFragment!!)
                    blurFragment
                }
            }
            transaction.commit()
        }

        val topNavigation: BottomNavigationView = findViewById(R.id.topNavigationView)
        topNavigation.menu.setGroupCheckable(0, false, true)

        topNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_save -> {
                    collectionDialog = CollectionDialogFragment()
                    collectionDialog.show(supportFragmentManager, "collectionDialog")

                    collectionDialog.setOnSaveClickListener {
                        val selectedCollection = collectionDialog.getSelectedCollection()
                        if (selectedCollection == null) {
                            Toast.makeText(this, "Please select a collection", Toast.LENGTH_LONG).show()
                        } else {
                            changesMade = true
                            val imageTextContainer = findViewById<FrameLayout>(R.id.image_text_container)
                            val hasTextViews = hasDraggableTextViews(imageTextContainer)

                            if(hasTextViews) {
                                uploadImageWithText(selectedCollection)
                            } else {
                                addImageToCollection(selectedCollection)
                            }
                        }
                    }
                }
                R.id.action_undo -> {
                    undoLastFilter()
                }
            }
            true
        }

        val callback = object : OnBackPressedCallback(true /* enabled by default */) {
            override fun handleOnBackPressed() {

                val builder = AlertDialog.Builder(this@PhotoEditingActivity, R.style.RoundedDialog)

                val inflater = layoutInflater
                val dialogView = inflater.inflate(R.layout.dialog_layout1, null)
                val dialogMessage = dialogView.findViewById<TextView>(R.id.dialogMessage)
                val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
                val noButton = dialogView.findViewById<Button>(R.id.noButton)
                noButton.visibility = View.VISIBLE
                if(intent.getStringExtra("fromCollageOrFS") == true.toString())
                    dialogMessage.text = "Do you want to override the image?"
                else {
                    dialogMessage.text = "Are you sure you want to exit?"
                    noButton.visibility = View.GONE
                }

                builder.setView(dialogView)
                val alertDialog = builder.create()

                val yesButton = dialogView.findViewById<Button>(R.id.yesButton)
                yesButton.setOnClickListener {
                    // Disable this callback and finish the activity
                    alertDialog.dismiss()
                    val bitmap: Bitmap = imageVersions.last()
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val filename = "editedImage_$timestamp.jpg"
                    val file = File(getExternalFilesDir(null), filename)

                    if (file.exists()) {
                        file.delete()
                    }

                    try {
                        val fos = FileOutputStream(file)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                        fos.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    val imageUri = Uri.fromFile(file)
                    val returnIntent = Intent()
                    returnIntent.putExtra("changes_made", changesMade)
                    returnIntent.putExtra("EDITED_IMAGE_URI", imageUri.toString())
                    setResult(Activity.RESULT_OK, returnIntent)
                    finish()

                }
                noButton.setOnClickListener {
                    alertDialog.dismiss()
                    finish()
                }
                cancelButton.setOnClickListener {
                    alertDialog.dismiss()

                }
                alertDialog.show()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun undoLastFilter() {
        if (imageVersions.size >= 2) {

            imageVersions.removeAt(imageVersions.size - 1)
            currentImage = imageVersions.last()

            imageView.setImageBitmap(currentImage)
            if(imageFilters.last() == "Brightness") {
                imageBrightnessPercentage.removeAt(imageBrightnessPercentage.size - 1)
                lastBrightnessPercentage = if(imageBrightnessPercentage.size > 1)
                    imageBrightnessPercentage.last()
                else
                    50
                val fragmentB = supportFragmentManager.findFragmentByTag("BRIGHTNESS_FRAGMENT") as BrightnessControlFragment?
                fragmentB?.updateSeekBar(lastBrightnessPercentage)
            }
            if(imageFilters.last() == "Saturation") {
                imageSaturationPercentage.removeAt(imageSaturationPercentage.size - 1)
                val imageSaturation = if(imageSaturationPercentage.size > 1)
                    imageSaturationPercentage.last()
                else
                    100
                val fragmentS = supportFragmentManager.findFragmentByTag("SATURATION_FRAGMENT") as SaturationControlFragment?
                fragmentS?.updateSeekBar(imageSaturation)
            }
            if(imageFilters.last() == "Blur") {
                imageBlurPercentage.removeAt(imageBlurPercentage.size - 1)
                val imageBlur = if(imageBlurPercentage.size > 1)
                    imageBlurPercentage.last()
                else
                    0
                val fragmentB = supportFragmentManager.findFragmentByTag("BLUR_FRAGMENT") as BlurControlFragment?
                fragmentB?.updateSeekBar(imageBlur)
            }

            imageFilters.removeAt(imageFilters.size - 1)

        } else {
            Toast.makeText(this, "No more steps to undo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bitmapToUri(bitmap: Bitmap, context: Context): Uri {
        val file = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")

        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        return Uri.fromFile(file)
    }

    private fun hasDraggableTextViews(viewGroup: ViewGroup): Boolean {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is DraggableTextView) {
                return true
            } else if (child is ViewGroup) {
                if (hasDraggableTextViews(child)) {
                    return true
                }
            }
        }
        return false
    }

    private fun addImageToCollection(selectedCollection: ImageCollection) {
        val collectionId = selectedCollection.id
        val imageView = findViewById<ImageView>(R.id.imageView)
        val bitmap = (imageView.drawable as BitmapDrawable).bitmap
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
        uploadToServer(collectionId, file, body, header)
    }

    private fun uploadImageWithText(selectedCollection: ImageCollection) {
        val imageView = findViewById<ImageView>(R.id.imageView)
        val imageTextContainer = findViewById<FrameLayout>(R.id.image_text_container)
        val imageWidth = imageView.width
        val imageHeight = imageView.height
        val bitmap = Bitmap.createBitmap(
            imageWidth,
            imageHeight,
            Bitmap.Config.ARGB_8888
        )

        val collectionId = selectedCollection.id
        val canvas = Canvas(bitmap)

        imageView.draw(canvas)
        imageTextContainer.draw(canvas)

        val file = File.createTempFile("image", null, cacheDir)
        val outputStream = FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream) // Compress the bitmap to JPEG with quality 80
        outputStream.flush()
        outputStream.close()

        val body = UploadRequestBody(file, "image")
        val sharedPreferences = applicationContext.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val jwt = sharedPreferences.getString("accessToken", null)
        val header = "Bearer $jwt"

        uploadToServer(collectionId, file, body, header)
    }

    private fun uploadToServer(collectionId: Int, file: File, body: UploadRequestBody, header: String) {
        MyApi().addImageToCollection(
            collectionId,
            MultipartBody.Part.createFormData("image",
            file.name,
            body),
            "json".toRequestBody("multipart/form-data".toMediaTypeOrNull()),
            header
        ).enqueue(object : Callback<AddImageResponse> {
            override fun onResponse(
                call: Call<AddImageResponse>,
                response: retrofit2.Response<AddImageResponse>
            ) {
                if(response.body()?.message == "Image uploaded successfully!") {
                    Toast.makeText(baseContext, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(baseContext, "Error uploading image!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AddImageResponse>, t: Throwable) {
                Toast.makeText(baseContext, "Error uploading image!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showUpdateDialog(textView: DraggableTextView) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Update your text")

        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_text_input, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.edit_text)
        val colorPickerButton = dialogLayout.findViewById<Button>(R.id.color_picker_button)
        var textColor = textView.currentTextColor

        editText.setText(textView.text)

        colorPickerButton.setOnClickListener {
            ColorPickerDialogBuilder
                .with(this)
                .setTitle("Choose color")
                .initialColor(Color.RED)
                .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                .density(12)
                .setOnColorSelectedListener { selectedColor ->

                    textColor = selectedColor
                    editText.setTextColor(selectedColor)
                }
                .setPositiveButton("Ok"
                ) { _, selectedColor, _ ->

                    textColor = selectedColor
                    editText.setTextColor(selectedColor)
                }
                .setNegativeButton(
                    "Cancel"
                ) { _, _ -> }
                .build()
                .show()
        }

        builder.setView(dialogLayout)
        builder.setPositiveButton("Ok") { _, _ ->
            val updatedText = editText.text.toString()

            textView.text = updatedText
            textView.setTextColor(textColor)
        }
        builder.setNegativeButton("Cancel") { _, _ ->
            // Do nothing
        }
        builder.setNeutralButton("Delete TextView") { _, _ ->
            val deleteBuilder = AlertDialog.Builder(this)
            deleteBuilder.setMessage("Are you sure you want to delete this TextView?")
                .setPositiveButton("Yes") { _, _ ->
                    val parent = draggableTextView.parent as ViewGroup
                    parent.removeView(draggableTextView)
                }
                .setNegativeButton("No") { deleteDialog, _ ->
                    deleteDialog.dismiss()
                }
            deleteBuilder.create().show()
        }
        builder.show()
    }
}