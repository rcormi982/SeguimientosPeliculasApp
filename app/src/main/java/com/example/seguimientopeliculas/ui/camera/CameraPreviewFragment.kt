package com.example.seguimientopeliculas.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil3.load
import coil3.request.crossfade
import com.example.seguimientopeliculas.R
import com.example.seguimientopeliculas.databinding.FragmentCameraPreviewBinding
import com.example.seguimientopeliculas.ui.profile.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

@AndroidEntryPoint
class CameraPreviewFragment : Fragment() {
    private lateinit var binding: FragmentCameraPreviewBinding
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var cameraController: LifecycleCameraController

    // Registrar el launcher de permisos
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso concedido, iniciar la cámara
            startCamera()
        } else {
            // Permiso denegado
            Toast.makeText(
                requireContext(),
                "Permiso de cámara denegado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraPreviewBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verificar y solicitar permisos
        checkCameraPermission()

        binding.captureIncidentBtn.setOnClickListener {
            captureImageToDisk()
        }
    }

    private fun checkCameraPermission() {
        when {
            // Si ya tiene permiso, iniciar la cámara
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            // Mostrar explicación si es necesario
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(
                    requireContext(),
                    "Se necesita permiso de cámara para tomar fotos",
                    Toast.LENGTH_LONG
                ).show()
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            // Solicitar permiso
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        try {
            val previewView = binding.profilePreview
            cameraController = LifecycleCameraController(requireContext())
            cameraController.bindToLifecycle(viewLifecycleOwner)
            cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraController.setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            previewView.controller = cameraController
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error al inicializar la cámara: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    //Función para almacenar la foto tomada en disco

    private fun captureImageToDisk() {
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            requireContext().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        val cameraExecutor = Executors.newSingleThreadScheduledExecutor()

        cameraController.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // Mostrar un diálogo de confirmación con la foto capturada
                    viewLifecycleOwner.lifecycleScope.launch {
                        showConfirmationDialog(outputFileResults.savedUri)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.message?.let {
                        Toast.makeText(
                            requireContext(),
                            "Error al capturar la foto: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    @SuppressLint("MissingInflatedId")
    private fun showConfirmationDialog(photoUri: android.net.Uri?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_photo_confirmation, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val imagePreview = dialogView.findViewById<ImageView>(R.id.imagePreview)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        // Mostrar la foto capturada en el ImageView
        imagePreview.load(photoUri) {
            crossfade(true)
        }

        // Si el usuario confirma, guardar la foto y volver al perfil
        btnConfirm.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.onImageCaptured(photoUri)
                findNavController().popBackStack()
            }
            dialog.dismiss()
        }

        // Si el usuario cancela, volver a la cámara
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}