package com.mosoft.filterdemo.app.filterList

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.mosoft.filterdemo.databinding.ItemFilterBinding

class filterListRVItemViewHolder(private val binding: ItemFilterBinding) :
    ViewHolder(binding.root) {

    fun bind(data: filterDataClass) {
        binding.tvFilterName.text = data.filterName
    }
}