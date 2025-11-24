package com.proyectofinal.quicklist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private var taskList: List<Task>,
    private val onStatusChange: (Task, String) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val taskText: TextView = view.findViewById(R.id.taskTextView)
        val chipPending: TextView = view.findViewById(R.id.chipPending)
        val chipInProgress: TextView = view.findViewById(R.id.chipInProgress)
        val chipDone: TextView = view.findViewById(R.id.chipDone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun getItemCount() = taskList.size

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.taskText.text = task.text

        // Seleccionar chip según estado actual
        highlightChip(holder, task.status)

        // Cambiar estado
        holder.chipPending.setOnClickListener {
            onStatusChange(task, "pending")
        }
        holder.chipInProgress.setOnClickListener {
            onStatusChange(task, "in_progress")
        }
        holder.chipDone.setOnClickListener {
            onStatusChange(task, "done")
        }
    }

    private fun highlightChip(holder: TaskViewHolder, status: String) {
        holder.chipPending.alpha = if (status == "pending") 1f else 0.4f
        holder.chipInProgress.alpha = if (status == "in_progress") 1f else 0.4f
        holder.chipDone.alpha = if (status == "done") 1f else 0.4f
    }

    fun updateList(newList: List<Task>) {
        taskList = newList
        notifyDataSetChanged()
    }
}

