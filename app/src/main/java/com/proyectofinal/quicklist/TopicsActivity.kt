package com.proyectofinal.quicklist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_topics)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val addButton = findViewById<Button>(R.id.addTopicButton)
        val recyclerView = findViewById<RecyclerView>(R.id.topicsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TopicsAdapter(listOf()) { topic ->
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("TOPIC_ID", topic.id)
            intent.putExtra("TOPIC_NAME", topic.name)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        addButton.setOnClickListener {
            showAddTopicDialog()
        }

        loadTopics()
    }
    // Muestra un cuadro donde el usuario escribe el nombre del tema
    private fun showAddTopicDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Nuevo tema")
            .setMessage("Introduce el nombre del tema")
            .setView(input)
            .setPositiveButton("Crear") { _, _ ->
                val name = input.text.toString()
                if (name.isNotEmpty()) createTopic(name)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    //Crea un nuevo documento en Firestore con el nombre del tema y el UID del usuario en la lista members
    private fun createTopic(name: String) {
        val userId = auth.currentUser?.uid ?: return
        val topic = hashMapOf(
            "name" to name,
            "members" to listOf(userId)
        )
        db.collection("topics").add(topic)
    }
    // Escucha en tiempo real todos los temas donde el usuario es miembro
    private fun loadTopics() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("topics")
            .whereArrayContains("members", userId)
            .addSnapshotListener { snapshot, _ ->
                val topics = snapshot?.documents?.map {
                    Topic(
                        id = it.id,
                        name = it.getString("name") ?: "",
                        members = it.get("members") as? List<String> ?: emptyList()
                    )
                } ?: emptyList()
                adapter.updateList(topics)
            }
    }
}

