package com.example.voicelock.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import com.example.voicelock.base.BaseBottomSheetDialogFragment
import com.example.voicelock.base.BaseDialogFragment
import com.example.voicelock.base.doActionWhenResume

fun Fragment.safeShowDialog(
    dialog: BaseDialogFragment<out ViewBinding>?,
    fragmentManager: FragmentManager = childFragmentManager,
) {
    if (dialog == null) return
    doActionWhenResume { dialog.show(fragmentManager) }
}

fun Fragment.safeShowBottomSheet(
    dialog: BaseBottomSheetDialogFragment<out ViewBinding>?,
    fragmentManager: FragmentManager = childFragmentManager,
) {
    if (dialog == null) return
    doActionWhenResume { dialog.show(fragmentManager) }
}

inline fun <T> Flow<T>.collectFlowOnView(
    owner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline onCollect: suspend (T) -> Unit,
) = owner.lifecycleScope.launch {
    owner.repeatOnLifecycle(state) {
        collect { onCollect(it) }
    }
}
