package com.gameonanil.qrattendenceproject.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.gameonanil.qrattendenceproject.databinding.ActivityLoginBinding
import com.gameonanil.qrattendenceproject.ui.admin.AdminActivity
import com.gameonanil.qrattendenceproject.ui.student.StudentActivity
import com.gameonanil.qrattendenceproject.ui.teacher.TeacherActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "LoginActivity"
    }

    private lateinit var binding: ActivityLoginBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var firebaseFirestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()


        initCheckIfLoggedIn()

        binding.apply {
            btnLogin.setOnClickListener {
                if (etEmail.text.isEmpty() || etPass.text.isEmpty()) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Please enter both email and password",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                btnLogin.isEnabled = false
                progressbarLogin.isVisible = true
                val email = etEmail.text.toString().trim()
                val password = etPass.text.toString().trim()
                signInWithEmailPass(email, password)
            }

            tvForgetPassword.setOnClickListener {
                goToForgotPassword()
            }

        }


    }

    private fun initCheckIfLoggedIn() {
        if (mAuth.currentUser != null) {
            binding.progressbarLogin.isVisible = true
            val currentUser = mAuth.currentUser
            val docRef = firebaseFirestore.collection("users").document(currentUser!!.uid)

            docRef.get().addOnCompleteListener { docSnapshot ->
                if (docSnapshot.result!!.exists()) {
                    val userTypeString = docSnapshot.result!!.data!!["user_type"]
                    Log.d(TAG, "onCreate: userTYpe = $userTypeString ")
                    when (userTypeString) {
                        "admin" -> {
                            mAuth.signOut()
                            binding.progressbarLogin.isVisible = false
                        }
                        "student" -> goToStudentActivity()
                        "teacher" -> goToTeacherActivity()
                    }
                } else {
                    mAuth.signOut()
                    binding.progressbarLogin.isVisible = false
                    Log.d(TAG, "onCreate: ELSE(DOESNT EXIST) CALLED")
                }
            }
        }
    }

    private fun goToForgotPassword() {
        val intent = Intent(this, ForgetPasswordActivity::class.java)
        startActivity(intent)
    }

    private fun signInWithEmailPass(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val currentUser = task.result?.user
                val docRef = firebaseFirestore.collection("users").document(currentUser!!.uid)

                docRef.get().addOnSuccessListener { docSnapshot ->
                    val userTypeString = docSnapshot.data!!["user_type"]
                    Log.d(TAG, "onCreate: userTYpe = $userTypeString ")
                    when (userTypeString) {
                        "admin" -> goToAdminActivity()
                        "student" -> goToStudentActivity()
                        "teacher" -> goToTeacherActivity()

                    }

                }.addOnFailureListener {
                    binding.btnLogin.isEnabled = true
                    binding.progressbarLogin.isVisible = false
                    Toast.makeText(this@LoginActivity, "Error: ${it.message}", Toast.LENGTH_LONG)
                        .show()
                }
            } else {
                binding.btnLogin.isEnabled = true
                binding.progressbarLogin.isVisible = false
                Toast.makeText(
                    this@LoginActivity,
                    "Failed to login: ${task.exception}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun goToAdminActivity() {
        binding.btnLogin.isEnabled = true
        binding.progressbarLogin.isVisible = false
        val intent = Intent(this, AdminActivity::class.java)
        intent.putExtra("email", binding.etEmail.text.toString())
        intent.putExtra("password", binding.etPass.text.toString())
        startActivity(intent)
        finish()
    }

    private fun goToTeacherActivity() {
        binding.btnLogin.isEnabled = true
        binding.progressbarLogin.isVisible = false
        val intent = Intent(this, TeacherActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun goToStudentActivity() {
        binding.btnLogin.isEnabled = true
        binding.progressbarLogin.isVisible = false
        val intent = Intent(this, StudentActivity::class.java)
        startActivity(intent)
        finish()
    }
}