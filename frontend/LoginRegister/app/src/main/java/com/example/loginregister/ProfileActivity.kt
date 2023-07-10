package com.example.loginregister

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.example.loginregister.api.*
import com.example.loginregister.login.LoginActivity
import com.github.drjacky.imagepicker.ImagePicker
import com.github.drjacky.imagepicker.constant.ImageProvider
import okhttp3.MultipartBody
import java.io.File

class ProfileActivity : AppCompatActivity() {
    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { it ->
            if (it.resultCode == Activity.RESULT_OK) {
                val selectedImageUri = it.data?.data
                Glide.with(this)
                    .load(selectedImageUri)
                    .into(profilePicture)

                val file = selectedImageUri?.path?.let { File(it) }
                val requestFile = file?.let { UploadRequestBody(it, "image") }
                val body = requestFile?.let { MultipartBody.Part.createFormData("image", file.name, it) }

                if (body != null) {
                    MyApi().addImageProfile(body, header).enqueue(object :
                        retrofit2.Callback<RequestResponse> {
                        override fun onResponse(
                            call: retrofit2.Call<RequestResponse>,
                            response: retrofit2.Response<RequestResponse>
                        ) {
                            if (response.isSuccessful) {
                                Log.d("GalleryViewModel", "Successfully added image")
                            } else {
                                Log.d("GalleryViewModel", "Failed to add image")
                            }
                        }

                        override fun onFailure(call: retrofit2.Call<RequestResponse>, t: Throwable) {
                            Log.d("GalleryViewModel", "Error: ${t.message}")
                        }
                    })
                }
            }
        }

    private lateinit var addImageButton: ImageButton
    private lateinit var profilePicture: ImageView
    private lateinit var logoutButton: Button
    private lateinit var deleteAccount: TextView
    private lateinit var emailTextView: TextView
    private lateinit var genderTextView: TextView
    private lateinit var usernameTextView: TextView
    private lateinit var firstNameTextView: TextView
    private lateinit var lastNameTextView: TextView

    private lateinit var infoUser: UserResponse
    private lateinit var header: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        fetchInformation()

        addImageButton = findViewById(R.id.changeProfilePictureButton)
        profilePicture = findViewById(R.id.profilePicture)

        emailTextView = findViewById(R.id.email)
        genderTextView = findViewById(R.id.gender)
        usernameTextView = findViewById(R.id.username)
        firstNameTextView = findViewById(R.id.firstname)
        lastNameTextView = findViewById(R.id.lastname)
        deleteAccount = findViewById(R.id.delete)

        addImageButton.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .provider(ImageProvider.BOTH)
                .createIntentFromDialog { launcher.launch(it) }
        }

        logoutButton = findViewById(R.id.logout)
        logoutButton.setOnClickListener {
            performLogout()
        }

        deleteAccount.setOnClickListener {
            val builder = AlertDialog.Builder(this, R.style.RoundedDialog)

            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_layout_delete, null)
            builder.setView(dialogView)
            val alertDialog = builder.create()

            val yesButton = dialogView.findViewById<Button>(R.id.yesButton)
            yesButton.setOnClickListener {
                alertDialog.dismiss()
                deleteAccount()
            }

            val noButton = dialogView.findViewById<Button>(R.id.noButton)
            noButton.setOnClickListener {
                alertDialog.dismiss()
            }

            alertDialog.show()
        }
    }

    private fun deleteAccount(){
        val sharedPreferences =
            applicationContext.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        MyApi().deleteUser(header).enqueue(object : retrofit2.Callback<RequestResponse> {
            override fun onResponse(
                call: retrofit2.Call<RequestResponse>,
                response: retrofit2.Response<RequestResponse>
            ) {
                if(response.body()?.message  == "User has been deleted successfully") {
                    Log.d("ProfileActivity", "Successfully deleted account")
                    val editor = sharedPreferences.edit()
                    editor.remove("accessToken")
                    editor.apply()

                    val intent =
                        Intent(applicationContext, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else
                    Log.d("ProfileActivity", "Failed to delete account")
            }

            override fun onFailure(call: retrofit2.Call<RequestResponse>, t: Throwable) {
                Log.d("ProfileActivity", "Failed to delete account")
            }
        })
    }

    private fun fetchInformation() {
        val sharedPreferences =
            applicationContext.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val jwt = sharedPreferences.getString("accessToken", null)
        header = "Bearer $jwt"

        MyApi().getUserInfo(header).enqueue(object : retrofit2.Callback<UserResponse> {
            @SuppressLint("SetTextI18n")
            override fun onResponse(
                call: retrofit2.Call<UserResponse>,
                response: retrofit2.Response<UserResponse>
            ) {
                infoUser = response.body()!!
                emailTextView.text = "Email: ${infoUser.email}"
                genderTextView.text = "Gender: ${infoUser.gender}"
                firstNameTextView.text = infoUser.first_name
                lastNameTextView.text = infoUser.last_name
                usernameTextView.text = infoUser.username

                Glide.with(this@ProfileActivity)
                    .load(infoUser.profile_picture)
                    .into(profilePicture)

            }

            override fun onFailure(call: retrofit2.Call<UserResponse>, t: Throwable) {
                Log.d("Logout", "Failed to get in information")
            }
        })
    }

    private fun performLogout() {
        val sharedPreferences =
            applicationContext.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val jwt = sharedPreferences.getString("accessToken", null)
        val header = "Bearer $jwt"

        MyApi().logout(header).enqueue(object : retrofit2.Callback<RequestResponse> {
            override fun onResponse(
                call: retrofit2.Call<RequestResponse>,
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

            override fun onFailure(call: retrofit2.Call<RequestResponse>, t: Throwable) {
                Log.d("Logout", "Failed to logout")
            }
        })
    }
}