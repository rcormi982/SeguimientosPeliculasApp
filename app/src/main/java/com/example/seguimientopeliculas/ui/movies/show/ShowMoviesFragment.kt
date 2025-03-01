package com.example.seguimientopeliculas.ui.movies.show

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.seguimientopeliculas.databinding.FragmentShowMoviesBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShowMoviesFragment : Fragment() {

    private lateinit var binding: FragmentShowMoviesBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShowMoviesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}
