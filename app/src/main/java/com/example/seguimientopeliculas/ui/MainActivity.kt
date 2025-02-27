package com.example.seguimientopeliculas.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.seguimientopeliculas.R
import com.example.seguimientopeliculas.databinding.ActivityMainBinding
import com.example.seguimientopeliculas.workers.WorkManagerHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var workManagerHelper: WorkManagerHelper

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Notifications", "Permiso de notificaciones concedido")
        } else {
            Log.d("Notifications", "Permiso de notificaciones denegado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Solicitar permiso de notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionIfNeeded()
        }

        checkUserLoggedInAndSetupSync()

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
                    navController.navigate(R.id.movieListFragment)
                    true
                }
                R.id.nav_edit -> {
                    navController.navigate(R.id.movieListEditFragment)
                    true
                }
                R.id.nav_profile -> {
                    navController.navigate(R.id.profileFragment)
                    true
                }
                R.id.nav_map -> {
                    navController.navigate(R.id.mapsFragment)
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
                        navController.navigate(R.id.action_showMoviesFragment_to_addMoviesFragment)
                    }
                    R.id.movieListFragment -> {
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

    private fun requestNotificationPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun checkUserLoggedInAndSetupSync() {
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            Log.d("MainActivity", "Usuario logueado, programando sincronización")
            workManagerHelper.schedulePeriodicalSync()
        } else {
            Log.d("MainActivity", "Usuario no logueado")
        }
    }
}