package com.mosoft.filterdemo.db

import com.mosoft.filterdemo.app.filterList.filterDataClass

fun filterList(): List<filterDataClass> {
    var tmpList = buildList<filterDataClass> {
        lateinit var tmpStringList: List<String>
        lateinit var tmpBoolList: List<Boolean>
        lateinit var tmpIntList: List<Int>
        lateinit var tmpData: filterDataClass

        tmpStringList = listOf<String>(
            "Passband Frequency", "Hz",
            "Filter Order", "",
            "Passband Attenuation", "dB"
        )
        tmpBoolList = listOf<Boolean>(
            false,
            false,
            true
        )
        tmpIntList = listOf<Int>(
            12000,
            150,
            3000
        )
        tmpData = filterDataClass(
            0,
            "Low-Pass Filter (Filter Order, Attenuation in dB)",
            3,
            tmpStringList,
            tmpBoolList,
            tmpIntList
        )
        this.add(tmpData.id, tmpData)

        tmpStringList = listOf<String>(
            "Passband Frequency", "Hz",
            "Filter Order", "",
            "Passband Attenuation", ""
        )
        tmpBoolList = listOf<Boolean>(
            false,
            false,
            true
        )
        tmpIntList = listOf<Int>(
            12000,
            150,
            3000
        )
        tmpData = filterDataClass(
            1,
            "Low-Pass Filter (Filter Order, Linear Attenuation)",
            3,
            tmpStringList,
            tmpBoolList,
            tmpIntList
        )
        this.add(tmpData.id, tmpData)

        tmpStringList = listOf<String>(
            "Passband Frequency", "Hz",
            "Stopband Frequency", "Hz",
            "Passband Attenuation", "dB",
            "Stopband Attenuation", "dB"
        )
        tmpBoolList = listOf<Boolean>(
            false,
            false,
            true,
            true
        )
        tmpIntList = listOf<Int>(
            12000,
            12000,
            3000,
            3000
        )
        tmpData = filterDataClass(
            2,
            "Low-Pass Filter (Stopband, Attenuation in dB)",
            4,
            tmpStringList,
            tmpBoolList,
            tmpIntList
        )
        this.add(tmpData.id, tmpData)

        tmpStringList = listOf<String>(
            "Passband Frequency", "Hz",
            "Stopband Frequency", "Hz",
            "Passband Attenuation", "",
            "Stopband Attenuation", ""
        )
        tmpBoolList = listOf<Boolean>(
            false,
            false,
            true,
            true
        )
        tmpIntList = listOf<Int>(
            12000,
            12000,
            3000,
            3000
        )
        tmpData = filterDataClass(
            3,
            "Low-Pass Filter (Stopband, Linear Attenuation)",
            4,
            tmpStringList,
            tmpBoolList,
            tmpIntList
        )
        this.add(tmpData.id, tmpData)

        tmpStringList = listOf<String>(
            "Passband Frequency", "Hz",
            "Filter Order", "",
            "Passband Attenuation", "dB"
        )
        tmpBoolList = listOf<Boolean>(
            false,
            false,
            true
        )
        tmpIntList = listOf<Int>(
            12000,
            150,
            3000
        )
        tmpData = filterDataClass(
            4,
            "High-Pass Filter (Filter Order, Attenuation in dB)",
            3,
            tmpStringList,
            tmpBoolList,
            tmpIntList
        )
        this.add(tmpData.id, tmpData)

        tmpStringList = listOf<String>(
            "Passband Frequency", "Hz",
            "Filter Order", "",
            "Passband Attenuation", ""
        )
        tmpBoolList = listOf<Boolean>(
            false,
            false,
            true
        )
        tmpIntList = listOf<Int>(
            12000,
            150,
            3000
        )
        tmpData = filterDataClass(
            5,
            "High-Pass Filter (Filter Order, Linear Attenuation)",
            3,
            tmpStringList,
            tmpBoolList,
            tmpIntList
        )
        this.add(tmpData.id, tmpData)

        tmpStringList = listOf<String>(
            "Passband Frequency", "Hz",
            "Stopband Frequency", "Hz",
            "Passband Attenuation", "dB",
            "Stopband Attenuation", "dB"
        )
        tmpBoolList = listOf<Boolean>(
            false,
            false,
            true,
            true
        )
        tmpIntList = listOf<Int>(
            12000,
            12000,
            3000,
            3000
        )
        tmpData = filterDataClass(
            6,
            "High-Pass Filter (Stopband, Attenuation in dB)",
            4,
            tmpStringList,
            tmpBoolList,
            tmpIntList
        )
        this.add(tmpData.id, tmpData)

        tmpStringList = listOf<String>(
            "Passband Frequency", "Hz",
            "Stopband Frequency", "Hz",
            "Passband Attenuation", "",
            "Stopband Attenuation", ""
        )
        tmpBoolList = listOf<Boolean>(
            false,
            false,
            true,
            true
        )
        tmpIntList = listOf<Int>(
            12000,
            12000,
            3000,
            3000
        )
        tmpData = filterDataClass(
            7,
            "High-Pass Filter (Stopband, Linear Attenuation)",
            4,
            tmpStringList,
            tmpBoolList,
            tmpIntList
        )
        this.add(tmpData.id, tmpData)
    }

    return tmpList
}