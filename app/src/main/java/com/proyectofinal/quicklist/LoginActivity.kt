package com.proyectofinal.quicklist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Patterns

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializamos Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)

        // REGISTRO
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        saveUserToFirestore()

                        Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show()

                        startActivity(Intent(this, TopicsActivity::class.java))
                        finish()

                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // LOGIN
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()

            if (email.isBlank()) {
                emailEditText.error = "Introduce tu correo electrónico"
                emailEditText.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEditText.error = "Introduce un correo electrónico válido"
                emailEditText.requestFocus()
                return@setOnClickListener
            }

            if (password.isBlank()) {
                passwordEditText.error = "Introduce tu contraseña"
                passwordEditText.requestFocus()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        Toast.makeText(this, "Inicio de sesión correcto", Toast.LENGTH_SHORT).show()

                        startActivity(Intent(this, TopicsActivity::class.java))
                        finish()

                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    // 👇 ESTA FUNCIÓN VA FUERA DE onCreate
    private fun saveUserToFirestore() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val db = FirebaseFirestore.getInstance()

        val userData = hashMapOf(
            "email" to user.email
        )

        db.collection("users")
            .document(user.uid)
            .set(userData)
    }
}
