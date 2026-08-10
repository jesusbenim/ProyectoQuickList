package com.proyectofinal.quicklist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ApprovedRewardAdapter(
    private var rewards: List<RewardRequest>,
    private val onUsed: (RewardRequest) -> Unit
) : RecyclerView.Adapter<ApprovedRewardAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val rewardName: TextView = view.findViewById(R.id.rewardName)
        val rewardUser: TextView = view.findViewById(R.id.rewardUser)
        val btnUsed: Button = view.findViewById(R.id.btnUsed)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_approved_reward, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount() = rewards.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val reward = rewards[position]

        holder.rewardName.text = reward.rewardName
        holder.rewardUser.text = reward.userEmail

        holder.btnUsed.setOnClickListener {
            onUsed(reward)
        }
    }

    fun updateList(newList: List<RewardRequest>) {
        rewards = newList
        notifyDataSetChanged()
    }
}