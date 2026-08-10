package com.proyectofinal.quicklist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RewardRequestAdapter(
    private var requests: List<RewardRequest>,
    private val onApprove: (RewardRequest) -> Unit,
    private val onReject: (RewardRequest) -> Unit
) : RecyclerView.Adapter<RewardRequestAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val requestUser: TextView = view.findViewById(R.id.requestUser)
        val requestReward: TextView = view.findViewById(R.id.requestReward)
        val requestPoints: TextView = view.findViewById(R.id.requestPoints)
        val approveButton: Button = view.findViewById(R.id.approveButton)
        val rejectButton: Button = view.findViewById(R.id.rejectButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reward_request, parent, false)

        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]

        holder.requestUser.text = "Usuario: ${request.userEmail}"
        holder.requestReward.text = "Premio: ${request.rewardName}"
        holder.requestPoints.text = "Coste: ${request.pointsRequired} puntos"

        holder.approveButton.setOnClickListener {
            onApprove(request)
        }

        holder.rejectButton.setOnClickListener {
            onReject(request)
        }
    }

    override fun getItemCount(): Int = requests.size

    fun updateList(newRequests: List<RewardRequest>) {
        requests = newRequests
        notifyDataSetChanged()
    }
}