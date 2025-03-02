package com.example.seguimientopeliculas.ui.movies.add

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.seguimientopeliculas.R
import com.example.seguimientopeliculas.data.remote.models.Movie
import com.example.seguimientopeliculas.databinding.FragmentAddMoviesBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddMovieFragment : Fragment() {

    private lateinit var binding: FragmentAddMoviesBinding
    private val viewModel: AddMovieViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddMoviesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observa el estado de la UI
        observeUiState()

        // Configurar el botón de guardar
        binding.saveButton.setOnClickListener {
            saveMovie()
        }

        // Configurar los dropdowns de género y estado
        setupDropdowns()
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AddMovieUiState.Idle -> {
                        binding.saveButton.isEnabled = true
                        binding.errorMessage.visibility = View.GONE
                    }
                    is AddMovieUiState.Loading -> {
                        binding.saveButton.isEnabled = false
                        binding.errorMessage.visibility = View.GONE
                    }
                    is AddMovieUiState.Success -> {
                        binding.saveButton.isEnabled = true
                        binding.errorMessage.text = "Película añadida correctamente."
                        binding.errorMessage.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                        binding.errorMessage.visibility = View.VISIBLE

                        // Redirigir a MovieListFragment
                        findNavController().navigate(R.id.action_addMoviesFragment_to_movieListFragment)
                    }
                    is AddMovieUiState.Error -> {
                        binding.saveButton.isEnabled = true
                        binding.errorMessage.text = state.message
                        binding.errorMessage.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                        binding.errorMessage.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun saveMovie() {
        val jwt = getJwtToken()
        val moviesUserId = getMoviesUserId()

        if (jwt == null) {
            showMessage("Error: No se encontró el token de autenticación.", isError = true)
            return
        }

        if (moviesUserId == null) {
            showMessage("Error: No se pudo obtener el ID del usuario.", isError = true)
            return
        }

        if (!validateInputs()) return

        val newMovie = Movie(
            id = 0,
            title = binding.titleInput.text.toString(),
            genre = binding.genreDropdown.text.toString(),
            rating = binding.ratingInput.text.toString().toInt(),
            premiere = binding.premiereSwitch.isChecked,
            status = binding.statusDropdown.text.toString(),
            comments = binding.commentsInput.text.toString(),
            moviesUserId = moviesUserId
        )

        lifecycleScope.launch {
            viewModel.addMovie(newMovie, jwt)
        }
    }

    private fun showMessage(message: String, isError: Boolean) {
        binding.errorMessage.apply {
            text = message
            setTextColor(
                if (isError) resources.getColor(android.R.color.holo_red_dark, null)
                else resources.getColor(android.R.color.holo_green_dark, null)
            )
            visibility = View.VISIBLE
        }
    }

    private fun getJwtToken(): String? {
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("jwt_token", null)
    }

    private fun getMoviesUserId(): Int? {
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val moviesUserId = sharedPreferences.getInt("moviesUserId", -1)
        return if (moviesUserId != -1) moviesUserId else null
    }

    private fun validateInputs(): Boolean {
        if (binding.titleInput.text.isNullOrBlank()) {
            showMessage("El título no puede estar vacío.", isError = true)
            return false
        }

        if (binding.genreDropdown.text.isNullOrBlank()) {
            showMessage("Por favor, selecciona un género.", isError = true)
            return false
        }

        if (binding.ratingInput.text.isNullOrBlank() || binding.ratingInput.text.toString().toIntOrNull() == null) {
            showMessage("Por favor, introduce una calificación válida.", isError = true)
            return false
        }

        if (binding.statusDropdown.text.isNullOrBlank()) {
            showMessage("Por favor, selecciona un estado.", isError = true)
            return false
        }

        return true
    }

    private fun setupDropdowns() {
        val genres = listOf(
            "Comedia", "Ciencia Ficción", "Acción", "Musical", "Terror", "Aventuras",
            "Drama", "Fantasía", "Suspense", "Animación", "Romántico", "Western", "Documental", "Serie"
        )

        val statuses = listOf("Quiero ver", "Visto")

        val genreAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genres)
        binding.genreDropdown.setAdapter(genreAdapter)
        binding.genreDropdown.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.genreDropdown.showDropDown()
        }

        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statuses)
        binding.statusDropdown.setAdapter(statusAdapter)
        binding.statusDropdown.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.statusDropdown.showDropDown()
        }
    }
}