package com.mosoft.filterdemo.app.events

import android.util.Log
import com.squareup.otto.Bus

object EventBus {
    //val bus = Bus()

    private val mBus = Bus()

    fun getInstance() : Bus {
        Log.i("EventBus", "Instance of Event Bus is called")
        return mBus
    }

    private fun BusProvider() {

    }


}