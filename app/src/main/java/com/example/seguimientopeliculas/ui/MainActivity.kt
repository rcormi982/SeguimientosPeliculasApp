package com.example.seguimientopeliculas.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.seguimientopeliculas.R
import com.example.seguimientopeliculas.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener NavController correctamente
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.main) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = binding.bottomNavigationView

        // Configurar BottomNavigationView con NavController
        bottomNavigationView.setupWithNavController(navController)

        // Manejar la visibilidad de BottomNavigationView y FAB
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.welcomeFragment, R.id.loginFragment, R.id.registerFormFragment -> {
                    bottomNavigationView.visibility = View.GONE
                    binding.fabAddMovie.visibility = View.GONE
                }
                R.id.showMoviesFragment, R.id.movieListFragment -> {
                    bottomNavigationView.visibility = View.VISIBLE
                    binding.fabAddMovie.visibility = View.VISIBLE
                }
                else -> {
                    bottomNavigationView.visibility = View.VISIBLE
                    binding.fabAddMovie.visibility = View.GONE
                }
            }
        }

        // Manejar clics en los ítems del BottomNavigationView
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_show_movies -> {
                    Log.d("MainActivity", "Navegando a ShowMoviesFragment")
                    navController.navigate(R.id.movieListFragment)
                    true
                }
                R.id.nav_edit -> {
                    Log.d("MainActivity", "Navegando a MovieListEditFragment")
                    navController.navigate(R.id.movieListEditFragment)
                    true
                }
                R.id.nav_profile -> {
                    Log.d("MainActivity", "Navegando a ProfileFragment (comportamiento no implementado)")
                    navController.navigate(R.id.profileFragment)
                    true
                }
                else -> false
            }
        }

        // Configurar Floating Action Button (FAB)
        binding.fabAddMovie.setOnClickListener {
            try {
                val currentDestination = navController.currentDestination?.id
                when (currentDestination) {
                    R.id.showMoviesFragment -> {
                        Log.d("MainActivity", "Navegando de ShowMoviesFragment a AddMoviesFragment")
                        navController.navigate(R.id.action_showMoviesFragment_to_addMoviesFragment)
                    }
                    R.id.movieListFragment -> {
                        Log.d("MainActivity", "Navegando de MovieListFragment a AddMoviesFragment")
                        navController.navigate(R.id.action_movieListFragment_to_addMoviesFragment)
                    }
                    else -> {
                        Log.d("MainActivity", "El FAB no tiene acción definida en este fragmento")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al manejar el clic del FAB: ${e.message}", e)
            }
        }
    }
}