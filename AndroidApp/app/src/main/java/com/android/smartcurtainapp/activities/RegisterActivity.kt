package com.android.smartcurtainapp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.android.smartcurtainapp.R

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.etEmail)
        val passwordEditText = findViewById<EditText>(R.id.etPassword)
        val registerButton = findViewById<Button>(R.id.btnRegister)
        val LoginTextView = findViewById<TextView>(R.id.tvLogin)

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Đăng ký thành công
                            Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            // Đăng ký thất bại
                            Toast.makeText(this, "Đăng ký thất bại, hãy nhập đúng định dạng email và mật khẩu từ 6 ký tự trở lên", Toast.LENGTH_SHORT).show()
                            Log.d("ERROR", "${task.exception?.message}")
                        }
                    }
            } else {
                Toast.makeText(this, "Tài khoản và mật khẩu không được để trống", Toast.LENGTH_SHORT).show()
            }
        }

        LoginTextView.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
