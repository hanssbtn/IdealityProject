package com.ideality.idealityproject

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.ArrayList

class ModelRepository {
    private val models = MutableStateFlow<ArrayList<Bitmap>>(arrayListOf())

    init {

    }

    fun getModels(): MutableStateFlow<ArrayList<Bitmap>> = models
}