package com.woocommerce.android.ui.orders.notes

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.woocommerce.android.ui.orders.notes.OrderNoteListItem.Header
import com.woocommerce.android.ui.orders.notes.OrderNoteListItem.Note
import com.woocommerce.android.ui.orders.notes.OrderNoteListItem.ViewType

class OrderNotesAdapter : Adapter<OrderNoteViewHolder>() {
    private val notes = mutableListOf<OrderNoteListItem>()

    init {
        setHasStableIds(true)
    }

    fun setNotes(newList: List<OrderNoteListItem>) {
        val diffResult = DiffUtil.calculateDiff(OrderNotesDiffCallback(notes.toList(), newList))
        notes.clear()
        notes.addAll(newList)

        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderNoteViewHolder {
        return when (viewType) {
            ViewType.NOTE.id -> NoteItemViewHolder(parent)
            ViewType.HEADER.id -> HeaderItemViewHolder(parent)
            else -> throw IllegalArgumentException("Unexpected view type in OrderNotesAdapter")
        }
    }

    override fun onBindViewHolder(holder: OrderNoteViewHolder, position: Int) {
        when (holder) {
            is NoteItemViewHolder -> holder.bind(notes[position] as Note)
            is HeaderItemViewHolder -> holder.bind(notes[position] as Header)
            else -> throw IllegalArgumentException("Unexpected view holder in OrderNotesAdapter")
        }
    }

    override fun getItemCount() = notes.size

    override fun getItemId(position: Int): Long = notes[position].longId

    override fun getItemViewType(position: Int): Int {
        return notes[position].viewType.id
    }
}
