package com.example.seguimientopeliculas.ui

import android.os.Bundle
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.seguimientopeliculas.R
import com.example.seguimientopeliculas.data.remote.UserRemoteDataSource
import com.example.seguimientopeliculas.databinding.FragmentRegisterFormBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RegisterFormFragment : Fragment() {

    private lateinit var binding: FragmentRegisterFormBinding

    @Inject
    lateinit var userRemoteDataSource: UserRemoteDataSource

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.regButton.setOnClickListener {
            handleRegisterButton()
        }
    }

    private fun handleRegisterButton() {
        val username = binding.username.text.toString().trim()
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()
        val confirmPassword = binding.confirmPassword.text.toString().trim()

        if (!validateInput(username, email, password, confirmPassword)) {
            return
        }

        lifecycleScope.launch {
            registerUser(username, email, password)
        }
    }

    private fun validateInput(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        when {
            username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                Toast.makeText(requireContext(), "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return false
            }
            username.length < 3 -> {
                Toast.makeText(requireContext(), "El nombre de usuario debe tener al menos 3 caracteres", Toast.LENGTH_SHORT).show()
                return false
            }
            !email.contains("@") || !email.contains(".") -> {
                Toast.makeText(requireContext(), "Por favor, ingresa un correo electrónico válido", Toast.LENGTH_SHORT).show()
                return false
            }
            password.length < 6 -> {
                Toast.makeText(requireContext(), "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return false
            }
            password != confirmPassword -> {
                Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return false
            }
            else -> return true
        }
    }

    private suspend fun registerUser(username: String, email: String, password: String) {
        try {
            val response = userRemoteDataSource.registerUser(username, email, password)

            if (response.isSuccessful) {
                Toast.makeText(requireContext(), "Registro exitoso. Redirigiendo...", Toast.LENGTH_SHORT).show()
                navigateToLogin()
            } else {
                val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                Toast.makeText(requireContext(), "Error en el registro: $errorBody", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al registrar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToLogin() {
        try {
            findNavController().navigate(R.id.action_registerFormFragment_to_loginFragment)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al redirigir al login: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
