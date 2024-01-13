package com.example.location

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.location.models.User
import com.example.location.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var user: User
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        user=User()
        auth= FirebaseAuth.getInstance()
        firestore= FirebaseFirestore.getInstance()

        binding.btnCreateAccount.setOnClickListener{
                val email = binding.editTextEmail.text.toString().trim()
                val password = binding.editTextPassword.text.toString().trim()

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Incomplete Details", Toast.LENGTH_SHORT).show()
                }else {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                user.apply {
                                    this.email = email
                                    this.password = password
                                }
                                val userId = auth.currentUser?.uid

                                if (userId != null) {
                                    firestore.collection("user")
                                        .document(userId).set(user)
                                        .addOnCompleteListener {
                                            startActivity(Intent(this@SignUpActivity, HomeActivity::class.java))
                                            finish()
                                        }
                                } else {
                                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        }
                }
        }
        binding.btnAlreadyHaveAccount.setOnClickListener {
            startActivity(Intent(this@SignUpActivity,LoginActivity::class.java))
        }
    }
}