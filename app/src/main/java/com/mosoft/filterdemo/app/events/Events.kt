package com.mosoft.filterdemo.app.events

import com.mosoft.filterdemo.app.filterList.filterDataClass

class Events {

    class filterListItemOnClick(eventData: filterDataClass) {
        val data = eventData
    }
}