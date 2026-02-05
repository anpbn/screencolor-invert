package com.screencolor.invert.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.screencolor.invert.data.ColorPair
import com.screencolor.invert.databinding.ItemColorPairBinding

/**
 * RecyclerView Adapter for ColorPair items
 */
class ColorPairAdapter(
    private val onColorClick: (ColorPair, Boolean) -> Unit,
    private val onToleranceChange: (ColorPair, Float) -> Unit,
    private val onEnableChange: (ColorPair, Boolean) -> Unit,
    private val onDeleteClick: (ColorPair) -> Unit
) : ListAdapter<ColorPair, ColorPairAdapter.ViewHolder>(DiffCallback()) {

    private val colorPairs = mutableListOf<ColorPair>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemColorPairBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemCount(): Int = colorPairs.size

    override fun getItem(position: Int): ColorPair = colorPairs[position]

    /**
     * Submit new list
     */
    fun submitList(list: List<ColorPair>) {
        colorPairs.clear()
        colorPairs.addAll(list)
        notifyDataSetChanged()
    }

    /**
     * Add new item
     */
    fun addItem(pair: ColorPair) {
        colorPairs.add(pair)
        notifyItemInserted(colorPairs.size - 1)
    }

    /**
     * Update item
     */
    fun updateItem(pair: ColorPair) {
        val index = colorPairs.indexOfFirst { it.id == pair.id }
        if (index != -1) {
            colorPairs[index] = pair
            notifyItemChanged(index)
        }
    }

    /**
     * Remove item
     */
    fun removeItem(pair: ColorPair) {
        val index = colorPairs.indexOfFirst { it.id == pair.id }
        if (index != -1) {
            colorPairs.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class ViewHolder(
        private val binding: ItemColorPairBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pair: ColorPair) {
            // Enable switch
            binding.switchEnable.isChecked = pair.isEnabled
            binding.switchEnable.setOnCheckedChangeListener { _, isChecked ->
                onEnableChange(pair, isChecked)
            }

            // Target color
            binding.viewTargetColor.setBackgroundColor(pair.targetColor)
            binding.cardTargetColor.setOnClickListener {
                onColorClick(pair, true)
            }

            // Replacement color
            binding.viewReplacementColor.setBackgroundColor(pair.replacementColor)
            binding.cardReplacementColor.setOnClickListener {
                onColorClick(pair, false)
            }

            // Tolerance slider
            binding.sliderTolerance.value = pair.tolerance * 100
            binding.tvToleranceValue.text = "${(pair.tolerance * 100).toInt()}%"
            
            binding.sliderTolerance.addOnChangeListener { _, value, _ ->
                binding.tvToleranceValue.text = "${value.toInt()}%"
                onToleranceChange(pair, value)
            }

            // Delete button
            binding.btnDelete.setOnClickListener {
                onDeleteClick(pair)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ColorPair>() {
        override fun areItemsTheSame(oldItem: ColorPair, newItem: ColorPair): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ColorPair, newItem: ColorPair): Boolean {
            return oldItem == newItem
        }
    }
}
