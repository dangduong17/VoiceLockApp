package com.example.voicelock.feature.applock.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.voicelock.base.BaseListAdapter
import com.example.voicelock.base.createDiffCallback
import com.example.voicelock.data.model.applock.AppLockItemUiModel
import com.example.voicelock.databinding.LayoutItemAppLockBinding

class AppLockAdapter : BaseListAdapter<AppLockItemUiModel, LayoutItemAppLockBinding>(
    createDiffCallback(
        areItemsTheSame = { old, new -> old.packageName == new.packageName },
        areContentsTheSame = { old, new -> old == new },
    ),
) {
    interface Listener {
        fun onToggleLocked(
            packageName: String,
            isLocked: Boolean,
        )
    }

    private var listener: Listener? = null

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int,
    ): LayoutItemAppLockBinding = LayoutItemAppLockBinding.inflate(inflater, parent, false)

    override fun bindView(
        binding: LayoutItemAppLockBinding,
        item: AppLockItemUiModel,
        position: Int,
    ) {
        binding.ivAppIcon.setImageDrawable(item.icon)
        binding.tvAppLabel.text = item.label
        binding.switchAppLock.setOnCheckedChangeListener(null)
        binding.switchAppLock.isChecked = item.isLocked
        binding.switchAppLock.setOnCheckedChangeListener { _, isChecked ->
            listener?.onToggleLocked(item.packageName, isChecked)
        }
    }
}
