package com.example.voicelock.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Shared singleton executor for all ListAdapter diff calculations.
 * Prevents thread leak from creating new executor per adapter.
 */
private val diffExecutor: ExecutorService by lazy {
    Executors.newSingleThreadExecutor { r ->
        Thread(r, "ListAdapter-Diff-Thread").apply {
            isDaemon = true
        }
    }
}

interface BaseRecyclerAdapter<Item : Any, VB : ViewBinding> {
    fun bindView(
        binding: VB,
        item: Item,
        position: Int,
    )
}

abstract class BaseListAdapter<Item : Any, Binding : ViewBinding>(
    diffCallback: DiffUtil.ItemCallback<Item>,
) : ListAdapter<Item, BaseViewHolder<Binding>>(
        AsyncDifferConfig
            .Builder(diffCallback)
            .setBackgroundThreadExecutor(diffExecutor)
            .build(),
    ),
    BaseRecyclerAdapter<Item, Binding> {

    abstract fun inflateBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int,
    ): Binding

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BaseViewHolder<Binding> {
        val inflater = LayoutInflater.from(parent.context)
        return BaseViewHolder(inflateBinding(inflater, parent, viewType))
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<Binding>,
        position: Int,
    ) {
        val item = getItem(position)
        if (item != null) {
            bindView(holder.binding, item, position)
        }
    }
}

open class BaseViewHolder<Binding : ViewBinding>(
    val binding: Binding,
) : RecyclerView.ViewHolder(binding.root)

inline fun <T> createDiffCallback(
    crossinline areItemsTheSame: (T, T) -> Boolean,
    crossinline areContentsTheSame: (T, T) -> Boolean,
): DiffUtil.ItemCallback<T> =
    object : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(
            oldItem: T & Any,
            newItem: T & Any,
        ): Boolean = areItemsTheSame(oldItem, newItem)

        override fun areContentsTheSame(
            oldItem: T & Any,
            newItem: T & Any,
        ): Boolean = areContentsTheSame(oldItem, newItem)
    }
