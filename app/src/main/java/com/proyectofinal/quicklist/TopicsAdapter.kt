package com.proyectofinal.quicklist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TopicsAdapter(
    private var topics: List<Topic>,
    private val onClick: (Topic) -> Unit
) : RecyclerView.Adapter<TopicsAdapter.TopicViewHolder>() {

    inner class TopicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.topicNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_topic, parent, false)
        return TopicViewHolder(view)
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        val topic = topics[position]
        holder.nameTextView.text = topic.name
        holder.itemView.setOnClickListener { onClick(topic) }
    }

    override fun getItemCount() = topics.size

    fun updateList(newList: List<Topic>) {
        topics = newList
        notifyDataSetChanged()
    }
}

