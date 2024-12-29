package com.example.seguimientopeliculas.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.seguimientopeliculas.databinding.FragmentMovieListBinding
import com.example.seguimientopeliculas.data.Movie
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MovieListEditFragment : Fragment() {

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

        binding.title.text = "SELECCIONA PELÍCULA"
        binding.shareFab.visibility = View.GONE

        // Inicializar el adaptador con clics habilitados para editar
        movieListAdapter = MovieListAdapter { movie ->
            navigateToEditMovie(movie)
        }

        // Configuramos el RecyclerView
        setupRecyclerView()

        // Cargar las películas desde el ViewModel
        lifecycleScope.launch {
            viewModel.loadMovies(requireContext())
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

    private fun navigateToEditMovie(movie: Movie) {
        try {
            val action = MovieListEditFragmentDirections.actionMovieListEditFragmentToEditMovieFragment(
                movieId = movie.id,
                movieTitle = movie.title,
                movieGenre = movie.genre,
                movieRating = movie.rating,
                moviePremiere = movie.premiere,
                movieStatus = movie.status,
                movieComments = movie.comments
            )
            findNavController().navigate(action)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        movieListAdapter = null // Liberamos el adaptador para evitar fugas de memoria
    }
}
