package com.example.financeapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.google.maps.android.compose.*

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyBanksScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val green = Color(0xFF2E7D32)

    var userLocation by remember {
        mutableStateOf<LatLng?>(null)
    }

    var nearbyPlaces by remember {
        mutableStateOf<List<Place>>(emptyList())
    }

    var selectedFilter by remember {
        mutableStateOf("All")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    // Default location until GPS is available
    val defaultLocation = LatLng(35.8456, -86.3903)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            defaultLocation, 13f
        )
    }

    // Check location permissions
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            hasPermission =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (!hasPermission) {
                errorMessage = "Location permission is required."
            }
        }

    // Request permission when the screen opens
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Get the user's current location
    LaunchedEffect(hasPermission) {

        if (hasPermission) {

            val locationClient =
                LocationServices.getFusedLocationProviderClient(context)

            isLoading = true
            errorMessage = ""

            locationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                null
            ).addOnSuccessListener { location ->

                isLoading = false

                if (location != null) {

                    val currentLocation = LatLng(
                        location.latitude,
                        location.longitude
                    )

                    userLocation = currentLocation

                    cameraPositionState.position =
                        CameraPosition.fromLatLngZoom(
                            currentLocation, 14f
                        )

                } else {
                    errorMessage = "Unable to find your location."
                }

            }.addOnFailureListener {
                isLoading = false
                errorMessage = "Unable to access location."
            }
        }
    }

    // Initialize Google Places
    val placesClient = remember {

        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )

        val apiKey = appInfo.metaData
            ?.getString("com.google.android.geo.API_KEY")
            .orEmpty()

        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(
                context.applicationContext,
                apiKey
            )
        }

        Places.createClient(context)
    }

    // Search for nearby banks and ATMs
    LaunchedEffect(userLocation, selectedFilter) {

        val location = userLocation ?: return@LaunchedEffect

        isLoading = true
        errorMessage = ""
        nearbyPlaces = emptyList()

        val searchFilter = selectedFilter

        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.LOCATION,
            Place.Field.FORMATTED_ADDRESS
        )

        val includedTypes = when (searchFilter) {
            "Banks" -> listOf("bank")
            "ATMs" -> listOf("atm")
            else -> listOf("bank", "atm")
        }

        // Search within 5 kilometers
        val bounds = CircularBounds.newInstance(
            location,
            5000.0
        )

        val request = SearchNearbyRequest.builder(
            bounds,
            placeFields
        )
            .setIncludedTypes(includedTypes)
            .setMaxResultCount(20)
            .build()

        placesClient.searchNearby(request)
            .addOnSuccessListener { response ->

                if (selectedFilter == searchFilter &&
                    userLocation == location) {

                    nearbyPlaces = response.places
                    isLoading = false

                    if (nearbyPlaces.isEmpty()) {
                        errorMessage = "No nearby locations found."
                    }
                }

            }
            .addOnFailureListener { exception ->

                if (selectedFilter == searchFilter &&
                    userLocation == location) {

                    isLoading = false
                    errorMessage = exception.message
                        ?: "Unable to find nearby banks."
                }
            }
    }

    // Screen layout
    Scaffold(

        topBar = {
            TopAppBar(
                title = {
                    Text("Nearby Banks & ATMs")
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = green,
                    titleContentColor = Color.White
                )
            )
        }

    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // Filter buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                listOf("All", "Banks", "ATMs").forEach { filter ->

                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = {
                            selectedFilter = filter
                        },
                        label = {
                            Text(filter)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = green,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Loading indicator
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = green
                )
            }

            // Error message
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Allow the user to request permission again
            if (!hasPermission) {
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text("Enable Location")
                }
            }

            // Google Map
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),

                cameraPositionState = cameraPositionState,

                properties = MapProperties(
                    isMyLocationEnabled = hasPermission
                ),

                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = hasPermission
                )

            ) {

                nearbyPlaces.forEach { place ->

                    val position = place.location

                    if (position != null) {

                        key(place.id, position) {
                            Marker(
                                state = rememberMarkerState(
                                    position = position
                                ),
                                title = place.displayName ?: "Bank or ATM",
                                snippet = place.formattedAddress ?: ""
                            )
                        }
                    }
                }
            }

            // Number of locations found
            Surface(
                color = Color(0xFFE8F5E9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "${nearbyPlaces.size} nearby locations found",
                        color = green,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Select a marker to view location details.",
                        color = Color.DarkGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
