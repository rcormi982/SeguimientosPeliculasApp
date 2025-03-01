package com.example.seguimientopeliculas.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil3.load
import coil3.request.crossfade
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import com.example.seguimientopeliculas.R
import com.example.seguimientopeliculas.data.remote.StrapiApi
import com.example.seguimientopeliculas.data.remote.UserRemoteDataSource
import com.example.seguimientopeliculas.databinding.FragmentProfileBinding
import com.example.seguimientopeliculas.workers.WorkManagerHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val viewModel: ProfileViewModel by viewModels()

    @Inject
    lateinit var strapiApi: StrapiApi

    @Inject
    lateinit var userRemoteDataSource: UserRemoteDataSource

    @Inject
    lateinit var workManagerHelper: WorkManagerHelper

    private var currentPhotoPath: Uri? = null

    // Registro para permisos y resultados de actividades
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            takePhoto()
        } else {
            Toast.makeText(
                requireContext(),
                "Se necesita permiso de cámara para esta función",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val requestGalleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(
                requireContext(),
                "Se necesita permiso de almacenamiento para esta función",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val takePhotoResult = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoPath?.let { uri ->
                viewModel.onImageCaptured(uri)
            }
        }
    }

    private val pickImageResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.onImageCaptured(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserData()
        setupPhotoObservers()

        // Configurar los clics de la imagen y el botón de cámara
        binding.profileImage.setOnClickListener {
            showImagePickerDialog()
        }

        binding.cameraButton.setOnClickListener {
            checkCameraPermission()
        }

        binding.updateButton.setOnClickListener {
            val username = binding.usernameInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()
            val confirmPassword = binding.confirmPasswordInput.text.toString().trim()

            if (username.isBlank() || email.isBlank()) {
                Toast.makeText(
                    requireContext(),
                    "Por favor, completa los campos obligatorios.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (password.isNotBlank() && password != confirmPassword) {
                Toast.makeText(
                    requireContext(),
                    "Las contraseñas no coinciden.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val success =
                        updateUser(username, email, if (password.isBlank()) null else password)
                    if (success) {
                        Toast.makeText(
                            requireContext(),
                            "Usuario actualizado correctamente.",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.passwordInput.text?.clear()
                        binding.confirmPasswordInput.text?.clear()
                        binding.root.clearFocus()
                        binding.root.invalidate()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Error al actualizar el usuario.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.deleteButton.setOnClickListener {
            // Mostrar diálogo de confirmación
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Eliminar cuenta")
                .setMessage("¿Estás seguro que desea eliminar la cuenta?")
                .setPositiveButton("Sí") { _, _ ->
                    // Usuario confirmó eliminar cuenta
                    lifecycleScope.launch {
                        val success = deleteUser()
                        if (success) {
                            Toast.makeText(
                                requireContext(),
                                "Usuario eliminado correctamente.",
                                Toast.LENGTH_SHORT
                            ).show()
                            logoutUser()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Error al eliminar el usuario.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .setNegativeButton("No") { dialog, _ ->
                    // Usuario canceló la acción
                    dialog.dismiss()
                }
                .show()
        }

        binding.logoutButton.setOnClickListener {
            logoutUser()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Tomar foto", "Elegir de la galería", "Cancelar")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar foto de perfil")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> checkGalleryPermission()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                takePhoto()
            }

            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkGalleryPermission() {
        when {
            // Para Android 13 y superior
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        openGallery()
                    }

                    else -> {
                        requestGalleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                }
            }
            // Para Android 12 y anterior
            else -> {
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        openGallery()
                    }

                    else -> {
                        requestGalleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            }
        }
    }

    private fun takePhoto() {
        try {
            // Asegurarse de que tenemos un directorio válido para guardar la foto
            val context = requireContext()
            val storageDir = context.getExternalFilesDir(null) ?: context.filesDir

            // Crear el archivo temporal para la foto
            val photoFile = File.createTempFile(
                "profile_photo_",
                ".jpg",
                storageDir
            ).apply {
                // Asegurarse de que el archivo es borrado cuando la app se cierra
                deleteOnExit()
            }

            // Generar el URI usando FileProvider
            currentPhotoPath = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                photoFile
            )

            // Lanzar la cámara
            currentPhotoPath?.let { uri ->
                takePhotoResult.launch(uri)
            }

        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error al crear el archivo de la foto: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageResult.launch(intent)
    }

    private fun setupPhotoObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.photo.collect { uri ->
                if (uri != Uri.EMPTY) {
                    binding.profileImage.load(uri) {
                        crossfade(true)
                        transformations(CircleCropTransformation())
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.photoUrl.collect { url ->
                url?.let {
                    binding.profileImage.load(url) {
                        crossfade(true)
                        transformations(CircleCropTransformation())
                    }
                }
            }
        }
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            try {
                val moviesUser = userRemoteDataSource.getMoviesUserData()
                binding.usernameInput.setText(moviesUser.username)
                binding.emailInput.setText(moviesUser.email)

                moviesUser.imageUrl?.let { url ->
                    binding.profileImage.load(url) {
                        crossfade(true)
                        transformations(CircleCropTransformation())
                    }
                    viewModel.updatePhotoUrl(url)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error al cargar datos del usuario: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun updateUser(username: String, email: String, password: String?): Boolean {
        val userId = obtenerUserId()
        val moviesUserId = obtenerMoviesUserId()

        if (userId == -1 || moviesUserId == -1) {
            throw Exception("User ID o MoviesUser ID no encontrado en SharedPreferences")
        }

        try {
            // Preparar payload para actualizar User
            val userUpdatePayload = mutableMapOf(
                "username" to username,
                "email" to email
            )

            // Variable para rastrear si se cambió la contraseña
            val passwordChanged = !password.isNullOrBlank()

            // Agregar contraseña si se proporciona
            if (passwordChanged) {
                userUpdatePayload["password"] = password!!
            }

            // Actualizar User
            val userUpdateResponse = strapiApi.updateUser(userId, userUpdatePayload)
            if (!userUpdateResponse.isSuccessful) {
                Log.e("ProfileFragment", "Error al actualizar User")
                return false
            }

            // Preparar payload para actualizar MoviesUser
            val moviesUserUpdatePayload = mutableMapOf<String, Any>(
                "username" to username,
                "email" to email
            )

            // Manejar la foto de perfil
            val photoUri = viewModel.photo.value
            if (photoUri != Uri.EMPTY) {
                val uploadResult = userRemoteDataSource.uploadProfilePhoto(photoUri, moviesUserId)
                if (uploadResult != null) {
                    Log.d("ProfileFragment", "Foto subida exitosamente. URL: ${uploadResult.url}")

                    // Actualizar la foto del usuario
                    val photoUpdateSuccess = userRemoteDataSource.updateUserPhoto(moviesUserId, uploadResult)
                    if (!photoUpdateSuccess) {
                        Log.e("ProfileFragment", "Error al actualizar la foto en el usuario")
                    }

                    // Si la foto se sube correctamente, la agregamos al payload
                    moviesUserUpdatePayload["imageUrl"] = uploadResult.id
                    viewModel.updatePhotoUrl(uploadResult.url)
                }
            }

            // Actualizar MoviesUser
            val moviesUserUpdateSuccess = userRemoteDataSource.updateMoviesUser(moviesUserId, moviesUserUpdatePayload)
            if (!moviesUserUpdateSuccess) {
                return false
            }

            // Si se cambió la contraseña, cerrar sesión
            if (passwordChanged) {
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Contraseña actualizada. Por favor, inicie sesión nuevamente.",
                        Toast.LENGTH_SHORT
                    ).show()
                    logoutUser()
                }
                return true
            }

            return true
        } catch (e: Exception) {
            return false
        }
    }

    private suspend fun deleteUser(): Boolean {
        val userId = obtenerUserId()
        if (userId == -1) throw Exception("User ID no encontrado en SharedPreferences")

        val deleteMoviesUserSuccess = deleteMoviesUser()
        if (!deleteMoviesUserSuccess) {
            return false
        }

        val response = strapiApi.deleteUser(userId)
        return response.isSuccessful
    }

    private suspend fun deleteMoviesUser(): Boolean {
        val sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val moviesUserId = sharedPreferences.getInt("moviesUserId", -1)
        if (moviesUserId == -1) throw Exception("MoviesUser ID no encontrado en SharedPreferences")

        val response = strapiApi.deleteMoviesUser(moviesUserId)
        return response.isSuccessful
    }

    private fun obtenerUserId(): Int {
        val sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", -1)
        return userId
    }

    private fun obtenerMoviesUserId(): Int {
        val sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("moviesUserId", -1)
    }

    private fun logoutUser() {
        val sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }

        workManagerHelper.cancelSyncWork()

        findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        Toast.makeText(requireContext(), "Sesión cerrada.", Toast.LENGTH_SHORT).show()
    }
}