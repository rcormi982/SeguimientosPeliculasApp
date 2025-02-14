package com.example.seguimientopeliculas.ui

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.seguimientopeliculas.BuildConfig
import com.example.seguimientopeliculas.R
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
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.AndroidEntryPoint

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
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
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

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                searchNearbyCinemas()
            } ?: run {
                Toast.makeText(requireContext(), "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error al obtener la ubicación", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchNearbyCinemas() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        try {
            val placeFields = listOf(
                Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS,
                Place.Field.LAT_LNG, Place.Field.TYPES
            )

            val request = FindCurrentPlaceRequest.newInstance(placeFields)
            placesClient.findCurrentPlace(request)
                .addOnSuccessListener { response ->
                    val cinemaList = response.placeLikelihoods.mapNotNull { it.place }
                        .filter { it.types?.contains(Place.Type.MOVIE_THEATER) == true }

                    if (cinemaList.isEmpty()) {
                        Toast.makeText(requireContext(), "No se encontraron cines cercanos", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val displayList = cinemaList.map { "${it.name ?: "Sin nombre"} - ${it.address ?: "Sin dirección"}" }
                    binding.listCines.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, displayList)

                    cinemaList.forEach { cinema ->
                        cinema.latLng?.let {
                            googleMap.addMarker(MarkerOptions().position(it).title(cinema.name))
                        }
                    }

                    binding.listCines.setOnItemClickListener { _, _, position, _ ->
                        cinemaList[position].latLng?.let {
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("MapsFragment", "Error al buscar cines", exception)
                    Toast.makeText(requireContext(), "Error al buscar cines: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: SecurityException) {
            Log.e("MapsFragment", "Error de permisos", e)
            Toast.makeText(requireContext(), "Error de permisos al buscar cines", Toast.LENGTH_SHORT).show()
        }
    }
}
