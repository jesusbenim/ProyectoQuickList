package com.proyectofinal.quicklist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MyRewardsAdapter(
    private var rewards: List<RewardRequest>
) : RecyclerView.Adapter<MyRewardsAdapter.MyRewardViewHolder>() {

    inner class MyRewardViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val rewardName: TextView =
            view.findViewById(R.id.rewardName)

        val rewardStatus: TextView =
            view.findViewById(R.id.rewardStatus)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyRewardViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_my_reward,
                parent,
                false
            )

        return MyRewardViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MyRewardViewHolder,
        position: Int
    ) {

        val reward = rewards[position]

        holder.rewardName.text = reward.rewardName

        holder.rewardStatus.text =
            when (reward.status) {
                "pending" -> "⏳ Pendiente de aprobación"
                "approved" -> "✅ Pendiente de disfrutar"
                "used" -> "🎉 Disfrutado"
                "rejected" -> "❌ Denegado"
                else -> reward.status
            }
    }

    override fun getItemCount(): Int = rewards.size

    fun updateList(newRewards: List<RewardRequest>) {
        rewards = newRewards
        notifyDataSetChanged()
    }
}