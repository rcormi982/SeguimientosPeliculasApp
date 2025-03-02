package com.example.seguimientopeliculas.ui.maps

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.seguimientopeliculas.BuildConfig
import com.example.seguimientopeliculas.R
import com.example.seguimientopeliculas.data.remote.api.GooglePlacesService
import com.example.seguimientopeliculas.databinding.FragmentMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.provider.Settings


@AndroidEntryPoint
class MapsFragment : Fragment(), OnMapReadyCallback {
    private lateinit var binding: FragmentMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private lateinit var googleMap: GoogleMap

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            getCurrentLocationAndSearchCinemas()
        } else {
            Toast.makeText(
                requireContext(),
                "Se requieren permisos de ubicación para encontrar cines cercanos",
                Toast.LENGTH_SHORT
            ).show()
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

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.MAPS_API_KEY)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        placesClient = Places.createClient(requireContext())

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        checkLocationPermissions()
    }

    private fun checkLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocationAndSearchCinemas()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("Se necesitan permisos de ubicación")
                    .setMessage("Esta app necesita acceso a la ubicación para encontrar cines cercanos")
                    .setPositiveButton("OK") { _, _ -> requestLocationPermissions() }
                    .create()
                    .show()
            }

            else -> requestLocationPermissions()
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

    private fun getCurrentLocationAndSearchCinemas() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        googleMap.isMyLocationEnabled = true

        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isGpsEnabled) {
            showEnableLocationDialog()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                searchNearbyCinemas()
            } ?: run {
                showEnableLocationDialog()
            }
        }.addOnFailureListener {
            showEnableLocationDialog()
        }
    }

    private fun showEnableLocationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Ubicación desactivada")
            .setMessage("Para encontrar cines cercanos, necesitas activar la ubicación. ¿Deseas activarla?")
            .setPositiveButton("Activar") { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(requireContext(), "No se activó la ubicación", Toast.LENGTH_SHORT).show()
            }
            .show()
    }


    private fun searchNearbyCinemas() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)

                val apiKey = BuildConfig.MAPS_API_KEY

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://maps.googleapis.com/maps/api/place/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service = retrofit.create(GooglePlacesService::class.java)

                lifecycleScope.launch {
                    try {
                        val response = withContext(Dispatchers.IO) {
                            service.getNearbyCinemas(
                                "${currentLatLng.latitude},${currentLatLng.longitude}",
                                20000,
                                "movie_theater",
                                "cinema|movie theater",
                                apiKey
                            )
                        }

                        if (response.isSuccessful) {
                            val placesResponse = response.body()
                            val results = placesResponse?.results ?: emptyList()

                            if (results.isEmpty()) {
                                Toast.makeText(
                                    requireContext(),
                                    "No se encontraron cines cercanos",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }

                            val cinemaList = results.map { "${it.name} - ${it.vicinity}" }

                            // Mostrar los cines en la lista
                            val adapter = ArrayAdapter(
                                requireContext(),
                                android.R.layout.simple_list_item_1,
                                cinemaList
                            )
                            binding.listCines.adapter = adapter

                            // Añadir marcadores al mapa
                            results.forEach { place ->
                                place.geometry?.location?.let {
                                    googleMap.addMarker(
                                        MarkerOptions()
                                            .position(LatLng(it.lat, it.lng))
                                            .title(place.name)
                                    )
                                }
                            }

                            // Mover la cámara al primer cine encontrado
                            results.firstOrNull()?.geometry?.location?.let {
                                googleMap.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(it.lat, it.lng), 12f
                                    )
                                )
                            }
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Error en la respuesta de la API",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            requireContext(),
                            "Error en la solicitud: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "No se pudo obtener la ubicación actual",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}