package com.example.seguimientopeliculas.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.seguimientopeliculas.R
import com.example.seguimientopeliculas.data.remote.StrapiApi
import com.example.seguimientopeliculas.data.remote.UserRemoteDataSource
import com.example.seguimientopeliculas.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding

    @Inject
    lateinit var strapiApi: StrapiApi

    @Inject
    lateinit var userRemoteDataSource: UserRemoteDataSource

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserData()

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
                val success = updateUser(username, email, if (password.isBlank()) null else password)
                if (success) {
                    Toast.makeText(
                        requireContext(),
                        "Usuario actualizado correctamente. Por favor, vuelve a iniciar sesión.",
                        Toast.LENGTH_SHORT
                    ).show()
                    logoutUser()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Error al actualizar el usuario.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.deleteButton.setOnClickListener {
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

        binding.logoutButton.setOnClickListener {
            logoutUser()
        }
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            try {
                val user = userRemoteDataSource.getUserData()
                binding.usernameInput.setText(user.username)
                binding.emailInput.setText(user.email)
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
        if (userId == -1) throw Exception("User ID no encontrado en SharedPreferences")

        val updatePayload = mutableMapOf(
            "username" to username,
            "email" to email
        )

        if (!password.isNullOrBlank()) {
            updatePayload["password"] = password
        }

        val response = strapiApi.updateUser(userId, updatePayload)
        return response.isSuccessful
    }

    private suspend fun deleteUser(): Boolean {
        val userId = obtenerUserId()
        if (userId == -1) throw Exception("User ID no encontrado en SharedPreferences")

        // Eliminar primero el MoviesUser
        val deleteMoviesUserSuccess = deleteMoviesUser()
        if (!deleteMoviesUserSuccess) {
            Log.e("ProfileFragment", "Error al eliminar MoviesUser asociado.")
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
        Log.d("ProfileFragment", "User ID obtenido desde SharedPreferences: $userId")
        return userId
    }

    private fun logoutUser() {
        val sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
        findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        Toast.makeText(requireContext(), "Sesión cerrada.", Toast.LENGTH_SHORT).show()
    }
}
