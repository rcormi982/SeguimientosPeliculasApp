package com.example.seguimientopeliculas.ui.movies.list

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.seguimientopeliculas.R
import com.example.seguimientopeliculas.data.remote.models.Movie
import com.example.seguimientopeliculas.databinding.MovieItemBinding
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MovieListAdapter @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val onMovieClick: ((Movie) -> Unit)? = null,// Callback para manejar el clic en una película

) : ListAdapter<Movie, MovieListAdapter.MovieViewHolder>(MovieComparer) {

    inner class MovieViewHolder(
        private val binding: MovieItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("StringFormatMatches")
        fun bind(movie: Movie) {
            // Asignamos los valores del objeto Movie a los TextViews
            binding.movieTitle.text = "Título: ${movie.title}"
            binding.movieTitle.text = context.getString(R.string.title, movie.title)
            binding.movieGenre.text = "Género: ${movie.genre}"
            binding.movieGenre.text = context.getString(R.string.genre, movie.genre)
            binding.movieRating.text = "Calificación: ${movie.rating}"  // Convertimos el rating a texto
            binding.movieRating.text = context.getString(R.string.rating, movie.rating)
            binding.movieStatus.text = "Estado: ${movie.status}"
            binding.movieStatus.text = context.getString(R.string.status, movie.status)
            binding.moviePremiere.text = "Estreno: ${if (movie.premiere) "Sí" else "No"}"
            binding.moviePremiere.text = context.getString(R.string.premiere, if (movie.premiere) "Sí" else "No")
            binding.movieComments.text = "Comentarios: ${movie.comments ?: "Sin comentarios"}"
            binding.movieComments.text = context.getString(R.string.comments, movie.comments ?: "Sin comentarios")

            // Configuramos el evento de clic en el elemento del adaptador
            binding.root.setOnClickListener {
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