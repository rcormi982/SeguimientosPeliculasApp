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
import android.view.WindowInsetsAnimation
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.privacysandbox.tools.core.model.Method
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.seguimientopeliculas.BuildConfig
import com.example.seguimientopeliculas.R
import com.example.seguimientopeliculas.data.remote.GooglePlacesService
import com.example.seguimientopeliculas.data.remote.PlacesResponse
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
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                searchNearbyCinemas()
            } ?: run {
                Toast.makeText(
                    requireContext(),
                    "No se pudo obtener la ubicación actual",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error al obtener la ubicación", Toast.LENGTH_SHORT)
                .show()
        }
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

                // Construir la URL de la API de Google Places (Nearby Search)
                val apiKey = BuildConfig.MAPS_API_KEY
                val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                        "location=${currentLatLng.latitude},${currentLatLng.longitude}" +
                        "&radius=20000" +  // 20 km en metros
                        "&type=cinema" +  // Buscar solo cines
                        "&key=$apiKey"

                // Configurar Retrofit
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://maps.googleapis.com/maps/api/place/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service = retrofit.create(GooglePlacesService::class.java)
                val call = service.getNearbyCinemas(
                    "${currentLatLng.latitude},${currentLatLng.longitude}",
                    20000, // 20km
                    "movie_theater",
                    "cinema",
                    apiKey
                )

                call.enqueue(object : retrofit2.Callback<PlacesResponse> {
                    override fun onResponse(
                        call: Call<PlacesResponse>,
                        response: Response<PlacesResponse>
                    ) {
                        if (response.isSuccessful) {
                            val placesResponse = response.body()
                            val results = placesResponse?.results ?: emptyList()

                            if (results.isEmpty()) {
                                Toast.makeText(
                                    requireContext(),
                                    "No se encontraron cines cercanos",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return
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
                                        LatLng(
                                            it.lat,
                                            it.lng
                                        ), 12f
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
                    }

                    override fun onFailure(call: Call<PlacesResponse>, t: Throwable) {
                        Toast.makeText(
                            requireContext(),
                            "Error en la solicitud: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
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