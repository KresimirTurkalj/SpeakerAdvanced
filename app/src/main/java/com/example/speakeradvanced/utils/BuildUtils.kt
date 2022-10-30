package com.example.speakeradvanced.utils

import android.os.Build

class BuildUtils {
    companion object {
        fun version() : Int {
            return when (Build.VERSION.SDK_INT) {
                23 -> 6     //M
                24,25 -> 7  //N
                26,27 -> 8  //O
                28 -> 9     //P
                29 -> 10    //Q
                30 -> 11    //R
                31 -> 12    //S
                else -> -1
            }
        }

        fun isVersion(versionNumber : Int) : Boolean {
            return version() >= versionNumber
        }
    }
}