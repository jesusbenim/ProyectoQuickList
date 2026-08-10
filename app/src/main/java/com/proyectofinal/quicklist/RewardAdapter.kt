package com.proyectofinal.quicklist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RewardAdapter(
    private var rewards: List<Reward>,
    private val onRedeem: (Reward) -> Unit
) : RecyclerView.Adapter<RewardAdapter.RewardViewHolder>() {

    inner class RewardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rewardName: TextView = view.findViewById(R.id.rewardName)
        val rewardPoints: TextView = view.findViewById(R.id.rewardPoints)
        val btnRedeem: Button = view.findViewById(R.id.btnRedeem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reward, parent, false)

        return RewardViewHolder(view)
    }

    override fun onBindViewHolder(holder: RewardViewHolder, position: Int) {
        val reward = rewards[position]

        holder.rewardName.text = reward.name
        holder.rewardPoints.text = "${reward.pointsRequired} puntos"

        holder.btnRedeem.setOnClickListener {
            onRedeem(reward)
        }
    }

    override fun getItemCount(): Int = rewards.size

    fun updateList(newRewards: List<Reward>) {
        rewards = newRewards
        notifyDataSetChanged()
    }
}