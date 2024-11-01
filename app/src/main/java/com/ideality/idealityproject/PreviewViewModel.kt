package com.ideality.idealityproject

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class PreviewViewModel @Inject internal constructor(

): ViewModel() {
    val selectedNode = MutableStateFlow<ModelNode?>(null)
    val selectedItem = MutableLiveData<View>()

    fun select(idx: Int) {

    }
}