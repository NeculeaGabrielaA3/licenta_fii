package com.example.loginregister.imageprocessing.collage

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.loginregister.R

class ImagePickerActivity : AppCompatActivity() {

    private val pickImagesLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) {
        val intent = Intent(this, CollageActivity::class.java)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_picker)

        val pickImageButton: Button = findViewById(R.id.pickImageButton)
        pickImageButton.setOnClickListener {
                openImagePicker()
        }
    }

    private fun openImagePicker() {
        pickImagesLauncher.launch(arrayOf("image/*"))
    }
}
