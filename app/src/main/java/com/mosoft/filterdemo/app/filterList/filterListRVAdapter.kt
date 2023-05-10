package com.mosoft.filterdemo.app.filterList

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mosoft.filterdemo.app.events.EventBus
import com.mosoft.filterdemo.app.events.Events
import com.mosoft.filterdemo.databinding.ItemFilterBinding

class filterListRVAdapter(val list: List<filterDataClass>) :
    RecyclerView.Adapter<filterListRVItemViewHolder>() {

    lateinit var binding: ItemFilterBinding
    var currentItemId = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): filterListRVItemViewHolder {
        binding = ItemFilterBinding.inflate(LayoutInflater.from(parent.context))
        return filterListRVItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: filterListRVItemViewHolder, position: Int) {
        holder.bind(list[position])

        EventBus.getInstance().register(this)
        holder.itemView.setOnClickListener {
            if (currentItemId == list[position].id) {
                //to do
            } else {
                currentItemId = list[position].id
                EventBus.getInstance().post(Events.filterListItemOnClick(list[position]))
            }
        }
    }
}