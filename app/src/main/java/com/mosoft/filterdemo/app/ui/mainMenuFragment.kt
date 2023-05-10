package com.mosoft.filterdemo.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.mosoft.filterdemo.R
import com.mosoft.filterdemo.app.base.baseFragment
import com.mosoft.filterdemo.databinding.FragmentMainMenuBinding

class mainMenuFragment : baseFragment() {
    lateinit var binding: FragmentMainMenuBinding


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_main_menu, container, false)

        binding.btnFiltering.setOnClickListener {
            it.findNavController().navigate(R.id.action_mainMenuFragment_to_filteringScreenFragment)
        }

        binding.btnPreFiltered.setOnClickListener {
            it.findNavController()
                .navigate(R.id.action_mainMenuFragment_to_preFilteredScreenFragment)
        }

        return binding.root
    }
}