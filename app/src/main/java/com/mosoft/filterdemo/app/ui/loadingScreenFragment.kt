package com.mosoft.filterdemo.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.mosoft.filterdemo.R
import com.mosoft.filterdemo.app.base.baseFragment
import com.mosoft.filterdemo.databinding.FragmentLoadingScreenBinding

class loadingScreenFragment: baseFragment() {
    lateinit var binding : FragmentLoadingScreenBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_loading_screen, container, false)

        return binding.root
    }
}