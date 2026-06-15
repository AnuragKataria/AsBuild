package com.rbt.survey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import com.rbt.survey.ui.navigation.AppNavigation
import com.rbt.survey.ui.theme.MyApplicationTheme
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.location.LocationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { _ -> }

            LaunchedEffect(Unit) {
                val permissions = mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.CAMERA
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    permissions.add(Manifest.permission.BLUETOOTH_SCAN)
                    permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                }
                permissionLauncher.launch(permissions.toTypedArray())
            }

            val contet = LocalContext.current

            // 🔹 Track location permission
            var hasLocationPermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        contet,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }

            // 🔹 Launcher for fine location
            val locationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                hasLocationPermission = granted
            }

            // 🔹 Ask ONLY if not granted
            LaunchedEffect(Unit) {
                if (!hasLocationPermission) {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                val backgroundLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { }

                LaunchedEffect(hasLocationPermission) {
                    if (hasLocationPermission) {
                        backgroundLauncher.launch(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
                    }
                }
            }

            MyApplicationTheme {

                val context = LocalContext.current

                fun isLocationEnabled(): Boolean {
                    val locationManager =
                        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

                    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                }

                var isGpsEnabled by remember { mutableStateOf(isLocationEnabled()) }

                LaunchedEffect(Unit) {
                    while (true) {
                        isGpsEnabled = isLocationEnabled()
                        kotlinx.coroutines.delay(2000)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {

                        // ✅ YOUR APP
                        AppNavigation()

                        // 🔴 BLOCK USER IF GPS OFF
                        if (!isGpsEnabled) {
                            AlertDialog(
                                onDismissRequest = {}, // cannot dismiss
                                title = { Text("Location Required") },
                                text = { Text("Please enable location to use this app") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            val intent =
                                                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                            context.startActivity(intent)
                                        }
                                    ) {
                                        Text("Enable Location")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}