package com.proyectofinal.quicklist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.app.Activity
import android.net.Uri
import android.widget.ImageView
import android.view.View

class HomeActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: TaskAdapter

    private var topicId: String? = null

    private var currentUserRole: String = "worker"
    private var topicName: String? = null
    private var selectedTaskForEvidence: Task? = null
    private val PICK_IMAGE_REQUEST = 1001

    private lateinit var dashboardTasks: TextView
    private lateinit var dashboardPoints: TextView
    private lateinit var dashboardRewards: TextView

    private lateinit var dashboardPosition: TextView

    private lateinit var inviteButton: Button
    private lateinit var removeUserButton: Button
    private lateinit var addTaskButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        dashboardTasks = findViewById(R.id.dashboardTasks)
        dashboardPoints = findViewById(R.id.dashboardPoints)
        dashboardRewards = findViewById(R.id.dashboardRewards)
        dashboardPosition = findViewById(R.id.dashboardPosition)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        removeUserButton = findViewById(R.id.removeUserButton)

        removeUserButton.setOnClickListener {
            showRemoveUserDialog()
        }

        val backButton = findViewById<Button>(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }
        topicId = intent.getStringExtra("TOPIC_ID")
        topicName = intent.getStringExtra("TOPIC_NAME")

        val topicNameTextView = findViewById<TextView>(R.id.topicNameTextView)
        topicNameTextView.text = "Tema: ${topicName ?: "Sin nombre"}"

        val recyclerView = findViewById<RecyclerView>(R.id.tasksRecyclerView)
        addTaskButton = findViewById(R.id.addTaskButton)
        val rankingButton = findViewById<Button>(R.id.rankingButton)


        val logoutButton = findViewById<Button>(R.id.logoutButton)
        inviteButton = findViewById(R.id.inviteButton)

        logoutButton.setOnClickListener {
            auth.signOut()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        inviteButton.setOnClickListener {
            showInviteDialog()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = TaskAdapter(
            tasks = listOf(),

            onStatusChange = { task, newStatus ->
                updateTaskStatus(task, newStatus)
            },

            onEdit = { task ->
                showEditTaskDialog(task)
            },

            onDelete = { task ->
                showDeleteTaskDialog(task)
            },


            isAdminProvider = {
                currentUserRole == "admin"
            },

            onUploadEvidence = { task ->
                selectedTaskForEvidence = task

                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "*/*"

                startActivityForResult(
                    Intent.createChooser(intent, "Selecciona una evidencia"),
                    PICK_IMAGE_REQUEST
                )
            },

            onApproveEvidence = { task ->
                confirmApproveEvidence(task)
            },

                    onViewEvidence = { task ->
                viewEvidence(task)
            },

            onRejectEvidence = { task ->
                confirmRejectEvidence(task)
            }

        )

        recyclerView.adapter = adapter

        addTaskButton.setOnClickListener {
            showAddTaskDialog()
        }

        rankingButton.setOnClickListener {
            val intent = Intent(this, RankingActivity::class.java)
            intent.putExtra("TOPIC_ID", topicId)
            startActivity(intent)
        }

        loadTasks()
        loadTopicMembers()
        loadCurrentUserRole()
        loadDashboard()
    }

    private fun loadDashboard() {
        val topicId = topicId ?: return
        val userId = auth.currentUser?.uid ?: return

        db.collection("topics")
            .document(topicId)
            .collection("tasks")
            .addSnapshotListener { snapshot, _ ->

                val tasks = snapshot?.documents ?: emptyList()

                val pending = tasks.count {
                    (it.getString("status") ?: "pending") == "pending"
                }

                val inProgress = tasks.count {
                    it.getString("status") == "in_progress"
                }

                val done = tasks.count {
                    it.getString("status") == "done"
                }

                dashboardTasks.text =
                    "📋 Pendientes: $pending · ⏳ En proceso: $inProgress · ✅ Terminadas: $done"
            }

        db.collection("topics")
            .document(topicId)
            .collection("ranking")
            .document(userId)
            .addSnapshotListener { doc, _ ->
                val points = doc?.getLong("points") ?: 0
                dashboardPoints.text = "⭐ Mis puntos: $points"
            }

        db.collection("topics")
            .document(topicId)
            .collection("rewardRequests")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "approved")
            .addSnapshotListener { snapshot, _ ->
                val rewards = snapshot?.size() ?: 0
                dashboardRewards.text = "🎁 Premios pendientes: $rewards"
            }

        db.collection("topics")
            .document(topicId)
            .collection("ranking")
            .get()
            .addOnSuccessListener { result ->

                val sorted = result.documents.sortedByDescending {
                    it.getLong("points") ?: 0
                }

                var position = 0

                sorted.forEachIndexed { index, doc ->
                    if (doc.id == userId) {
                        position = index + 1
                    }
                }

                dashboardPosition.text =
                    "🏆 Mi posición: ${position}º de ${sorted.size}"
            }


    }

    private fun confirmApproveEvidence(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Aprobar evidencia")
            .setMessage("¿Quieres aprobar la evidencia subida y marcar esta tarea como terminada?")
            .setPositiveButton("Aprobar") { _, _ ->
                approveEvidence(task)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun viewEvidence(task: Task) {
        if (task.evidenceUrl.isBlank()) {
            Toast.makeText(this, "No hay evidencia", Toast.LENGTH_SHORT).show()
            return
        }

        val imageView = ImageView(this)
        imageView.adjustViewBounds = true
        imageView.setPadding(20, 20, 20, 20)

        try {
            imageView.setImageURI(Uri.parse(task.evidenceUrl))

            AlertDialog.Builder(this)
                .setTitle("Evidencia")
                .setView(imageView)
                .setPositiveButton("Cerrar", null)
                .show()

        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir la evidencia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmRejectEvidence(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Denegar evidencia")
            .setMessage("¿Quieres denegar esta evidencia y pedir al trabajador que suba otra?")
            .setPositiveButton("Denegar") { _, _ ->
                rejectEvidence(task)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun rejectEvidence(task: Task) {
        val topicId = topicId ?: return

        db.collection("topics")
            .document(topicId)
            .collection("tasks")
            .document(task.id)
            .update(
                mapOf(
                    "evidenceUrl" to "",
                    "evidenceUploadedBy" to "",
                    "evidenceUploadedById" to "",
                    "approved" to false,
                    "evidenceRejected" to true,
                    "status" to "pending"
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Evidencia denegada. El trabajador debe subir otra.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun approveEvidence(task: Task) {
        val topicId = topicId ?: return

        val userId = task.evidenceUploadedById
        val userEmail = task.evidenceUploadedBy

        if (userId.isBlank()) {
            Toast.makeText(this, "No se encontró el usuario que subió la evidencia", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("topics")
            .document(topicId)
            .collection("tasks")
            .document(task.id)
            .update(
                mapOf(
                    "approved" to true,
                    "status" to "done",
                    "completedBy" to userId,
                    "completedByEmail" to userEmail
                )
            )
            .addOnSuccessListener {
                updatePoints(userId, 1)
                Toast.makeText(this, "Evidencia aprobada y tarea terminada", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveEvidenceLocal(imageUri: Uri) {
        val task = selectedTaskForEvidence ?: return
        val topicId = topicId ?: return
        val userEmail = auth.currentUser?.email ?: "usuario"
        val userId = auth.currentUser?.uid ?: return

        db.collection("topics")
            .document(topicId)
            .collection("tasks")
            .document(task.id)
            .update(
                mapOf(
                    "evidenceUrl" to imageUri.toString(),
                    "evidenceUploadedBy" to userEmail,
                    "evidenceUploadedById" to userId,
                    "approved" to false,
                    "status" to "pending_review",
                    "evidenceRejected" to false,
                )
            )
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Evidencia enviada para revisión",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Error al guardar evidencia",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (
            requestCode == PICK_IMAGE_REQUEST &&
            resultCode == Activity.RESULT_OK &&
            data != null &&
            data.data != null
        ) {

            val imageUri: Uri = data.data!!
            val takeFlags = data?.flags?.and(Intent.FLAG_GRANT_READ_URI_PERMISSION) ?: 0
            contentResolver.takePersistableUriPermission(
                imageUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            saveEvidenceLocal(imageUri)

        }
    }



    private fun loadCurrentUserRole() {
        val topicId = topicId ?: return
        val userId = auth.currentUser?.uid ?: return

        db.collection("topics")
            .document(topicId)
            .collection("members")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->

                currentUserRole = doc.getString("role") ?: "worker"

                if (currentUserRole == "admin") {

                    inviteButton.visibility = View.VISIBLE
                    removeUserButton.visibility = View.VISIBLE
                    addTaskButton.visibility = View.VISIBLE

                } else {

                    inviteButton.visibility = View.GONE
                    removeUserButton.visibility = View.GONE
                    addTaskButton.visibility = View.GONE
                }


                adapter.notifyDataSetChanged()
            }
    }

    private fun showEditTaskDialog(task: Task) {
        val editText = EditText(this)
        editText.setText(task.text)

        AlertDialog.Builder(this)
            .setTitle("Editar tarea")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val newText = editText.text.toString()
                if (newText.isNotEmpty()) {
                    val topicId = topicId ?: return@setPositiveButton
                    db.collection("topics")
                        .document(topicId)
                        .collection("tasks")
                        .document(task.id)
                        .update("text", newText)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteTaskDialog(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar tarea")
            .setMessage("¿Seguro que quieres eliminar esta tarea?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteTask(task)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteTask(task: Task) {
        val topicId = topicId ?: return

        db.collection("topics")
            .document(topicId)
            .collection("tasks")
            .document(task.id)
            .delete()
    }

    private fun checkDueSoonTasks(tasks: List<Task>) {
        val formatter = java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale.getDefault())
        val today = java.util.Calendar.getInstance()

        for (task in tasks) {
            if (task.dueDate.isBlank()) continue
            if (task.status == "done") continue

            try {
                val dueDate = formatter.parse(task.dueDate) ?: continue

                val dueCalendar = java.util.Calendar.getInstance()
                dueCalendar.time = dueDate

                val diffMillis = dueCalendar.timeInMillis - today.timeInMillis
                val diffDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMillis)

                if (diffDays in 0..2) {
                    Toast.makeText(
                        this,
                        "⚠️ La tarea \"${task.text}\" vence pronto (${task.dueDate})",
                        Toast.LENGTH_LONG
                    ).show()
                    break
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showInviteDialog() {
        val input = EditText(this)

        AlertDialog.Builder(this)
            .setTitle("Invitar usuario")
            .setMessage("Introduce el email")
            .setView(input)
            .setPositiveButton("Invitar") { _, _ ->
                val email = input.text.toString()
                if (email.isNotEmpty()) {
                    addUserToTopic(email)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addUserToTopic(email: String) {

        val cleanEmail = email.trim().lowercase()
        val topicId = topicId ?: return

        db.collection("users")
            .whereEqualTo("email", cleanEmail)
            .get()
            .addOnSuccessListener { result ->

                if (result.isEmpty) {
                    Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val userDoc = result.documents[0]
                val userId = userDoc.id
                val userEmail = userDoc.getString("email") ?: ""

                val member = hashMapOf(
                    "email" to userEmail,
                    "points" to 0,
                    "role" to "worker"
                )

                db.collection("topics")
                    .document(topicId)
                    .collection("members")
                    .document(userId)
                    .set(member)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Usuario añadido correctamente", Toast.LENGTH_SHORT).show()
                        loadTopicMembers()
                    }
            }
    }
    // 🔹 Crear tarea con diálogo (igual que temas)
    private fun showAddTaskDialog() {

        val input = EditText(this)

        val checkBox = android.widget.CheckBox(this)
        checkBox.text = "Requiere evidencia"

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(40, 20, 40, 0)

        layout.addView(input)
        layout.addView(checkBox)

        AlertDialog.Builder(this)
            .setTitle("Nueva tarea")
            .setMessage("Escribe la tarea")
            .setView(layout)

            .setPositiveButton("Siguiente") { _, _ ->

                val text = input.text.toString()

                if (text.isNotEmpty()) {

                    showDatePicker(
                        text,
                        checkBox.isChecked
                    )
                }
            }

            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDatePicker(text: String, requiresEvidence: Boolean) {

        val calendar = java.util.Calendar.getInstance()

        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH)
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        val datePicker = android.app.DatePickerDialog(
            this,
            { _, y, m, d ->

                val selectedDate = "$d/${m+1}/$y"

                createTask(text, selectedDate, requiresEvidence) // 👈 guardamos con fecha
            },
            year, month, day
        )

        datePicker.show()
    }

    // 🔹 Guardar tarea en Firebase
    private fun createTask(text: String, dueDate: String, requiresEvidence: Boolean) {

        val topicId = topicId ?: return


        val task = hashMapOf(
            "text" to text,
            "status" to "pending",
            "dueDate" to dueDate,
            "requiresEvidence" to requiresEvidence,
            "evidenceUrl" to "",
            "approved" to false
        )

        db.collection("topics")
            .document(topicId)
            .collection("tasks")
            .add(task)
    }

    private fun updateTaskStatus(
        task: Task,
        newStatus: String
    ) {

        val userId = auth.currentUser?.uid ?: return
        val userEmail = auth.currentUser?.email ?: return
        val topicId = topicId ?: return

        if (newStatus == "done" && task.status != "done") {

            AlertDialog.Builder(this)
                .setTitle("Marcar como terminada")
                .setMessage("¿Seguro que quieres marcar esta tarea como terminada?")
                .setPositiveButton("Sí") { _, _ ->

                    if (task.requiresEvidence) {

                        Toast.makeText(
                            this,
                            "Debes subir una evidencia",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        markTaskAsDone(
                            task,
                            userId,
                            userEmail,
                            topicId
                        )
                    }
                }

                .setNegativeButton("Cancelar", null)
                .show()

            return
        }

        updateTaskStatusDirect(
            task,
            newStatus,
            userId,
            topicId
        )
    }

    private fun markTaskAsDone(task: Task, userId: String, userEmail: String, topicId: String) {
        val ref = db.collection("topics")
            .document(topicId)
            .collection("tasks")
            .document(task.id)

        ref.update(
            mapOf(
                "status" to "done",
                "completedBy" to userId,
                "completedByEmail" to userEmail
            )
        )
        updatePoints(userId, 1)


    }

    private fun updateTaskStatusDirect(
        task: Task,
        newStatus: String,
        userId: String,
        topicId: String
    ) {

        val ref = db.collection("topics")
            .document(topicId)
            .collection("tasks")
            .document(task.id)

        ref.update("status", newStatus)
    }
    // 🔹 Cargar tareas en tiempo real
    private fun loadTasks() {
        val topicId = topicId ?: return

        db.collection("topics")
            .document(topicId)
            .collection("tasks")
            .addSnapshotListener { snapshot, _ ->

                val tasks = snapshot?.documents?.map {
                    Task(
                        id = it.id,
                        text = it.getString("text") ?: "",
                        status = it.getString("status") ?: "pending",
                        dueDate = it.getString("dueDate") ?: "",
                        completedBy = it.getString("completedBy") ?: "",
                        completedByEmail = it.getString("completedByEmail") ?: "",
                        requiresEvidence = it.getBoolean("requiresEvidence") ?: false,
                        evidenceUrl = it.getString("evidenceUrl") ?: "",
                        approved = it.getBoolean("approved") ?: false,
                        evidenceUploadedBy = it.getString("evidenceUploadedBy") ?: "",
                        evidenceUploadedById = it.getString("evidenceUploadedById") ?: "",
                        evidenceRejected = it.getBoolean("evidenceRejected") ?: false

                    )
                } ?: emptyList()

                adapter.updateList(tasks)
                checkDueSoonTasks(tasks)
            }
    }

    private fun loadTopicMembers() {
        val topicId = topicId ?: return
        val topicMembersTextView = findViewById<TextView>(R.id.topicMembersTextView)
        val topicMembersTitleTextView = findViewById<TextView>(R.id.topicMembersTitleTextView)

        val currentUser = auth.currentUser
        val currentUid = currentUser?.uid
        val currentEmail = currentUser?.email

        db.collection("topics")
            .document(topicId)
            .collection("members")
            .get()
            .addOnSuccessListener { snapshot ->

                topicMembersTitleTextView.text = "👥 Miembros del tema (${snapshot.size()})"

                if (snapshot.isEmpty) {
                    topicMembersTextView.text = "• ninguno"
                } else {
                    val lines = snapshot.documents.mapNotNull { doc ->
                        val email = doc.getString("email") ?: return@mapNotNull null
                        val uid = doc.id

                        if (uid == currentUid || email == currentEmail) {
                            "• $email (Tú)"
                        } else {
                            "• $email"
                        }
                    }

                    topicMembersTextView.text = lines.joinToString("\n")
                }
            }
    }

    private fun showRemoveUserDialog() {
        val topicId = topicId ?: return
        val currentUid = auth.currentUser?.uid

        db.collection("topics")
            .document(topicId)
            .collection("members")
            .get()
            .addOnSuccessListener { snapshot ->

                val removableDocs = snapshot.documents.filter { it.id != currentUid }

                if (removableDocs.isEmpty()) {
                    Toast.makeText(this, "No hay usuarios invitados para eliminar", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val emails = removableDocs.map { it.getString("email") ?: "Usuario sin email" }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Eliminar usuario del tema")
                    .setItems(emails) { _, which ->
                        val selectedDoc = removableDocs[which]
                        confirmRemoveUser(selectedDoc.id, selectedDoc.getString("email") ?: "usuario")
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
    }

    private fun updatePoints(userId: String, change: Int) {
        val topicId = topicId ?: return

        val rankingRef = db.collection("topics")
            .document(topicId)
            .collection("ranking")
            .document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(rankingRef)
            val current = snapshot.getLong("points") ?: 0
            val newPoints = (current + change).coerceAtLeast(0)

            transaction.set(
                rankingRef,
                mapOf("points" to newPoints),
                com.google.firebase.firestore.SetOptions.merge()
            )
        }.addOnSuccessListener {

            db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { userDoc ->
                    val email = userDoc.getString("email") ?: return@addOnSuccessListener

                    db.collection("topics")
                        .document(topicId)
                        .collection("members")
                        .whereEqualTo("email", email)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { result ->
                            val memberDoc = result.documents.firstOrNull() ?: return@addOnSuccessListener

                            memberDoc.reference.update(
                                "points",
                                com.google.firebase.firestore.FieldValue.increment(change.toLong())
                            )
                        }
                }
        }
    }

    private fun confirmRemoveUser(userIdToRemove: String, email: String) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar usuario")
            .setMessage("¿Seguro que quieres eliminar a $email del tema?")
            .setPositiveButton("Eliminar") { _, _ ->
                removeUserFromTopic(userIdToRemove)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun removeUserFromTopic(userIdToRemove: String) {
        val topicId = topicId ?: return

        val topicRef = db.collection("topics").document(topicId)

        topicRef.collection("members")
            .document(userIdToRemove)
            .delete()
            .addOnSuccessListener {

                topicRef.collection("ranking")
                    .document(userIdToRemove)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Usuario eliminado del tema y ranking", Toast.LENGTH_SHORT).show()
                        loadTopicMembers()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Usuario eliminado, pero error al borrar ranking", Toast.LENGTH_SHORT).show()
                        loadTopicMembers()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al eliminar usuario", Toast.LENGTH_SHORT).show()
            }
    }
}

