package com.example.seguimientopeliculas.ui.movies.list

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.seguimientopeliculas.databinding.FragmentMovieListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MovieListFragment : Fragment() {

    private lateinit var binding: FragmentMovieListBinding
    private val viewModel: MovieListViewModel by viewModels()
    private var movieListAdapter: MovieListAdapter? = null // Inicializamos el adaptador en `onViewCreated`

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMovieListBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.shareFab.setOnClickListener {
            shareMovies()
        }

        // Inicializar el adaptador
        movieListAdapter = MovieListAdapter(requireContext())

        // Configuramos el RecyclerView
        setupRecyclerView()

        // Cargar las películas desde el ViewModel
        lifecycleScope.launch {
            viewModel.loadMovies()
        }

        // Observamos los cambios en el estado de la UI
        observeUiState()
    }

    private fun setupRecyclerView() {
        binding.movieList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = movieListAdapter
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                when (uiState) {
                    MovieListUiState.Loading -> {
                        binding.loadingIndicator.visibility = View.VISIBLE
                        binding.movieList.visibility = View.GONE
                        binding.errorMessage.visibility = View.GONE
                    }
                    is MovieListUiState.Success -> {
                        binding.loadingIndicator.visibility = View.GONE
                        binding.movieList.visibility = View.VISIBLE
                        binding.errorMessage.visibility = View.GONE
                        movieListAdapter?.submitList(uiState.movieList) // Pasamos la lista al adaptador
                    }
                    is MovieListUiState.Error -> {
                        binding.loadingIndicator.visibility = View.GONE
                        binding.movieList.visibility = View.GONE
                        binding.errorMessage.apply {
                            text = uiState.message
                            visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun shareMovies() {
        // Verificar el estado del UI
        val currentUiState = viewModel.uiState.value

        // Generar el mensaje para compartir
        val moviesText = if (currentUiState is MovieListUiState.Success) {
            val moviesListText = currentUiState.movieList.joinToString(separator = "\n") { movie ->
                "Título: ${movie.title}, Género: ${movie.genre}, Calificación: ${movie.rating}"
            }
            "¡Comparto mi lista de películas!\n\n$moviesListText"
        } else {
            when (currentUiState) {
                is MovieListUiState.Error -> "Error: ${currentUiState.message}"
                is MovieListUiState.Loading -> "Cargando películas, inténtalo más tarde."
                else -> "No hay películas para compartir."
            }
        }

        // Crear un intent para compartir
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, moviesText)
        }

        // Mostrar el selector de aplicaciones para compartir
        startActivity(Intent.createChooser(shareIntent, "Compartir películas con:"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        movieListAdapter = null // Liberamos el adaptador para evitar fugas de memoria
    }
}