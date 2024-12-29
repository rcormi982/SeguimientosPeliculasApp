package com.example.seguimientopeliculas.login

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
import com.example.seguimientopeliculas.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding

    @Inject
    lateinit var strapiApi: StrapiApi
    @Inject
    lateinit var userRemoteDataSource: UserRemoteDataSource

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginButton.setOnClickListener {
            val username = binding.username.text.toString()
            val password = binding.password.text.toString()

            var valid = true
            if (username.isBlank()) {
                Toast.makeText(requireContext(), "El campo de usuario es obligatorio", Toast.LENGTH_SHORT).show()
                valid = false
            }

            if (password.isBlank()) {
                Toast.makeText(requireContext(), "El campo de contraseña es obligatorio", Toast.LENGTH_SHORT).show()
                valid = false
            }

            if (valid) {
                lifecycleScope.launch {
                    binding.loginProgressBar.visibility = View.VISIBLE
                    binding.loginButton.isEnabled = false

                    try {
                        val loginRequest = LoginRequestApi(
                            identifier = username,
                            password = password
                        )

                        val loginResponse = strapiApi.loginUser(loginRequest)

                        if (loginResponse.isSuccessful) {
                            val jwt = loginResponse.body()?.jwt
                            val moviesUser = loginResponse.body()?.user

                            if (jwt != null && moviesUser != null) {
                                saveUserSession(jwt, moviesUser.id)
                                Toast.makeText(requireContext(), "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                                findNavController().navigate(R.id.action_loginFragment_to_showMoviesFragment)
                            } else {
                                Toast.makeText(requireContext(), "Error al obtener la información del usuario", Toast.LENGTH_SHORT).show()
                                enableLoginButton()
                            }
                        } else {
                            Toast.makeText(requireContext(), "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                            enableLoginButton()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                        enableLoginButton()
                    }
                }
            }
        }

        binding.registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFormFragment)
        }
    }

    private fun saveUserSession(jwt: String, userId: Int) {
        lifecycleScope.launch {
            try {
                val moviesUserId = userRemoteDataSource.fetchMoviesUserId(userId)
                if (moviesUserId != null) {
                    val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
                    with(sharedPreferences.edit()) {
                        putString("jwt_token", jwt)
                        putInt("moviesUserId", moviesUserId)
                        putInt("userId", userId)
                        putBoolean("isLoggedIn", true)
                        apply()
                    }
                    Log.d("LoginFragment", "Sesión guardada: JWT y MoviesUserId guardados correctamente")
                } else {
                    Log.e("LoginFragment", "MoviesUserId no encontrado para el usuario con ID: $userId")
                }
            } catch (e: Exception) {
                Log.e("LoginFragment", "Error al guardar la sesión del usuario: ${e.message}")
            }
        }
    }

    private fun enableLoginButton() {
        binding.loginProgressBar.visibility = View.GONE
        binding.loginButton.isEnabled = true
    }
}
