package com.proyectofinal.quicklist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class TopicsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: TopicsAdapter

    private val globalAdmins = listOf(
        "jesusbenim78@icloud.com"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_topics)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val addButton = findViewById<Button>(R.id.addTopicButton)




        val currentEmail = auth.currentUser?.email ?: ""
        val isGlobalAdmin = globalAdmins.contains(currentEmail)

        addButton.visibility = if (isGlobalAdmin) android.view.View.VISIBLE else android.view.View.GONE

        val greetingTextView = findViewById<TextView>(R.id.greetingTextView)
        val questionTextView = findViewById<TextView>(R.id.questionTextView)

        val email = auth.currentUser?.email ?: "usuario"
        val name = email.substringBefore("@")

        greetingTextView.text = "¡Hola, $name! ☀️"
        questionTextView.text = "¿Qué harás hoy?"


        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val recyclerView = findViewById<RecyclerView>(R.id.topicsRecyclerView)

        logoutButton.setOnClickListener {
            auth.signOut()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)



        adapter = TopicsAdapter(
            topics = listOf(),
            onClick = { topic ->
                openTopic(topic)
            },
            onEdit = { topic ->
                showEditTopicDialog(topic)
            },
            onDelete = { topic ->
                showDeleteTopicDialog(topic)
            },
            isGlobalAdminProvider = {
                isGlobalAdmin
            }
        )

        recyclerView.adapter = adapter

        addButton.setOnClickListener {
            showAddTopicDialog()
        }

        loadTopics()
    }

    private fun addUserToTopic(email: String, topicId: String) {

        db.collection("users")
            .whereEqualTo("email", email.trim().lowercase())
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
                    "points" to 0
                )

                db.collection("topics")
                    .document(topicId)
                    .collection("members")
                    .document(userId)
                    .set(member)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Usuario añadido", Toast.LENGTH_SHORT).show()
                    }
            }
    }
    private fun showAddTopicDialog() {
        val input = EditText(this)
        input.setText("") // limpia siempre

        AlertDialog.Builder(this)
            .setTitle("Nuevo tema")
            .setView(input)
            .setPositiveButton("Crear") { _, _ ->
                val name = input.text.toString()
                if (name.isNotEmpty()) {
                    createTopic(name)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun createTopic(name: String) {

        val user = auth.currentUser ?: return
        val topicRef = db.collection("topics").document()

        val topic = hashMapOf(
            "name" to name
        )

        topicRef.set(topic)
            .addOnSuccessListener {

                val member = hashMapOf(
                    "email" to user.email,
                    "points" to 0,
                    "role" to "admin"
                )

                topicRef.collection("members")
                    .document(user.uid)
                    .set(member)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Tema creado correctamente", Toast.LENGTH_SHORT).show()
                        loadTopics()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al añadir creador", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al crear tema", Toast.LENGTH_SHORT).show()
            }
    }
    private fun openTopic(topic: Topic) {
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra("TOPIC_ID", topic.id)
        intent.putExtra("TOPIC_NAME", topic.name)
        startActivity(intent)
    }

    private fun showEditTopicDialog(topic: Topic) {
        val editText = EditText(this)
        editText.setText(topic.name)

        AlertDialog.Builder(this)
            .setTitle("Editar tema")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = editText.text.toString()
                if (newName.isNotEmpty()) {
                    db.collection("topics")
                        .document(topic.id)
                        .update("name", newName)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteTopicDialog(topic: Topic) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar tema")
            .setMessage("¿Seguro que quieres borrar este tema?")
            .setPositiveButton("Borrar") { _, _ ->
                db.collection("topics")
                    .document(topic.id)
                    .update("deleted", true)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Tema eliminado", Toast.LENGTH_SHORT).show()
                        loadTopics()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAddUserDialog(topicId: String) {
        val input = EditText(this)

        AlertDialog.Builder(this)
            .setTitle("Añadir usuario por email")
            .setView(input)
            .setPositiveButton("Añadir") { _, _ ->
                val email = input.text.toString()
                if (email.isNotEmpty()) {
                    addUserToTopic(email, topicId)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun loadTopics() {

        val userId = auth.currentUser?.uid ?: return

        db.collection("topics")
            .addSnapshotListener { snapshot, _ ->

                val docs = snapshot?.documents ?: emptyList()
                val topics = mutableListOf<Topic>()

                if (docs.isEmpty()) {
                    adapter.updateList(emptyList())
                    return@addSnapshotListener
                }

                var processed = 0

                docs.forEach { doc ->

                    if (doc.getBoolean("deleted") == true) {
                        processed++

                        if (processed == docs.size) {
                            adapter.updateList(topics.sortedBy { it.name.lowercase() })
                        }

                        return@forEach
                    }

                    db.collection("topics")
                        .document(doc.id)
                        .collection("members")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { memberDoc ->

                            if (memberDoc.exists()) {
                                topics.add(
                                    Topic(
                                        id = doc.id,
                                        name = doc.getString("name") ?: "",
                                        members = listOf()
                                    )
                                )
                            }

                            processed++

                            if (processed == docs.size) {
                                adapter.updateList(topics.sortedBy { it.name.lowercase() })
                            }
                        }
                        .addOnFailureListener {
                            processed++

                            if (processed == docs.size) {
                                adapter.updateList(topics.sortedBy { it.name.lowercase() })
                            }
                        }
                }
            }
    }

    override fun onResume() {
        super.onResume()
        loadTopics()
    }
}
