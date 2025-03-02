package com.example.seguimientopeliculas.ui.movies.add

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.seguimientopeliculas.data.remote.models.Movie
import com.example.seguimientopeliculas.databinding.FragmentAddMoviesBinding

class AddMoviesAdapter(
    private val onItemClick: (Movie) -> Unit
) : ListAdapter<Movie, AddMoviesAdapter.AddMovieViewHolder>(MovieDiffCallback()) {

    inner class AddMovieViewHolder(
        private val binding: FragmentAddMoviesBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {

            binding.titleInput.setText(movie.title)
            binding.genreDropdown.setText(movie.genre)
            binding.ratingInput.setText(movie.rating.toString())
            binding.premiereSwitch.isChecked = movie.premiere
            binding.statusDropdown.setText(movie.status)
            binding.commentsInput.setText(movie.comments)

            binding.saveButton.setOnClickListener {
                onItemClick(movie)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddMovieViewHolder {
        val binding = FragmentAddMoviesBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AddMovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddMovieViewHolder, position: Int) {
        val movie = getItem(position)
        holder.bind(movie)
    }
}

class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
    override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
        return oldItem.title == newItem.title
    }

    override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean {
        return oldItem == newItem
    }
}
