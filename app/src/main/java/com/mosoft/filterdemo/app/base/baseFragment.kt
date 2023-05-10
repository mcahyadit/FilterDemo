package com.mosoft.filterdemo.app.base

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import com.mosoft.filterdemo.R
import com.mosoft.filterdemo.app.events.EventBus
import com.google.android.material.snackbar.Snackbar

open class baseFragment : Fragment() {

    override fun onResume() {
        super.onResume()

        EventBus.getInstance().register(this)
    }

    override fun onPause() {
        super.onPause()

        EventBus.getInstance().unregister(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.base_fragment, container, false)
    }

    fun MakeToast(text : String, dur : Int = Toast.LENGTH_LONG) {
        Toast.makeText(context, text, dur).show()
    }

    fun MakeSnackbar(text : String, dur: Int = Snackbar.LENGTH_LONG) {
        Snackbar.make(context!!, view!!, text, dur).show()
    }

    fun showKeyboard() {
    }

    fun hideKeyboard() {
        val inputMethodManager = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}