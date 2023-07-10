package com.example.loginregister

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.loginregister.api.MyApi
import com.example.loginregister.api.RequestResponse
import com.example.loginregister.gallery.CollectionFragment
import com.example.loginregister.gallery.CollectionViewModel
import com.example.loginregister.gallery.FullScreenImageFragment
import com.example.loginregister.imageprocessing.collage.LayoutSelectionActivity
import com.example.loginregister.imageprocessing.editor.PhotoEditingActivity
import com.example.loginregister.login.LoginActivity
import com.github.drjacky.imagepicker.ImagePicker
import com.github.drjacky.imagepicker.constant.ImageProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import org.opencv.android.OpenCVLoader
import retrofit2.Call
import retrofit2.Callback


class MainActivity : AppCompatActivity(), FullScreenImageFragment.NavigationVisibility {

    private var selectedImageUri: Uri? = null
    private var layoutRoot: View? = null
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var backPressedCallback: OnBackPressedCallback
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var collectionViewModel: CollectionViewModel

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                selectedImageUri = it.data?.data
                val intent =
                    Intent(applicationContext, PhotoEditingActivity::class.java)
                intent.putExtra("fileName", selectedImageUri.toString())
                activityResultLauncher.launch(intent)
            }
        }

    private val activityResultLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val changesMade = result.data?.getBooleanExtra("changes_made", false)

            if (changesMade == true) {
                val existingFragment = supportFragmentManager.findFragmentByTag("gallery") as? CollectionFragment
                existingFragment?.refreshCollections()
            }
        }
    }

    override fun showNavigation() {
        bottomNavigationView.visibility = View.VISIBLE
    }

    override fun hideNavigation() {
        bottomNavigationView.visibility = View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.add_collection_button_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
            return true
        }

        when(item.itemId) {
            R.id.action_add_collection -> {
                val builder = AlertDialog.Builder(this, R.style.RoundedDialog)
                val viewInflated: View = LayoutInflater.from(this).inflate(R.layout.dialog_new_collection, null, false)
                val input = viewInflated.findViewById(R.id.collectionNameEditText) as EditText

                builder.setView(viewInflated)

                val alertDialog = builder.create()
                val yesButton = viewInflated.findViewById<Button>(R.id.yesButton)
                val noButton = viewInflated.findViewById<Button>(R.id.noButton)

                yesButton.setOnClickListener {
                    alertDialog.dismiss()
                    val collectionName = input.text.toString()
                    if(collectionName.isNotEmpty()){
                        val sharedPref = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
                        val jwt = sharedPref.getString("accessToken", null)
                        val userId = sharedPref.getInt("userId", 0)

                        if(jwt != null){
                           collectionViewModel.addCollection(userId, collectionName, "Bearer $jwt")
                        }

                    } else{
                        Toast.makeText(this, "Collection name cannot be empty!", Toast.LENGTH_SHORT).show()
                    }
                }
                noButton.setOnClickListener { alertDialog.cancel() }

                alertDialog.show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        collectionViewModel = ViewModelProvider(this)[CollectionViewModel::class.java]
        layoutRoot = findViewById(R.id.layout)

        showBottomNavigationMenu()
        showDrawerMenu()
        setupBackButtonBehaviour()
        showFragment(CollectionFragment())

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

    }

    private fun setupBackButtonBehaviour() {
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    // Do nothing
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    private fun showBottomNavigationMenu() {
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.gallery_button -> {
                    val intent = Intent(this, MainActivity::class.java)
                    activityResultLauncher.launch(intent)
                    finish()
                    true
                }
                R.id.upload_photo_button -> {

                    ImagePicker.with(this)
                        .crop()
                        .provider(ImageProvider.BOTH)
                        .createIntentFromDialog { launcher.launch(it) }
                    true
                }
                R.id.collage_button -> {
                    val intent = Intent(this, LayoutSelectionActivity::class.java)
                    activityResultLauncher.launch(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        val existingFragment = fragmentManager.findFragmentByTag("gallery")

        if (existingFragment != null) {
            transaction.show(existingFragment)
        } else {
            transaction.add(R.id.fragment_container, fragment, "gallery")
        }

        for (otherFragment in fragmentManager.fragments) {
            if (otherFragment != existingFragment) {
                transaction.hide(otherFragment)
            }
        }
        transaction.commit()
    }

    private fun showDrawerMenu() {
        drawerLayout = findViewById(R.id.drawer_layout)

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            }

            override fun onDrawerOpened(drawerView: View) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }

            override fun onDrawerClosed(drawerView: View) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }

            override fun onDrawerStateChanged(newState: Int) {
            }
        })

        drawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        val listener =
            NavigationView.OnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_profile -> {
                        val intent = Intent(this, ProfileActivity::class.java)
                        startActivity(intent)
                        drawerLayout.closeDrawers()
                    }
                    R.id.nav_setting -> {

                    }
                    R.id.logout -> {
                        performLogout()
                    }
                }
                false
            }

        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(listener)
    }

    private fun performLogout() {
        val sharedPreferences =
            applicationContext.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val jwt = sharedPreferences.getString("accessToken", null)
        val header = "Bearer $jwt"

        MyApi().logout(header).enqueue(object : Callback<RequestResponse> {
            override fun onResponse(
                call: Call<RequestResponse>,
                response: retrofit2.Response<RequestResponse>
            ) {
                val editor = sharedPreferences.edit()
                editor.remove("accessToken")
                editor.apply()

                val intent =
                    Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }

            override fun onFailure(call: Call<RequestResponse>, t: Throwable) {
                Log.d("Logout", "Failed to logout")
            }
        })
    }

    companion object {
        init {
            if (!OpenCVLoader.initDebug()) {
                Log.d("OpenCV", "OpenCV not loaded")
            } else {
                Log.d("OpenCV", "OpenCV loaded")
            }
        }
    }
}

