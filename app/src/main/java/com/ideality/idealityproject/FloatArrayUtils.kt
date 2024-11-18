package com.ideality.idealityproject

fun FloatArray.setXY(idx: Int, x: Float, y: Float) {
    this[(idx * 2) + 0] = x
    this[(idx * 2) + 1] = y
}

fun FloatArray.setUV(idx: Int, u: Float, v: Float) = setXY(idx, u, v)

fun FloatArray.setXYZ(idx: Int, x: Float, y: Float, z: Float) {
    this[(idx * 3) + 0] = x
    this[(idx * 3) + 1] = y
    this[(idx * 3) + 2] = z
}