package com.proyectofinal.quicklist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip


class TaskAdapter(
    private var tasks: List<Task>,
    private val onStatusChange: (Task, String) -> Unit,
    private val onEdit: (Task) -> Unit,
    private val onDelete: (Task) -> Unit,
    private val isAdminProvider: () -> Boolean,
    private val onUploadEvidence: (Task) -> Unit,
    private val onApproveEvidence: (Task) -> Unit,
    private val onViewEvidence: (Task) -> Unit,
    private val onRejectEvidence: (Task) -> Unit
)
 : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val taskText: TextView = view.findViewById(R.id.taskText)
        val taskDate: TextView = view.findViewById(R.id.taskDate)

        val chipPending: Chip = view.findViewById(R.id.chipPending)
        val chipInProgress: Chip = view.findViewById(R.id.chipInProgress)
        val chipDone: Chip = view.findViewById(R.id.chipDone)

        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)

        val taskCompletedBy: TextView = view.findViewById(R.id.taskCompletedBy)

        val evidenceInfo: TextView = view.findViewById(R.id.evidenceInfo)

        val uploadEvidenceButton: Button =
            view.findViewById(R.id.uploadEvidenceButton)

        val approveEvidenceButton: Button =
            view.findViewById(R.id.approveEvidenceButton)

        val viewEvidenceButton: Button = view.findViewById(R.id.viewEvidenceButton)
        val rejectEvidenceButton: Button = view.findViewById(R.id.rejectEvidenceButton)


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun getItemCount(): Int = tasks.size

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        val isAdmin = isAdminProvider()
        holder.taskText.text = task.text
        holder.itemView.alpha = 1f

        if (task.requiresEvidence && task.approved) {

            holder.evidenceInfo.visibility = View.VISIBLE
            holder.evidenceInfo.text = "✅ Evidencia aprobada"
            holder.evidenceInfo.setTextColor(Color.parseColor("#22C55E"))

        }
        else if (task.requiresEvidence && task.evidenceRejected) {

            holder.evidenceInfo.visibility = View.VISIBLE
            holder.evidenceInfo.text = "❌ Evidencia rechazada · sube otra"
            holder.evidenceInfo.setTextColor(Color.parseColor("#C026D3"))

        }
        else if (task.requiresEvidence && task.evidenceUrl.isNotBlank()) {

            holder.evidenceInfo.visibility = View.VISIBLE
            holder.evidenceInfo.text = "⏳ Evidencia enviada · pendiente de revisión"
            holder.evidenceInfo.setTextColor(Color.parseColor("#F59E0B"))

        }
        else if (task.requiresEvidence) {

            holder.evidenceInfo.visibility = View.VISIBLE
            holder.evidenceInfo.text = "📎 Requiere evidencia"
            holder.evidenceInfo.setTextColor(Color.parseColor("#6B7280"))

        }
        else {

            holder.evidenceInfo.visibility = View.GONE
        }
        holder.uploadEvidenceButton.visibility = View.GONE

        if (
            task.requiresEvidence &&
            task.evidenceUrl.isBlank() &&
            task.status != "done"
        ) {
            holder.uploadEvidenceButton.visibility = View.VISIBLE
        }

        holder.uploadEvidenceButton.setOnClickListener {
            onUploadEvidence(task)
        }
        holder.approveEvidenceButton.visibility = View.GONE

        if (
            isAdmin &&
            task.requiresEvidence &&
            task.evidenceUrl.isNotBlank() &&
            !task.approved
        ) {
            holder.approveEvidenceButton.visibility = View.VISIBLE
        }

        holder.approveEvidenceButton.setOnClickListener {
            onApproveEvidence(task)
        }

        holder.viewEvidenceButton.visibility = View.GONE
        holder.rejectEvidenceButton.visibility = View.GONE

        if (
            isAdmin &&
            task.requiresEvidence &&
            task.evidenceUrl.isNotBlank() &&
            !task.approved
        ) {
            holder.viewEvidenceButton.visibility = View.VISIBLE
            holder.approveEvidenceButton.visibility = View.VISIBLE
            holder.rejectEvidenceButton.visibility = View.VISIBLE
        }

        holder.viewEvidenceButton.setOnClickListener {
            onViewEvidence(task)
        }

        holder.rejectEvidenceButton.setOnClickListener {
            onRejectEvidence(task)
        }

        val formatter = java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale.getDefault())
        val today = java.util.Calendar.getInstance()

        if (task.status == "done") {
            holder.taskCompletedBy.visibility = View.VISIBLE
            holder.taskCompletedBy.text = "Terminada por ${task.completedByEmail}"

            holder.chipPending.isEnabled = false
            holder.chipInProgress.isEnabled = false
            holder.chipDone.isEnabled = false
        } else {
            holder.taskCompletedBy.visibility = View.GONE
            holder.taskCompletedBy.text = ""

            holder.chipPending.isEnabled = true
            holder.chipInProgress.isEnabled = true
            holder.chipDone.isEnabled = true
        }

        if (task.dueDate.isBlank()) {
            holder.taskDate.text = ""
        } else {
            try {
                val dueDateParsed = formatter.parse(task.dueDate)

                if (dueDateParsed != null) {
                    val dueCalendar = java.util.Calendar.getInstance()
                    dueCalendar.time = dueDateParsed

                    val diffMillis = dueCalendar.timeInMillis - today.timeInMillis
                    val diffDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMillis)

                    when {
                        diffDays < 0 -> {
                            holder.taskDate.text = "Vencida · ${task.dueDate}"
                            holder.taskDate.setTextColor(android.graphics.Color.parseColor("#C026D3"))
                            holder.itemView.alpha = 0.85f
                        }

                        diffDays in 0..2 -> {
                            holder.taskDate.text = "Próxima · ${task.dueDate}"
                            holder.taskDate.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
                            holder.itemView.alpha = 1f
                        }

                        else -> {
                            holder.taskDate.text = "Fecha límite · ${task.dueDate}"
                            holder.taskDate.setTextColor(android.graphics.Color.parseColor("#6B7280"))
                            holder.itemView.alpha = 1f
                        }
                    }
                } else {
                    holder.taskDate.text = "Fecha límite · ${task.dueDate}"
                    holder.taskDate.setTextColor(android.graphics.Color.parseColor("#6B7280"))
                    holder.itemView.alpha = 1f
                }

            } catch (e: Exception) {
                holder.taskDate.text = "Fecha límite · ${task.dueDate}"
                holder.taskDate.setTextColor(android.graphics.Color.parseColor("#6B7280"))
                holder.itemView.alpha = 1f
            }

        }

        holder.chipPending.isChecked = false
        holder.chipInProgress.isChecked = false
        holder.chipDone.isChecked = false

        when (task.status) {
            "pending" -> holder.chipPending.isChecked = true
            "in_progress" -> holder.chipInProgress.isChecked = true
            "done" -> holder.chipDone.isChecked = true
        }

        holder.chipPending.setOnClickListener {
            onStatusChange(task, "pending")
        }

        holder.chipInProgress.setOnClickListener {
            onStatusChange(task, "in_progress")
        }

        holder.chipDone.setOnClickListener {
            if (task.requiresEvidence && !task.approved) {
                Toast.makeText(
                    holder.itemView.context,
                    "La evidencia debe ser aprobada por el admin",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            onStatusChange(task, "done")
        }

        holder.btnEdit.visibility =
            if (isAdmin) View.VISIBLE else View.GONE

        holder.btnDelete.visibility =
            if (isAdmin) View.VISIBLE else View.GONE

        holder.btnEdit.isEnabled = isAdmin
        holder.btnDelete.isEnabled = isAdmin
        holder.btnEdit.isClickable = isAdmin
        holder.btnDelete.isClickable = isAdmin

        holder.btnEdit.setOnClickListener {
            Toast.makeText(
                holder.itemView.context,
                "Editar pulsado",
                Toast.LENGTH_SHORT
            ).show()

            onEdit(task)
        }

        holder.btnDelete.setOnClickListener {
            Toast.makeText(
                holder.itemView.context,
                "Borrar pulsado",
                Toast.LENGTH_SHORT
            ).show()

            onDelete(task)
        }




    }

    fun updateList(newTasks: List<Task>) {
        val formatter = java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale.getDefault())
        val today = java.util.Calendar.getInstance()

        tasks = newTasks.sortedWith(compareBy { task ->
            try {
                val date = formatter.parse(task.dueDate)

                if (date != null) {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = date

                    val diffMillis = cal.timeInMillis - today.timeInMillis
                    val diffDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMillis)

                    when {
                        diffDays < 0 -> 0
                        diffDays in 0..2 -> 1
                        else -> 2
                    }
                } else {
                    3
                }
            } catch (e: Exception) {
                3
            }
        })

        notifyDataSetChanged()
    }
}