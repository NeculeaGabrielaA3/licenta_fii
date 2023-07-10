package com.example.loginregister.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.loginregister.MainActivity
import com.example.loginregister.R
import com.example.loginregister.api.LoginRequestBody
import com.example.loginregister.api.MyApi
import com.example.loginregister.api.RequestResponse


class LoginActivity : AppCompatActivity() {

    private lateinit var backPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MyCustomTheme)
        setContentView(R.layout.activity_simple_login)

        doNotAllowBackButton()

        val loginBtn: Button = findViewById(R.id.button_login)
        val registerText: TextView = findViewById(R.id.textView_register_now)

        loginBtn.setOnClickListener {
            performLogin2()
        }

        registerText.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogin2() {
        val email: EditText = findViewById(R.id.editText_email_login)
        val password: EditText = findViewById(R.id.editText_password_login)

        if (email.text.isEmpty() || password.text.isEmpty()) {
            Toast.makeText(this,"Please fill all the fields!", Toast.LENGTH_SHORT).show()
            return
        }

        val emailInput = email.text.toString()
        val passwdInput = password.text.toString()

        val request = LoginRequestBody(emailInput, passwdInput)

        MyApi().login(request).enqueue(object : retrofit2.Callback<RequestResponse> {
            override fun onResponse(
                call: retrofit2.Call<RequestResponse>,
                response: retrofit2.Response<RequestResponse>
            ) {

                if(response.body()?.message == "User does not exist.") {
                    Toast.makeText(baseContext, "User does not exist.", Toast.LENGTH_SHORT)
                        .show()
                } else if(response.body()?.message == "Incorrect password.") {
                    Toast.makeText(baseContext, "Incorrect password.", Toast.LENGTH_SHORT)
                        .show()
                } else if(response.body()?.message == "Logged in successfully!") {
                    val accessToken = response.body()?.access_token
                    val userId = response.body()?.user_id
                    val sharedPreferences = applicationContext.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)

                    val editor = sharedPreferences.edit()
                    editor.putString("accessToken", accessToken)
                    if (userId != null) {
                        editor.putInt("userId", userId)
                    }
                    editor.apply()

                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                else {
                    Toast.makeText(baseContext, "Error Logging In!", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailure(call: retrofit2.Call<RequestResponse>, t: Throwable) {
                Toast.makeText(baseContext, "Error Logging In!", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun doNotAllowBackButton() {
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        }
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

}
