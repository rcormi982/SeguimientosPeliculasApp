package com.example.seguimientopeliculas.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.seguimientopeliculas.R
import com.example.seguimientopeliculas.databinding.FragmentWelcomeBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WelcomeFragment : Fragment() {

    private lateinit var binding: FragmentWelcomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gifImageView = view.findViewById<ImageView>(R.id.clapperboardImage)
        Glide.with(this)
            .asGif()
            .load(R.drawable.claqueta6) // Cambia por el nombre de tu archivo GIF
            .into(gifImageView)

        lifecycleScope.launch {
            delay(4000) // Esperar 4 segundos (4000 milisegundos)
            val isLoggedIn = checkUserSession() // Verificar el estado de la sesión
            if (isLoggedIn) {
                findNavController().navigate(R.id.action_welcomeFragment_to_showMoviesFragment)
            } else {
                findNavController().navigate(R.id.action_welcomeFragment_to_loginFragment)
            }
        }
    }
    private fun checkUserSession(): Boolean {
        // Leer estado de sesión desde SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isLoggedIn", false) // Devuelve false si no está configurado
    }
}

