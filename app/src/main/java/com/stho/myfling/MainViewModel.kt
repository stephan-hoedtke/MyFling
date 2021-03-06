package com.stho.myfling

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlin.math.roundToInt

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val angleLiveData: MutableLiveData<Double> = MutableLiveData<Double>().apply { value = 0.0 }

    val angleLD: LiveData<Double>
        get() = angleLiveData

    val canResetLD: LiveData<Boolean>
        get() = Transformations.map(angleLiveData) { angle -> angle.roundToInt() != 0 }

    fun rotate(delta: Double) {
        val newAngle = Degree.normalize(angle + delta)
        angleLiveData.postValue(newAngle)
    }

    fun reset() {
        angleLiveData.postValue(0.0)
    }

    private val angle: Double
        get() = angleLiveData.value ?: 0.0
}
