package com.gameonanil.qrattendenceproject.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gameonanil.qrattendenceproject.databinding.AttendanceListBinding
import com.gameonanil.qrattendenceproject.model.Student


class AttendanceAdapter(
    private val context: Context,
    private val userList: MutableList<Student>,
    private val listener: OnAttendanceClickListener
) :
    RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = AttendanceListBinding.inflate(LayoutInflater.from(context), parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        holder.bindTo(userList[position])
    }

    override fun getItemCount() = userList.size
    inner class AttendanceViewHolder(private val binding: AttendanceListBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
        }

        fun bindTo(user: Student) {
            binding.apply {
                tvUserName.text = user.username.toString()
            }
            binding.editList.setOnClickListener {
                val position = adapterPosition
                if (position !=RecyclerView.NO_POSITION){
                    listener.handleDeleteClicked(position)
                }
            }
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            val user = userList[position]
            if (position !=RecyclerView.NO_POSITION){
                listener.handleItemClicked(position,user)
            }
        }

    }

    interface OnAttendanceClickListener {
        fun handleItemClicked(position: Int, user: Student)
        fun handleDeleteClicked(position: Int)

    }
}