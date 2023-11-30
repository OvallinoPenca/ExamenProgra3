package com.example.ExamenPrograMobile

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class FormRecepcionViewModel  : ViewModel() {
    val receptor = mutableStateOf("")
    val latitud = mutableStateOf(0.0)
    val longitud = mutableStateOf(0.0)
    val fotoRecepcion = mutableStateOf<Uri?>(null)
}
