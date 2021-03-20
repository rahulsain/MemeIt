package com.example.memeit

import android.app.Activity
import android.graphics.Point
import android.os.Bundle
import android.util.DisplayMetrics

class Gesture : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gesture)

        // get default display from the windows manager
        val display = windowManager.defaultDisplay

        // declare and initialize a point
        val size = Point()

        // store the points related details from the
        // display variable in the size variable
        display.getSize(size)

        // store the point information in integer
        // variables width and height
        // where .x extracts width pixels and
        // .y extracts height pixels
        val width = size.x
        val height = size.y

        window.setLayout((width * .8).toInt(), (height * .9).toInt())
    }
}