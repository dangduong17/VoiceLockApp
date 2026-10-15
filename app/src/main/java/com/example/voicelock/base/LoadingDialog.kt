package com.example.voicelock.base

import android.os.Bundle
import com.example.voicelock.databinding.DialogLoadingBinding

class LoadingDialog : BaseDialogFragment<DialogLoadingBinding>(DialogLoadingBinding::inflate) {
    override fun initView(savedInstanceState: Bundle?) {
        isCancelable = false
    }
}
