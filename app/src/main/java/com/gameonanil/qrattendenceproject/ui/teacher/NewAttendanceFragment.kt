package com.gameonanil.qrattendenceproject.ui.teacher

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.gameonanil.qrattendenceproject.R
import com.gameonanil.qrattendenceproject.adapter.NewAttendanceAdapter
import com.gameonanil.qrattendenceproject.databinding.FragmentNewAttendanceBinding
import com.gameonanil.qrattendenceproject.model.Student
import com.gameonanil.qrattendenceproject.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.apache.poi.hssf.usermodel.HSSFCellStyle
import org.apache.poi.hssf.usermodel.HSSFFont
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*

class NewAttendanceFragment : Fragment(), NewAttendanceAdapter.OnAttendanceClickListener {
    companion object {
        private const val TAG = "NewAttendanceFragment"
    }


    private var _binding: FragmentNewAttendanceBinding? = null
    private val binding get() = _binding!!

    private lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var adapter: NewAttendanceAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var attendanceList: MutableList<Student>
    private lateinit var teacherId: String
    private lateinit var dateText: String
    private lateinit var subjectText: String
    private var cell: Cell? = null
    private var row: Row? = null
    private lateinit var defaultStyle: CellStyle
    private lateinit var currentDate: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewAttendanceBinding.inflate(inflater, container, false)

