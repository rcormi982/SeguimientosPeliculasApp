package com.example.seguimientopeliculas.ui.movies.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.seguimientopeliculas.R
import com.example.seguimientopeliculas.data.remote.models.Movie
import com.example.seguimientopeliculas.databinding.FragmentEditMovieBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditMovieFragment : Fragment() {

    private lateinit var binding: FragmentEditMovieBinding
    private val viewModel: EditMovieViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditMovieBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar desplegables
        setupDropdowns()

        // Obtener los argumentos
        val movieId = arguments?.getInt("movieId") ?: -1
        val movieTitle = arguments?.getString("movieTitle") ?: ""
        val movieGenre = arguments?.getString("movieGenre") ?: ""
        val movieRating = arguments?.getInt("movieRating") ?: 0
        val moviePremiere = arguments?.getBoolean("moviePremiere") ?: false
        val movieStatus = arguments?.getString("movieStatus") ?: ""
        val movieComments = arguments?.getString("movieComments") ?: ""

        if (movieId == -1) {
            Toast.makeText(requireContext(), "Película no encontrada", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        // Prellenar los datos de la película
        binding.editMovieTitle.setText(movieTitle)
        binding.editMovieGenre.setText(movieGenre, false) // False para evitar problemas con el adaptador
        binding.editMovieRating.setText(movieRating.toString())
        binding.editMoviePremiereSwitch.isChecked = moviePremiere
        binding.editMovieStatus.setText(movieStatus, false) // False para evitar problemas con el adaptador
        binding.editMovieComments.setText(movieComments)

        // Configuración de botones
        setupButtons(movieId)
        observeViewModel()
    }

    private fun setupDropdowns() {
        val genres = listOf(
            "Comedia", "Ciencia Ficción", "Acción", "Musical", "Terror", "Aventuras",
            "Drama", "Fantasía", "Suspense", "Animación", "Romántico", "Western", "Documental", "Serie"
        )

        val statuses = listOf("Quiero ver", "Visto")

        val genreAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genres)
        binding.editMovieGenre.setAdapter(genreAdapter)
        binding.editMovieGenre.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.editMovieGenre.showDropDown()
        }

        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statuses)
        binding.editMovieStatus.setAdapter(statusAdapter)
        binding.editMovieStatus.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.editMovieStatus.showDropDown()
        }
    }

    private fun setupButtons(movieId: Int) {
        // Botón para actualizar la película
        binding.updateButton.setOnClickListener {
            if (!validateInputs()) return@setOnClickListener

            val updatedMovie = Movie(
                id = movieId,
                title = binding.editMovieTitle.text.toString(),
                genre = binding.editMovieGenre.text.toString(),
                rating = binding.editMovieRating.text.toString().toIntOrNull() ?: 0,
                premiere = binding.editMoviePremiereSwitch.isChecked,
                status = binding.editMovieStatus.text.toString(),
                comments = binding.editMovieComments.text.toString()
            )
            viewModel.updateMovie(updatedMovie)
        }

        // Botón para eliminar la película
        binding.deleteButton.setOnClickListener {
            viewModel.deleteMovie(movieId)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is EditMovieViewModel.EditMovieUiState.SuccessUpdate -> {
                        showToast("Película actualizada correctamente.")
                        findNavController().navigate(R.id.movieListFragment)
                    }
                    is EditMovieViewModel.EditMovieUiState.SuccessDelete -> {
                        showToast("Película eliminada correctamente.")
                        findNavController().navigate(R.id.movieListFragment)
                    }
                    is EditMovieViewModel.EditMovieUiState.Error -> {
                        showToast("Error: ${state.message}")
                    }
                    EditMovieViewModel.EditMovieUiState.Idle -> {
                        // Puedes mostrar un estado inicial o dejarlo vacío
                    }
                    EditMovieViewModel.EditMovieUiState.Loading -> {
                        binding.loadingIndicator.visibility = View.VISIBLE // Asegúrate de tener este indicador en tu layout
                    }
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        if (binding.editMovieTitle.text.isNullOrBlank()) {
            showToast("El título no puede estar vacío.")
            return false
        }

        if (binding.editMovieGenre.text.isNullOrBlank()) {
            showToast("Por favor, selecciona un género.")
            return false
        }

        if (binding.editMovieRating.text.isNullOrBlank() || binding.editMovieRating.text.toString().toIntOrNull() == null) {
            showToast("Por favor, introduce una calificación válida.")
            return false
        }

        if (binding.editMovieStatus.text.isNullOrBlank()) {
            showToast("Por favor, selecciona un estado.")
            return false
        }

        return true
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
