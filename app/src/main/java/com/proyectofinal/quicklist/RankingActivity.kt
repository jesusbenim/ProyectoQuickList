package com.proyectofinal.quicklist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class RankingActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var container: LinearLayout
    private var topicId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)

        // 🔙 Botón volver
        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val rewardsButton = findViewById<Button>(R.id.rewardsButton)

        rewardsButton.setOnClickListener {
            val intent = Intent(this, RewardsActivity::class.java)
            intent.putExtra("TOPIC_ID", topicId)
            startActivity(intent)
        }

        // 🔥 Firebase
        db = FirebaseFirestore.getInstance()
        container = findViewById(R.id.rankingContainer)

        topicId = intent.getStringExtra("TOPIC_ID")

        loadRanking()
    }

    private fun loadRanking() {

        val topicId = topicId ?: return

        db.collection("topics")
            .document(topicId)
            .collection("ranking")
            .get()
            .addOnSuccessListener { result ->

                container.removeAllViews()

                val sorted = result.documents.sortedByDescending {
                    it.getLong("points") ?: 0
                }

                var lastPoints: Long? = null
                var currentPosition = 0

                for (doc in sorted) {

                    val userId = doc.id
                    val points = doc.getLong("points") ?: 0

                    if (lastPoints == null || points != lastPoints) {
                        currentPosition++
                        lastPoints = points
                    }

                    val positionForThisUser = currentPosition

                    val medal = when (positionForThisUser) {
                        1 -> "🥇"
                        2 -> "🥈"
                        3 -> "🥉"
                        else -> "🏅"
                    }

                    db.collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { userDoc ->

                            val email = userDoc.getString("email") ?: "Usuario"
                            val textView = TextView(this)

                            textView.text = "$medal ${positionForThisUser}º - $email → $points puntos"
                            textView.textSize = 18f

                            container.addView(textView)
                        }
                }
            }
    }
}