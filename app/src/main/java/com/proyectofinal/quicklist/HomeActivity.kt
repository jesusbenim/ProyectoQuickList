package com.proyectofinal.quicklist

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: TaskAdapter
    private var topicId: String? = null
    private var topicName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // ✅ Recuperamos el tema seleccionado desde TopicsActivity
        topicId = intent.getStringExtra("TOPIC_ID")
        topicName = intent.getStringExtra("TOPIC_NAME")

        if (topicId == null) {
            Toast.makeText(this, "Error: No se recibió el tema", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 🔤 Mostramos el nombre del tema en pantalla
        findViewById<TextView>(R.id.welcomeTextView).text = "Tema: $topicName"

        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            finish()  // 🔙 vuelve a TopicsActivity
        }
        val taskEditText = findViewById<EditText>(R.id.taskEditText)
        val addButton = findViewById<Button>(R.id.addTaskButton)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val recyclerView = findViewById<RecyclerView>(R.id.tasksRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TaskAdapter(listOf()) { task, newStatus ->
            updateTaskStatus(task, newStatus)
        }
        recyclerView.adapter = adapter


        // 🔄 Cargar tareas del tema seleccionado
        loadTasks()

        addButton.setOnClickListener {
            val text = taskEditText.text.toString()
            if (text.isNotEmpty()) addTask(text)
            taskEditText.text.clear()
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            finish()
        }
    }

    private fun loadTasks() {
        topicId?.let { id ->
            db.collection("topics").document(id).collection("tasks")
                .addSnapshotListener { snapshot, _ ->
                    val tasks = snapshot?.documents?.map {
                        Task(
                            id = it.id,
                            text = it.getString("text") ?: "",
                            status = it.getString("status") ?: "pending",
                            photoUrl = it.getString("photoUrl")
                        )

                    } ?: emptyList()
                    adapter.updateList(tasks)
                }
        }
    }

    private fun addTask(text: String) {
        topicId?.let { id ->
            val task = hashMapOf(
                "text" to text,
                "status" to "pending",
                "photoUrl" to null
            )

            db.collection("topics").document(id).collection("tasks").add(task)
        }
    }

    private fun updateTaskStatus(task: Task, newStatus: String) {
        val userId = auth.currentUser?.uid ?: return
        val topicId = intent.getStringExtra("TOPIC_ID") ?: return

        val ref = db.collection("topics").document(topicId)
            .collection("tasks").document(task.id)

        ref.update("status", newStatus)
    }

}

