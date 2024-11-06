package com.ideality.idealityproject

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow

class ModelRepository {
    private val models = MutableStateFlow<ArrayList<Bitmap>>(arrayListOf())

    init {

    }

    fun getModels(): MutableStateFlow<ArrayList<Bitmap>> = models
}