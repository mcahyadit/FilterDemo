package com.mosoft.filterdemo.app.filterList

data class filterDataClass(
    val id: Int,
    val filterName: String,
    val numberOfParameters: Int,
    val parameterString: List<String>,
    val parameterBool: List<Boolean>,
    val paramaterInt: List<Int>
)