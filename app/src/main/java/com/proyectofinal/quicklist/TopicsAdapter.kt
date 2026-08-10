package com.proyectofinal.quicklist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TopicsAdapter(
    private var topics: List<Topic>,
    private val onClick: (Topic) -> Unit,
    private val onEdit: (Topic) -> Unit,
    private val onDelete: (Topic) -> Unit,
    private val isGlobalAdminProvider: () -> Boolean
) : RecyclerView.Adapter<TopicsAdapter.TopicViewHolder>() {

    inner class TopicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.topicNameTextView)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_topic, parent, false)
        return TopicViewHolder(view)
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        val topic = topics[position]

        holder.name.text = topic.name

        holder.itemView.setOnClickListener {
            onClick(topic)
        }

        val isAdmin = isGlobalAdminProvider()

        holder.btnEdit.visibility =
            if (isAdmin) View.VISIBLE else View.GONE

        holder.btnDelete.visibility =
            if (isAdmin) View.VISIBLE else View.GONE

        holder.btnEdit.setOnClickListener {
            onEdit(topic)
        }

        holder.btnDelete.setOnClickListener {
            onDelete(topic)
        }
    }

    override fun getItemCount() = topics.size

    fun updateList(newList: List<Topic>) {
        topics = newList
        notifyDataSetChanged()
    }
}

