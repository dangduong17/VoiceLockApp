package com.example.voicelock.base

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import timber.log.Timber

abstract class BaseBottomSheetDialogFragment<T : ViewBinding>(
    private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> T,
) : BottomSheetDialogFragment() {
    private var _binding: T? = null

    protected val binding: T
        get() =
            checkNotNull(_binding) {
                "BottomSheetDialogFragment ${this::class.simpleName} binding cannot be accessed before onCreateView() or after onDestroyView()"
            }

    protected inline fun binding(block: T.() -> Unit): T = binding.apply(block)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Timber.d("${this::class.simpleName} onCreateView")
        _binding = inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initData(savedInstanceState)
        initView(savedInstanceState)
        addEvent(savedInstanceState)
    }

    open fun initData(savedInstanceState: Bundle?) {}

    open fun initView(savedInstanceState: Bundle?) {}

    open fun addEvent(savedInstanceState: Bundle?) {}

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener {
                findViewById<View>(R.id.design_bottom_sheet)
                    ?.setBackgroundResource(android.R.color.transparent)
            }
        }

    override fun onDestroyView() {
        Timber.d("${this::class.simpleName} onDestroyView")
        super.onDestroyView()
        _binding = null
    }

    fun show(manager: FragmentManager) {
        show(manager, tag)
    }

    override fun show(
        manager: FragmentManager,
        tag: String?,
    ) {
        if (isVisible) return
        runCatching {
            manager.beginTransaction().remove(this).commit()
            super.show(manager, tag)
        }
    }

    override fun dismiss() {
        runCatching { super.dismiss() }
    }
}
