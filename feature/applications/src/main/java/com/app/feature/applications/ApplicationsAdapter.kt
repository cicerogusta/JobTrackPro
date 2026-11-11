package com.app.feature.applications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.core.model.JobApplication
import com.app.designsystem.label
import com.app.feature.applications.databinding.ItemApplicationBinding

class ApplicationsAdapter(private val onOpen: (String) -> Unit) :
    ListAdapter<JobApplication, ApplicationsAdapter.Holder>(Difference) {
    class Holder(val binding: ItemApplicationBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemApplicationBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val application = getItem(position)
        holder.binding.role.text = application.role
        holder.binding.company.text = application.company
        holder.binding.status.text = application.status.label()
        holder.binding.root.setOnClickListener { onOpen(application.id) }
    }
    private object Difference : DiffUtil.ItemCallback<JobApplication>() {
        override fun areItemsTheSame(oldItem: JobApplication, newItem: JobApplication) = oldItem.id == newItem.id && oldItem.userId == newItem.userId
        override fun areContentsTheSame(oldItem: JobApplication, newItem: JobApplication) = oldItem == newItem
    }
}