        /**Setting Up Toolbar*/
        val navHostFragment = NavHostFragment.findNavController(this);
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.mainTeacherFragment,
            )
        )
        NavigationUI.setupWithNavController(
            binding.toolbarNewAttendance,
            navHostFragment,
            appBarConfiguration
        )


        /** TO USE OPTIONS MENU*/
        setHasOptionsMenu(true)
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbarNewAttendance)
        binding.toolbarNewAttendance.setNavigationOnClickListener {
            NavHostFragment.findNavController(this).navigateUp()
        }



        attendanceList = mutableListOf()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        adapter = NewAttendanceAdapter(requireActivity(), attendanceList, this)
        teacherId = auth.currentUser!!.uid

        subjectText = NewAttendanceFragmentArgs.fromBundle(requireArguments()).subjectText
        dateText = NewAttendanceFragmentArgs.fromBundle(requireArguments()).dateText
        getDataFromDb(dateText)
        currentDate = dateText



        binding.apply {
            recyclerNewList.adapter = adapter

            buttonSave.setOnClickListener {
                if (attendanceList.isNotEmpty()) {
                    handleDownloadAttendance()
                } else {
                    Toast.makeText(requireContext(), "Attendance Empty", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "onCreateView: attendance list empty!!!!!!!")
                }
            }


        }

        return binding.root
    }

    private fun getDataFromDb(dateText: String) {
        if (dateText.isNotEmpty()) {
            binding.toolbarText.text = "Attendance at : $dateText"
        }

        val collection = firestore
            .collection("attendance")
            .document("$teacherId,$subjectText,$dateText")
            .collection("student_list")

        collection.addSnapshotListener { snapshot, exception ->
            if (exception != null || snapshot == null) {
                Log.e(TAG, "onCreate: Exception: $exception")
                return@addSnapshotListener
            }
            val userFromDb = snapshot.toObjects(Student::class.java)

            attendanceList.clear()
            attendanceList.addAll(userFromDb)
            attendanceList.sortBy { it.roll!! }
            adapter.notifyDataSetChanged()

        }
    }


    override fun handleItemClicked(position: Int, user: Student) {
        val currentUser = attendanceList[position]
        val action =
            NewAttendanceFragmentDirections.actionNewAttendanceToStudentsDetailFragment(currentUser,subjectText)
        findNavController().navigate(action)

    }

    override fun handleDeleteClicked(position: Int) {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setMessage("Are you sure?")
            .setNegativeButton("No") { dialog, which ->
                Log.d(TAG, "handleDeleteClicked: ")
            }
            .setPositiveButton("Yes") { dialog, listener ->

                val collection = firestore
                    .collection("attendance")
                    .document("$teacherId,$subjectText,$dateText")
                    .collection("student_list")

                val currentUid = attendanceList[position].uid.toString().trim()

                collection
                    .document(currentUid)
                    .delete()
                    .addOnSuccessListener {
                        adapter.notifyDataSetChanged()
                        Log.d(TAG, "DocumentSnapshot successfully deleted!")
                        Toast.makeText(
                            requireActivity(),
                            "Student Deleted Successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        decreaseTotalAttendance(currentUid)
                    }.addOnFailureListener {
                        Log.d(TAG, "handleDeleteClicked: ERROR: ${it.message.toString()}")
                    }
            }
        builder.create().show()

    }

    private fun handleDownloadAttendance() {
        val wb = HSSFWorkbook()
        val sheet = wb.createSheet()

        val defaultFont: HSSFFont = wb.createFont()
        defaultFont.fontHeightInPoints = 10.toShort()
        defaultFont.fontName = "Arial"
        defaultFont.color = IndexedColors.BLACK.index
        defaultFont.boldweight = HSSFFont.BOLDWEIGHT_BOLD

        val cellStyle1: CellStyle = wb.createCellStyle()
        cellStyle1.fillForegroundColor = HSSFColor.AQUA.index
        cellStyle1.fillPattern = HSSFCellStyle.SOLID_FOREGROUND
        cellStyle1.alignment = CellStyle.ALIGN_CENTER
        cellStyle1.wrapText
        cellStyle1.setFont(defaultFont)

        val cellStyle2: CellStyle = wb.createCellStyle()
        cellStyle2.alignment = CellStyle.ALIGN_CENTER
        cellStyle2.wrapText

        row = sheet.createRow(0)
        defaultStyle = cellStyle1
        cell = row!!.createCell(0);
        cell?.setCellValue("Roll")
        cell?.cellStyle = defaultStyle

        cell = row!!.createCell(1);
        cell?.setCellValue("Student Name")
        cell?.cellStyle = defaultStyle

        cell = row!!.createCell(2);
        cell?.setCellValue("Phone")
        cell?.cellStyle = defaultStyle

        cell = row!!.createCell(3);
        cell?.setCellValue("Email")
        cell?.cellStyle = defaultStyle

        defaultStyle = cellStyle2
        for (i in attendanceList.indices) {
            row = sheet.createRow(i + 1)

            cell = row!!.createCell(0);
            cell?.setCellValue(attendanceList[i].roll.toString())
            cell?.cellStyle = defaultStyle

            cell = row!!.createCell(1);
            cell?.setCellValue(attendanceList[i].username)
            cell?.cellStyle = defaultStyle

            cell = row!!.createCell(2);
            cell?.setCellValue(attendanceList[i].phone)
            cell?.cellStyle = defaultStyle

            cell = row!!.createCell(3);
            cell?.setCellValue(attendanceList[i].email)
            cell?.cellStyle = defaultStyle


        }
        sheet.setColumnWidth(0, 3600)
        sheet.setColumnWidth(1, 5500)
        sheet.setColumnWidth(2, 3600)
        sheet.setColumnWidth(3, 8500)

        val fos: OutputStream
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d(TAG, "createExcel: new way called")
                val resolver = requireActivity().applicationContext.contentResolver
                val contentValues = ContentValues()
                contentValues.put(
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    "Attendance_$currentDate.xls"
                )
                contentValues.put(
                    MediaStore.Files.FileColumns.MIME_TYPE,
                    "application/vnd.ms-excel"
                )
                contentValues.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOCUMENTS + File.separator + "Attendance"
                )
                val excelUri = resolver.insert(
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    contentValues
                )
                fos = resolver.openOutputStream(Objects.requireNonNull(excelUri)!!)!!
                wb.write(fos)
                Toast.makeText(requireContext(), "Excel File Downloaded", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "handleDownloadAttendance: document saved sdk Q")
            } else {

                val file = File(
                    requireActivity().applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                    "Attendance_$currentDate.xls"
                )
                val outputStream = FileOutputStream(file)
                wb.write(outputStream)

                Log.d(TAG, "createExcel: Old way called")
                Toast.makeText(requireContext(), "Excel File Downloaded", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (io: IOException) {
            Toast.makeText(requireContext(), "Error:${io.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun decreaseTotalAttendance(studentId: String) {
        Log.d(TAG, "decreaseTotalAttendance: deletetotalatt called!!!")
        val studentDocRef = firestore.collection("attendance_count")
            .document("$studentId,$subjectText")
        Log.d(TAG, "increaseTotalAttendance: docRef=${studentDocRef.path}")

        studentDocRef.get().addOnCompleteListener { docSnapshot ->
            /** When student subject attendance count exists**/
            if (docSnapshot.result!!.exists()) {
                Log.d(TAG, "decreaseTotalAttendance:  EXIST")
                val totalAttendance =
                    docSnapshot.result!!["total_attendance"].toString().toInt()
                if (totalAttendance > 0) {
                    studentDocRef.update("total_attendance", FieldValue.increment(-1))
                    Log.d(TAG, "decreaseTotalAttendance: UPDATED!!!!!!!")
                }

            } else {
                Log.d(TAG, "increaseTotalAttendance: Error:Document Doesnt Exixt")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_logout, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.itemLogout) {
            Log.d(TAG, "onOptionsItemSelected: logout pressed")

            FirebaseAuth.getInstance().signOut()
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}