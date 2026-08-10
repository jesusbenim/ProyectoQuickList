package com.proyectofinal.quicklist

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class RewardsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var rewardAdapter: RewardAdapter
    private lateinit var requestAdapter: RewardRequestAdapter

    private var topicId: String? = null
    private var userPoints: Int = 0
    private var currentUserRole: String = "worker"

    private lateinit var myRewardsAdapter: MyRewardsAdapter

    private lateinit var approvedRewardAdapter: ApprovedRewardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewards)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        topicId = intent.getStringExtra("TOPIC_ID")

        val rewardsRecyclerView = findViewById<RecyclerView>(R.id.rewardsRecyclerView)
        val requestsRecyclerView = findViewById<RecyclerView>(R.id.requestsRecyclerView)
        val myRewardsRecyclerView = findViewById<RecyclerView>(R.id.myRewardsRecyclerView)
        val approvedRewardsRecyclerView =
            findViewById<RecyclerView>(R.id.approvedRewardsRecyclerView)

        val rewards = listOf(
            Reward("1", "🍕 Pizza", 1),
            Reward("2", "🎬 2 Entradas cine", 2),
            Reward("3", "🍔 Cena especial", 3),
            Reward("4", "🛌 Día libre de tareas", 4)
        )

        rewardAdapter = RewardAdapter(rewards) { reward ->
            confirmRewardRequest(reward)
        }

        requestAdapter = RewardRequestAdapter(
            requests = listOf(),
            onApprove = { request ->
                approveRewardRequest(request)
            },
            onReject = { request ->
                rejectRewardRequest(request)
            }

        )
        myRewardsAdapter = MyRewardsAdapter(listOf())

        approvedRewardAdapter = ApprovedRewardAdapter(listOf()) { reward ->
            markRewardAsUsed(reward)
        }

        rewardsRecyclerView.layoutManager = LinearLayoutManager(this)
        rewardsRecyclerView.adapter = rewardAdapter

        requestsRecyclerView.layoutManager = LinearLayoutManager(this)
        requestsRecyclerView.adapter = requestAdapter

        myRewardsRecyclerView.layoutManager = LinearLayoutManager(this)
        myRewardsRecyclerView.adapter = myRewardsAdapter
        approvedRewardsRecyclerView.layoutManager = LinearLayoutManager(this)
        approvedRewardsRecyclerView.adapter = approvedRewardAdapter

        loadUserPoints()
        loadCurrentUserRole()
        loadPendingRequests()
        loadMyRewards()
        loadApprovedRewards()
    }

    private fun loadUserPoints() {
        val topicId = topicId ?: return
        val userId = auth.currentUser?.uid ?: return

        db.collection("topics")
            .document(topicId)
            .collection("ranking")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                userPoints = doc.getLong("points")?.toInt() ?: 0
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

                if (currentUserRole != "admin") {
                    findViewById<RecyclerView>(R.id.requestsRecyclerView).visibility = android.view.View.GONE
                    findViewById<android.widget.TextView>(R.id.pendingRequestsTitle).visibility = android.view.View.GONE

                    findViewById<RecyclerView>(R.id.approvedRewardsRecyclerView).visibility = android.view.View.GONE
                    findViewById<android.widget.TextView>(R.id.approvedRewardsTitle).visibility = android.view.View.GONE
                } else {
                    findViewById<RecyclerView>(R.id.myRewardsRecyclerView).visibility = android.view.View.GONE
                    findViewById<android.widget.TextView>(R.id.myRewardsTitle).visibility = android.view.View.GONE
                }
            }
    }

    private fun loadPendingRequests() {
        val topicId = topicId ?: return

        db.collection("topics")
            .document(topicId)
            .collection("rewardRequests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->

                val requests = snapshot?.documents?.map {
                    RewardRequest(
                        id = it.id,
                        userId = it.getString("userId") ?: "",
                        userEmail = it.getString("userEmail") ?: "",
                        rewardName = it.getString("rewardName") ?: "",
                        pointsRequired = it.getLong("pointsRequired")?.toInt() ?: 0,
                        status = it.getString("status") ?: "pending"
                    )
                } ?: emptyList()

                requestAdapter.updateList(requests)
            }
    }

    private fun confirmRewardRequest(reward: Reward) {
        val topicId = topicId ?: return
        val userId = auth.currentUser?.uid ?: return

        db.collection("topics")
            .document(topicId)
            .collection("rewardRequests")
            .whereEqualTo("userId", userId)
            .whereIn("status", listOf("pending", "approved"))
            .get()
            .addOnSuccessListener { snapshot ->

                val reservedPoints = snapshot.documents.sumOf {
                    it.getLong("pointsRequired") ?: 0
                }

                val availablePoints = userPoints - reservedPoints

                if (availablePoints < reward.pointsRequired) {
                    Toast.makeText(
                        this,
                        "No tienes puntos disponibles suficientes",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                AlertDialog.Builder(this)
                    .setTitle("Solicitar premio")
                    .setMessage(
                        "Tienes $availablePoints puntos disponibles.\n\n" +
                                "¿Quieres solicitar ${reward.name} por ${reward.pointsRequired} puntos?"
                    )
                    .setPositiveButton("Solicitar") { _, _ ->
                        createRewardRequest(reward)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
    }

    private fun createRewardRequest(reward: Reward) {
        val topicId = topicId ?: return
        val user = auth.currentUser ?: return

        val request = hashMapOf(
            "userId" to user.uid,
            "userEmail" to (user.email ?: ""),
            "rewardName" to reward.name,
            "pointsRequired" to reward.pointsRequired,
            "status" to "pending"
        )

        db.collection("topics")
            .document(topicId)
            .collection("rewardRequests")
            .add(request)
            .addOnSuccessListener {
                Toast.makeText(this, "Solicitud enviada al administrador", Toast.LENGTH_SHORT).show()
            }
    }

    private fun approveRewardRequest(request: RewardRequest) {
        val topicId = topicId ?: return

        val rankingRef = db.collection("topics")
            .document(topicId)
            .collection("ranking")
            .document(request.userId)

        val requestRef = db.collection("topics")
            .document(topicId)
            .collection("rewardRequests")
            .document(request.id)

        db.runTransaction { transaction ->

            val requestSnapshot = transaction.get(requestRef)
            val currentStatus = requestSnapshot.getString("status") ?: "pending"

            if (currentStatus != "pending") {
                throw Exception("Esta solicitud ya fue gestionada")
            }

            val rankingSnapshot = transaction.get(rankingRef)
            val currentPoints = rankingSnapshot.getLong("points") ?: 0

            if (currentPoints < request.pointsRequired) {
                throw Exception("El usuario no tiene puntos suficientes")
            }

            val newPoints = currentPoints - request.pointsRequired

            transaction.update(rankingRef, "points", newPoints)

            transaction.update(
                requestRef,
                mapOf(
                    "status" to "approved",
                    "approvedAt" to FieldValue.serverTimestamp()
                )
            )

        }.addOnSuccessListener {
            Toast.makeText(
                this,
                "Premio aprobado",
                Toast.LENGTH_SHORT
            ).show()

        }.addOnFailureListener { e ->
            Toast.makeText(
                this,
                e.message ?: "No se pudo aprobar el premio",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun loadMyRewards() {
        val topicId = topicId ?: return
        val userId = auth.currentUser?.uid ?: return

        db.collection("topics")
            .document(topicId)
            .collection("rewardRequests")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->

                val rewards = snapshot?.documents?.map {
                    RewardRequest(
                        id = it.id,
                        userId = it.getString("userId") ?: "",
                        userEmail = it.getString("userEmail") ?: "",
                        rewardName = it.getString("rewardName") ?: "",
                        pointsRequired = it.getLong("pointsRequired")?.toInt() ?: 0,
                        status = it.getString("status") ?: "pending"
                    )
                } ?: emptyList()

                myRewardsAdapter.updateList(rewards)
            }
    }

    private fun rejectRewardRequest(request: RewardRequest) {
        val topicId = topicId ?: return

        db.collection("topics")
            .document(topicId)
            .collection("rewardRequests")
            .document(request.id)
            .update("status", "rejected")
            .addOnSuccessListener {
                Toast.makeText(this, "Solicitud denegada", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadApprovedRewards() {
        val topicId = topicId ?: return

        db.collection("topics")
            .document(topicId)
            .collection("rewardRequests")
            .whereEqualTo("status", "approved")
            .addSnapshotListener { snapshot, _ ->

                val rewards = snapshot?.documents?.map {
                    RewardRequest(
                        id = it.id,
                        userId = it.getString("userId") ?: "",
                        userEmail = it.getString("userEmail") ?: "",
                        rewardName = it.getString("rewardName") ?: "",
                        pointsRequired = it.getLong("pointsRequired")?.toInt() ?: 0,
                        status = it.getString("status") ?: "approved"
                    )
                } ?: emptyList()

                approvedRewardAdapter.updateList(rewards)
            }
    }

    private fun markRewardAsUsed(reward: RewardRequest) {
        val topicId = topicId ?: return

        db.collection("topics")
            .document(topicId)
            .collection("rewardRequests")
            .document(reward.id)
            .update(
                mapOf(
                    "status" to "used",
                    "usedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Premio marcado como disfrutado", Toast.LENGTH_SHORT).show()
            }
    }
}