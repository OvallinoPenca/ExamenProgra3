package com.example.ExamenPrograMobile

import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.engage.common.datamodel.Image

class MainActivity : ComponentActivity() {
    val cameraAppVm:CameraAppViewModel by viewModels()
    lateinit var cameraController:LifecycleCameraController
    val lanzadorPermisos = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            when {
                (it[android.Manifest.permission.ACCESS_FINE_LOCATION] ?:
                false) or (it[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?:
                false) -> {
                    Log.v("callback RequestMultiplePermissions", "permiso ubicacion granted")
                            cameraAppVm.onPermisoUbicacionOk()
                }
                (it[android.Manifest.permission.CAMERA] ?: false) -> {
                    Log.v("callback RequestMultiplePermissions", "permiso camara granted")
                            cameraAppVm.onPermisoCamaraOk()
                }
                else -> {
                }
            }
        }

    private fun setupCamara() {
        cameraController = LifecycleCameraController(this)
        cameraController.bindToLifecycle(this)
        cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraAppVm.lanzadorPermisos = lanzadorPermisos
        setupCamara()
        setContent {
            AppUI(cameraController)
        }
    }
}
fun generarNombreSegunFechaHastaSegundo():String = LocalDateTime
    .now().toString().replace(Regex("[T:.-]"), "").substring(0, 14)
fun crearArchivoImagenPrivado(contexto:Context):File = File(
    contexto.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
    "${generarNombreSegunFechaHastaSegundo()}.jpg"
)
fun uri2imageBitmap(uri: Uri, contexto: Context) =
    BitmapFactory.decodeStream(
        contexto.contentResolver.openInputStream(uri)
    ).asImageBitmap()
fun tomarFotografia(cameraController: CameraController, archivo:File,
                    contexto:Context, imagenGuardadaOk:(uri:Uri)->Unit) {
    val outputFileOptions = OutputFileOptions.Builder(archivo).build()
    cameraController.takePicture(outputFileOptions,
        ContextCompat.getMainExecutor(contexto), object:OnImageSavedCallback {
            override fun onImageSaved(outputFileResults:
                                      ImageCapture.OutputFileResults) {
                outputFileResults.savedUri?.also {
                    Log.v("tomarFotografia()::onImageSaved", "Foto guardada en ${it.toString()}")
                            imagenGuardadaOk(it)
                }
            }
            override fun onError(exception: ImageCaptureException) {
                Log.e("tomarFotografia()", "Error: ${exception.message}")
            }
        })
}
class SinPermisoException(mensaje:String) : Exception(mensaje)
fun getUbicacion(contexto: Context, onUbicacionOk:(location:Location) ->
Unit):Unit {

    try {
        val servicio =
            LocationServices.getFusedLocationProviderClient(contexto)
        val tarea =
            servicio.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        tarea.addOnSuccessListener {
            onUbicacionOk(it)
        }
    } catch (e:SecurityException) {
        throw SinPermisoException(e.message?:"No tiene permisos para conseguir la ubicación")
    }
}
@Composable
fun AppUI(cameraController: CameraController) {
    val contexto = LocalContext.current
    val formRecepcionVm:FormRecepcionViewModel = viewModel()
    val cameraAppViewModel:CameraAppViewModel = viewModel()
    when(cameraAppViewModel.pantalla.value) {
        Pantalla.FORM -> {
            PantallaFormUI(
                formRecepcionVm,
                tomarFotoOnClick = {
                    cameraAppViewModel.cambiarPantallaFoto()
                    cameraAppViewModel.lanzadorPermisos?.launch(arrayOf(Manifest.permission.C
                            AMERA))
                },
                actualizarUbicacionOnClick = {
                    cameraAppViewModel.onPermisoUbicacionOk = {
                        getUbicacion(contexto) {
                            formRecepcionVm.latitud.value = it.latitude
                            formRecepcionVm.longitud.value = it.longitude
                        }
                    }
                    cameraAppViewModel.lanzadorPermisos?.launch(arrayOf(Manifest.permission.A
                            CCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            )
        }
        Pantalla.FOTO -> {
            PantallaFotoUI(formRecepcionVm, cameraAppViewModel,
                cameraController)
        }
        else -> {
            Log.v("AppUI()", "when else, no debería entrar aquí")
        }
    }
}

@Composable
fun PantallaFormUI(
    formRecepcionVm:FormRecepcionViewModel,
    tomarFotoOnClick:() -> Unit = {},
    actualizarUbicacionOnClick:() -> Unit = {}
) {
    val contexto = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            label = { Text("Receptor") },
            value = formRecepcionVm.receptor.value,
            onValueChange = {formRecepcionVm.receptor.value = it},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        )

        //Nombre foto
        Text("Ingres eEl nombre del lugar :")
        Button(onClick = {
            tomarFotoOnClick()
        }) {
            Text("Tomar Fotografía")
        }
        formRecepcionVm.fotoRecepcion.value?.also {
            Box(Modifier.size(200.dp, 100.dp)) {
                Image(
                    painter = BitmapPainter(uri2imageBitmap(it,
                        contexto)),
                    contentDescription = "Imagen recepción encomienda
                    ${formRecepcionVm.receptor.value}"
                )
            }
        }
        Text("La ubicación es: lat: ${formRecepcionVm.latitud.value} y
                long: ${formRecepcionVm.longitud.value}")
        Button(onClick = {
            actualizarUbicacionOnClick()
        }) {
            Text("Actualizar Ubicación")
        }
        Spacer(Modifier.height(100.dp))
        MapaOsmUI(formRecepcionVm.latitud.value,
            formRecepcionVm.longitud.value)
    }
}

@Composable
fun PantallaFotoUI(formRecepcionVm:FormRecepcionViewModel, appViewModel:
CameraAppViewModel, cameraController: CameraController) {
    val contexto = LocalContext.current
    AndroidView(
        factory = {
            PreviewView(it).apply {
                controller = cameraController
            },
            modifier = Modifier.fillMaxSize()
            )
            Button(onClick = {
                tomarFotografia(
                    cameraController,
                    crearArchivoImagenPrivado(contexto),
                    contexto
                ) {
                    formRecepcionVm.fotoRecepcion.value = it
                    appViewModel.cambiarPantallaForm()
                }
            }) {
                Text("Tomar foto")
            }
        }
        @Composable
        fun MapaOsmUI(latitud:Double, longitud:Double) {
            val contexto = LocalContext.current
            AndroidView(
                factory = {
                    MapView(it).also {
                        it.setTileSource(TileSourceFactory.MAPNIK)
                        Configuration.getInstance().userAgentValue =
                            contexto.packageName
                    }
                }, update = {
                    it.overlays.removeIf { true }
                    it.invalidate()
                    it.controller.setZoom(18.0)
                    val geoPoint = GeoPoint(latitud, longitud)
                    it.controller.animateTo(geoPoint)
                    val marcador = Marker(it)
                    marcador.position = geoPoint
                    marcador.setAnchor(Marker.ANCHOR_CENTER,
                        Marker.ANCHOR_CENTER)
                    it.overlays.add(marcador)
                }
            )
        }
