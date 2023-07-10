package com.example.loginregister.login

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.loginregister.R
import com.example.loginregister.api.MyApi
import com.example.loginregister.api.RequestResponse
import com.example.loginregister.api.SignupRequestBody
import com.google.android.material.snackbar.Snackbar

class RegisterActivity : AppCompatActivity() {
    private lateinit var upperCaseTextView: TextView
    private lateinit var digitTextView: TextView
    private lateinit var specialCharTextView: TextView
    private lateinit var lengthTextView: TextView
    private lateinit var password: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MyCustomTheme)
        setContentView(R.layout.activity_register)

        val loginText: TextView = findViewById(R.id.textView_login_now)
        loginText.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        upperCaseTextView = findViewById(R.id.upperCaseTextView)
        digitTextView = findViewById(R.id.digitTextView)
        specialCharTextView = findViewById(R.id.specialCharTextView)
        lengthTextView = findViewById(R.id.lengthTextView)
        password = findViewById(R.id.editText_password_register)
        password.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // no-op
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // no-op
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePasswordStrengthIndicators(s.toString())
            }
        })
        val registerBtn: Button = findViewById(R.id.button_register)

        registerBtn.setOnClickListener {

            val confirmPassword = findViewById<EditText>(R.id.editText_confirm_password_register)
            val inputPassword = password.text.toString()
            val inputConfirmPassword = confirmPassword.text.toString()

            if (inputPassword != inputConfirmPassword) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(inputPassword.length < 8 || !(inputPassword.any { it.isDigit() }) || !(inputPassword.any { !it.isLetterOrDigit() }) || !(inputPassword.any { it.isUpperCase() })){
                Toast.makeText(this, "Password does not meet the requirements", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            performSignUp()
        }
    }

    private fun performSignUp() {

        val email = findViewById<EditText>(R.id.editText_email_register).text.toString()
        val password = findViewById<EditText>(R.id.editText_password_register).text.toString()
        val firstName = findViewById<EditText>(R.id.editText_first_name).text.toString()
        val lastName = findViewById<EditText>(R.id.editText_last_name).text.toString()
        val username = findViewById<EditText>(R.id.editText_username_register).text.toString()
        val gender =
                findViewById<RadioButton>(findViewById<RadioGroup>(R.id.gender_radio_group).checkedRadioButtonId).text.toString()

        val request = SignupRequestBody(email, password, firstName, lastName, username, gender)

        MyApi().signup(request).enqueue(object : retrofit2.Callback<RequestResponse> {
            override fun onResponse(
                call: retrofit2.Call<RequestResponse>,
                response: retrofit2.Response<RequestResponse>
            ) {
                if (response.body()?.message == "User created successfully.") {
                    Toast.makeText(baseContext, "User created successfully. Please log in.", Toast.LENGTH_SHORT)
                        .show()
                    val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else if (response.body()?.message == "User already exists.") {
                    Snackbar.make(findViewById(android.R.id.content), "The email address you provided is already registered under a different account.", Snackbar.LENGTH_SHORT).show()
                }
                else if(response.body()?.message == "Invalid email address.") {
                    Snackbar.make(findViewById(android.R.id.content), "Invalid email address.", Snackbar.LENGTH_SHORT).show()
                }
                else
                {
                    Toast.makeText(baseContext, response.body()?.message, Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailure(call: retrofit2.Call<RequestResponse>, t: Throwable) {
                // handle error
                Toast.makeText(baseContext, "Error Signing up2", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    fun updatePasswordStrengthIndicators(password: String) {
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        val hasProperLength = password.length >= 8

        upperCaseTextView.setTextColor(if (hasUpperCase) Color.GREEN else Color.RED)
        digitTextView.setTextColor(if (hasDigit) Color.GREEN else Color.RED)
        specialCharTextView.setTextColor(if (hasSpecialChar) Color.GREEN else Color.RED)
        lengthTextView.setTextColor(if (hasProperLength) Color.GREEN else Color.RED)
    }

}