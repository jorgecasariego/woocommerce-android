package com.woocommerce.android.ui.products.variations.attributes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.AttributeOptionListItemBinding
import com.woocommerce.android.model.ProductAttributeTerm
import com.woocommerce.android.ui.products.variations.attributes.AttributeOptionListAdapter.OptionViewHolder

class AttributeOptionListAdapter() : RecyclerView.Adapter<OptionViewHolder>() {
    private var optionsList = listOf<ProductAttributeTerm>()

    init {
        // local attributes all have an Id of 0, so we can't use stable Ids
        setHasStableIds(false)
    }

    override fun getItemCount() = optionsList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        return OptionViewHolder(
            AttributeOptionListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        holder.bind(optionsList[position])

        holder.itemView.setOnClickListener {
            // TODO onItemClick(item)
        }
    }

    private class OptionItemDiffUtil(
        val oldList: List<ProductAttributeTerm>,
        val newList: List<ProductAttributeTerm>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition].id == newList[newItemPosition].id

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem == newItem
        }
    }

    fun setOptionList(options: List<ProductAttributeTerm>) {
        val diffResult = DiffUtil.calculateDiff(
            OptionItemDiffUtil(
                optionsList,
                options
            )
        )

        optionsList = options
        diffResult.dispatchUpdatesTo(this)
    }

    inner class OptionViewHolder(val viewBinding: AttributeOptionListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(option: ProductAttributeTerm) {
            viewBinding.optionName.text = option.name
        }
    }
}
