package com.github.skgmn.cameraxx

fun interface OnZoomRangeChangedListener {
    fun onZoomRangeChanged(range: ClosedRange<Float>)
}