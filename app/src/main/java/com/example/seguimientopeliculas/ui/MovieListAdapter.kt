package com.example.seguimientopeliculas.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.seguimientopeliculas.data.Movie
import com.example.seguimientopeliculas.databinding.MovieItemBinding

class MovieListAdapter(
    private val onMovieClick: ((Movie) -> Unit)? = null// Callback para manejar el clic en una película
) : ListAdapter<Movie, MovieListAdapter.MovieViewHolder>(MovieComparer) {

    inner class MovieViewHolder(
        private val binding: MovieItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            // Asignamos los valores del objeto Movie a los TextViews
            binding.movieTitle.text = "Título: ${movie.title}"
            binding.movieGenre.text = "Género: ${movie.genre}"
            binding.movieRating.text = "Calificación: ${movie.rating}"  // Convertimos el rating a texto
            binding.movieStatus.text = "Estado: ${movie.status}"
            binding.moviePremiere.text = "Estreno: ${if (movie.premiere) "Sí" else "No"}"
            binding.movieComments.text = "Comentarios: ${movie.comments ?: "Sin comentarios"}"

            // Configuramos el evento de clic en el elemento del adaptador
            binding.root.setOnClickListener {
                Log.d("MovieListAdapter", "Clic registrado en la película: ${movie.title}")
                onMovieClick?.invoke(movie)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = MovieItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = getItem(position)
        holder.bind(movie)
    }

    object MovieComparer : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return oldItem == newItem
        }
    }
}