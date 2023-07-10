package com.example.loginregister.imageprocessing.collage

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.loginregister.R

class LayoutSelectionActivity : AppCompatActivity() {
    private lateinit var selectedLayoutName: String
    var changesMade = false

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            changesMade = result.data?.getBooleanExtra("changes_made", false) == true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MyCustomTheme)
        setContentView(R.layout.activity_layout_selection)

        val toolbar: Toolbar = findViewById(R.id.my_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Choose a collage layout"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val recyclerView = findViewById<RecyclerView>(R.id.layoutRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = CollageLayoutAdapter(CollageLayout.values().toList()) { selectedLayout ->
            selectedLayoutName = selectedLayout.name
        }

        val continueButton = findViewById<Button>(R.id.continueButton)
        continueButton.setOnClickListener {
            try {
                val intent = Intent(this, CollageActivity::class.java)
                intent.putExtra("selectedLayout", selectedLayoutName)
                activityResultLauncher.launch(intent)
            } catch (e: UninitializedPropertyAccessException) {
                Toast.makeText(this, "Please select a layout!", Toast.LENGTH_SHORT).show()
            }
        }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val returnIntent = Intent()
                returnIntent.putExtra("changes_made", changesMade)
                setResult(Activity.RESULT_OK, returnIntent)
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
}