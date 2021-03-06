package com.gameonanil.qrattendenceproject.ui.student

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.view.isVisible
import com.gameonanil.qrattendenceproject.R
import com.gameonanil.qrattendenceproject.databinding.ActivityStudentBinding
import com.gameonanil.qrattendenceproject.model.Student
import com.gameonanil.qrattendenceproject.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.integration.android.IntentIntegrator
import java.util.*


class StudentActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "StudentActivity"
    }

    private lateinit var auth: FirebaseAuth

    private lateinit var firestore: FirebaseFirestore
    private lateinit var collectionRef: CollectionReference
    private lateinit var currentUid: String
    private lateinit var binding: ActivityStudentBinding
    private lateinit var subjectText: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarStudent)
        supportActionBar!!.setDisplayShowTitleEnabled(false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        collectionRef = firestore.collection("attendance")
        currentUid = auth.currentUser!!.uid
        subjectText = ""


        binding.apply {
            buttonScan.setOnClickListener {
                val scanner = IntentIntegrator(this@StudentActivity)
                scanner.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                scanner.setPrompt("Scan a barcode")
                scanner.initiateScan()
            }
        }
        binding.progressbarStudent.isVisible = true



    }

    override fun onStart() {
        super.onStart()
        displayStudentDetail()
    }

    private fun displayStudentDetail() {
        val userDocRef = firestore.collection("users").document(currentUid)
        userDocRef.get().addOnSuccessListener { docSnapshot ->
            if (docSnapshot.exists()) {
                val userDetail = docSnapshot.toObject(Student::class.java)
                if (userDetail != null) {
                    userDetail.username?.let { binding.tvUserName.text = it }
                    userDetail.email?.let { binding.tvEmail.text = it }
                    userDetail.roll?.let { binding.tvRoll.text = it.toString() }
                    userDetail.address?.let { binding.tvAddress.text = it }
                    userDetail.phone?.let { binding.tvPhone.text = it }
                }
            }

            binding.progressbarStudent.isVisible = false

        }.addOnFailureListener {
            Toast.makeText(this, "Error:${it.message}", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "displayStudentDetail: Error:${it.message}")
            binding.progressbarStudent.isVisible = false
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (result != null) {
                if (result.contents == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
                } else {
                    Log.d(TAG, "onActivityResult: Scanned:${result.contents}")
                    val teacherIdPlusSem = result.contents.toString()
                    val  subjectAndDate = teacherIdPlusSem.substringAfter("/")
                    subjectText = subjectAndDate.substringBefore("/")
                    val finalDate = subjectAndDate.substringAfter("/")
                    val finalTeacherId = teacherIdPlusSem.substringBefore("/")
                    Log.d(TAG, "onActivityResult: !!!!!!!!!!teacherId=$finalTeacherId,")
                    Log.d(TAG, "onActivityResult: !!!!!!!!!!date=$finalDate,")
                    Log.d(TAG, "onActivityResult: !!!!!!!!!!subject=$subjectText,")
                    checkAccess(finalTeacherId, subjectText, finalDate)

                }
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    private fun checkAccess(teacherId: String, subjectText: String, date: String) {
        val accessDocReference = firestore
            .collection("attendance")
            .document("$teacherId,$subjectText,$date")
            .collection("access")
            .document(teacherId)

      accessDocReference.get().addOnSuccessListener { documentSnapnot ->
            if (documentSnapnot.exists()) {
                val accessCheck = documentSnapnot["access_allowed"]
                Log.d(TAG, "checkAccess: checkAcces=${accessCheck}")
                if (accessCheck == true) {
                    addStudentToDb(teacherId, subjectText, date)
                } else {
                    Toast.makeText(
                        this,
                        "Access Denied Please Contact Your Teacher.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Log.d(TAG, "checkAccess: Document Doesnt exixt!!!")
            }

        }
    }

    private fun addStudentToDb(teacherId: String, subjectText: String, date: String) {
        firestore.collection("users").document(currentUid).get().addOnSuccessListener {
            val userdata = it.toObject(Student::class.java)

            val docRef = collectionRef
                .document("$teacherId,$subjectText,$date")
                .collection("student_list")
                .document(currentUid)

            docRef.get().addOnCompleteListener { documentSnapshot ->
                if (documentSnapshot.result!!.exists()) {
                    Toast.makeText(this, "Student Already Added", Toast.LENGTH_SHORT).show()
                } else {
                    userdata?.let {
                        docRef.set(userdata).addOnSuccessListener {
                            Toast.makeText(
                                this@StudentActivity,
                                "Student added successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.d(TAG, "addStudentToDb: ADD CALLED!!!!!!!!!")
                            increaseTotalAttendance(teacherId, subjectText)
                        }.addOnFailureListener {
                            Toast.makeText(this, "Error:${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

        }

    }

    private fun increaseTotalAttendance(teacherId: String, subjectText: String) {
        val studentDocRef = firestore.collection("attendance_count")
            .document("$currentUid,$subjectText")

        Log.d(TAG, "increaseTotalAttendance: docRef=${studentDocRef.path}")

        studentDocRef.get().addOnCompleteListener { docSnapshot ->
            /** When student subject attendance count exists**/
            if (docSnapshot.result!!.exists()) {
                studentDocRef
                    .update("total_attendance", FieldValue.increment(1))
                    .addOnSuccessListener {
                        Log.d(TAG, "increaseTotalAttendance: Totalattendance updated")
                    }
                    .addOnFailureListener {
                        Log.d(
                            TAG,
                            "increaseTotalAttendance: totalattendance not updated:${it.message}"
                        )
                    }
            }
            /** When student subject attendance count Doesn't exists**/
            else {
                val attendanceHashMap = hashMapOf<String, Int>("total_attendance" to 1)
                studentDocRef.set(attendanceHashMap).addOnSuccessListener {
                    Log.d(TAG, "increaseTotalAttendance: new created")
                }.addOnFailureListener {
                    Log.d(TAG, "increaseTotalAttendance: Error:${it.message}")
                }

            }
        }


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        MenuInflater(this).inflate(R.menu.menu_detail, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.itemLogout -> {
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
                return true
            }
            R.id.item_edit -> {
                val intent = Intent(this, EditStudentActivity::class.java)
                intent.putExtra("studentUid", currentUid.toString())
                startActivity(intent)
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }
}