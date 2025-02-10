package com.example.seguimientopeliculas.ui

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.seguimientopeliculas.BuildConfig
import com.example.seguimientopeliculas.databinding.FragmentMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceLikelihood
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.AndroidEntryPoint
import java.util.jar.Manifest

@AndroidEntryPoint
class MapsFragment : Fragment() {
    private lateinit var binding: FragmentMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient

    // Launcher para solicitar permisos de ubicación
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                // Permiso de ubicación precisa concedido
                searchNearbyCinemas()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // Permiso de ubicación aproximada concedido
                searchNearbyCinemas()
            }
            else -> {
                // Permisos denegados
                Toast.makeText(
                    requireContext(),
                    "Location permissions are required to find nearby cinemas",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Places if not already initialized
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.MAPS_API_KEY)
        }

        // Initialize clients
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        placesClient = Places.createClient(requireContext())

        // Check and request location permissions
        checkLocationPermissions()
    }

    private fun checkLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso ya concedido
                searchNearbyCinemas()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Mostrar explicación de por qué se necesitan los permisos
                AlertDialog.Builder(requireContext())
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs location access to find nearby cinemas")
                    .setPositiveButton("OK") { _, _ ->
                        requestLocationPermissions()
                    }
                    .create()
                    .show()
            }
            else -> {
                // Solicitar permisos
                requestLocationPermissions()
            }
        }
    }

    private fun requestLocationPermissions() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun searchNearbyCinemas() {
        try {
            // Verificar permisos antes de solicitar la ubicación
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                checkLocationPermissions()
                return
            }

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val request = FindCurrentPlaceRequest.newInstance(
                            listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
                        )

                        val placeResponse = placesClient.findCurrentPlace(request)
                        placeResponse.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val response = task.result
                                val cinemas = response.placeLikelihoods
                                    .filter { it.place.types?.contains(Place.Type.MOVIE_THEATER) == true }
                                    .sortedByDescending { it.likelihood }

                                displayCinemas(cinemas)
                            }
                        }
                    } ?: run {
                        Toast.makeText(
                            requireContext(),
                            "Unable to get current location",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Error getting location",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } catch (e: SecurityException) {
            Toast.makeText(
                requireContext(),
                "Location permission denied",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun displayCinemas(cinemas: List<PlaceLikelihood>) {
        if (cinemas.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "No cinemas found nearby",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            cinemas.map { "${it.place.name} - ${it.place.address ?: "No address"}" }
        )

        binding.listCines.adapter = adapter

        binding.listCines.setOnItemClickListener { _, _, position, _ ->
            val selectedCinema = cinemas[position]
            Toast.makeText(
                requireContext(),
                "Cinema: ${selectedCinema.place.name}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}