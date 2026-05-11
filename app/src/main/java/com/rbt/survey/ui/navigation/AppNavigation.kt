package com.rbt.survey.ui.navigation

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rbt.survey.data.local.UserPreferences
import com.rbt.survey.data.local.db.AppDatabase
import com.rbt.survey.dgps.DgpsManager
import com.rbt.survey.data.remote.RetrofitClient
import com.rbt.survey.data.repository.AuthRepository
import com.rbt.survey.data.repository.FormRepository
import com.rbt.survey.data.repository.GeoRepository
import com.rbt.survey.ui.form.FormDataCollectionScreen
import com.rbt.survey.ui.form.FormDataCollectionViewModel
import com.rbt.survey.ui.form.FormDataCollectionViewModelFactory
import com.rbt.survey.ui.surveyDashboard.SurveyDashboardScreen
import com.rbt.survey.ui.surveyDashboard.SurveyDashboardViewModel
import com.rbt.survey.ui.surveyDashboard.SurveyDashboardViewModelFactory
import com.rbt.survey.ui.login.LoginScreen
import com.rbt.survey.ui.login.LoginViewModel
import com.rbt.survey.ui.login.LoginViewModelFactory
import com.rbt.survey.ui.map.MapScreen as GpMapScreen
import com.rbt.survey.ui.form.MapScreen as FieldMapScreen
import com.rbt.survey.ui.map.MapViewModel
import com.rbt.survey.ui.map.MapViewModelFactory
import com.rbt.survey.ui.dgps.DgpsViewModel
import com.rbt.survey.ui.dgps.DgpsViewModelFactory
import com.rbt.survey.ui.dgps.BluetoothDeviceListScreen
import com.rbt.survey.ui.dgps.BaseModeSettingsScreen
import com.rbt.survey.ui.dgps.DeviceInformationScreen
import com.rbt.survey.ui.dgps.DeviceSettingsScreen
import com.rbt.survey.ui.dgps.DgpsHomeScreen
import com.rbt.survey.ui.dgps.GnssSystemScreen
import com.rbt.survey.ui.dgps.InspectionAccuracyScreen
import com.rbt.survey.ui.dgps.NmeaSettingsScreen
import com.rbt.survey.ui.dgps.PoleCalibrationScreen
import com.rbt.survey.ui.dgps.PositionInformationScreen
import com.rbt.survey.ui.dgps.RoverModeSettingsScreen
import com.rbt.survey.ui.dgps.StaticSurveySettingsScreen
import com.rbt.survey.ui.splash.SplashScreen
import java.net.URLDecoder
import java.net.URLEncoder

import androidx.work.*
import com.rbt.survey.location.LocationService
import com.rbt.survey.ui.dashboard.DashboardScreen
import com.rbt.survey.ui.locationTrackingDashboard.LocationTrackingScreen
import com.rbt.survey.ui.locationTrackingDashboard.LocationTrackingViewModel
import com.rbt.survey.ui.locationTrackingDashboard.LocationTrackingViewModelFactory
import com.rbt.survey.worker.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object SurveyDashboard  : Screen("survey_dashboard")
    object Dashboard : Screen("dashboard")

    object LocationTracking : Screen("location_tracking")
    object FormDataCollection : Screen("form_data/{formId}?blockCode={blockCode}&gpName={gpName}&surveyRadius={surveyRadius}&submissionId={submissionId}") {
        fun createRoute(formId: Int, blockCode: String?, gpName: String?,surveyRadius: Int?, submissionId: Int? = null) = "form_data/$formId?blockCode=${blockCode ?: ""}&gpName=${gpName ?: ""}&surveyRadius=${surveyRadius ?: -1}&submissionId=${submissionId ?: -1}"
    }
    object GPMap : Screen("gp_map/{formId}/{blockCode}") {
        fun createRoute(formId: Int, blockCode: String) =
            "gp_map/$formId/$blockCode"
    }

    object FormMap : Screen("form_map/{type}/{fieldId}/{initialValue}?radius={radius}") {
        fun createRoute(type: String, fieldId: String, initialValue: String, radius: Int?) =
            "form_map/$type/$fieldId/${URLEncoder.encode(initialValue, "UTF-8")}?radius=${radius ?: -1}"
    }

    object DgpsSettings : Screen("dgps_settings")
    object DgpsRover : Screen("dgps_rover")
    object DgpsBase : Screen("dgps_base")
    object DgpsStatic : Screen("dgps_static")
    object DgpsInspection : Screen("dgps_inspection")
    object DgpsPoleCalibration : Screen("dgps_pole_calibration")
    object DgpsDeviceInformation : Screen("dgps_device_information")
    object DgpsDeviceSettings : Screen("dgps_device_settings")
    object DgpsNmeaSettings : Screen("dgps_nmea_settings")
    object DgpsPositionInformation : Screen("dgps_position_information")
    object DgpsGnssSystem : Screen("dgps_gnss_system")
    object SatelliteView : Screen("satellite_view")
    object BluetoothDeviceList : Screen("bluetooth_device_list")
}


@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val preferences = UserPreferences(context)
    val dgpsManager = remember { DgpsManager(context) }
    val isLoggedInState by preferences.authToken.collectAsState(initial = "LOADING")

    LaunchedEffect(isLoggedInState) {
        if (isLoggedInState != null && isLoggedInState != "LOADING") {

            val intent = Intent(context, LocationService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    // Setup Background Sync
    LaunchedEffect(Unit) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "OfflineSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    // 🔥 Auto-connect DGPS on app start
    LaunchedEffect(Unit) {
        val useDgps = preferences.useDgps.first()
        if (useDgps) {
            val address = preferences.dgpsDeviceAddress.first()
            val useCors = preferences.useCors.first()
            val host = preferences.corsHost.first()
            val port = preferences.corsPort.first()
            val mountpoint = preferences.corsMountpoint.first()
            val user = preferences.corsUser.first()
            val pass = preferences.corsPass.first()

            if (address != null && address.isNotEmpty()) {
                dgpsManager.connect(
                    address,
                    if (useCors) host else null,
                    if (useCors) port?.toIntOrNull() else null,
                    if (useCors) mountpoint else null,
                    if (useCors) user else null,
                    if (useCors) pass else null
                )
            }
        }
    }

    if (isLoggedInState == "LOADING") {
        // Show a simple blank screen while loading preferences to avoid flickering
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val isLoggedIn = isLoggedInState != null
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    isLoggedIn = isLoggedIn,
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                val repository = AuthRepository(RetrofitClient.getAuthApi(context))
                val viewModel: LoginViewModel = viewModel(
                    factory = LoginViewModelFactory(repository, preferences)
                )
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onInventoryClick = {
                        // TODO: navigate when ready
                    },
                    onSurveyClick = {
                        navController.navigate(Screen.SurveyDashboard.route)
                    },
                    onLocationTrackingClick = {
                        navController.navigate(Screen.LocationTracking.route)
                    },
                    onLogout = {
                        CoroutineScope(Dispatchers.Main).launch {
                            // clear auth
                            preferences.clearAuthData()

                            // stop service
                            val intent = Intent(context, LocationService::class.java)
                            context.stopService(intent)

                            // navigate
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onNavigateToDgpsSettings = {
                        navController.navigate(Screen.DgpsSettings.route)
                    }
                )
            }

            composable(Screen.SurveyDashboard.route) {
                val database = AppDatabase.getDatabase(context)
                val authApi = remember {
                    RetrofitClient.getAuthenticatedApi(context, preferences)
                }
                val geoApi = remember {
                    RetrofitClient.getGeoApi(context, preferences)
                }
                val repository = remember {
                    FormRepository(
                        authApi, 
                        database.formDraftDao(), 
                        database.offlineSubmissionDao(),
                        database.cachedFormDao(),
                        database.cachedFormDetailDao(),
                        database.pendingFileUploadDao()
                    )
                }
                val geoRepository = remember {
                    GeoRepository(
                        geoApi,
                        database.cachedBlockAssignmentDao(),
                        database.cachedBlockSummaryDao(),
                        database.cachedUploadedSubmissionDao(),
                        database.locationDao()
                    )
                }
                val parentEntry = remember {
                    navController.getBackStackEntry(Screen.SurveyDashboard.route)
                }

                val viewModel: SurveyDashboardViewModel = viewModel(
                    parentEntry,
                    factory = SurveyDashboardViewModelFactory(repository, geoRepository, preferences)
                )
                SurveyDashboardScreen(
                    viewModel = viewModel,

                    onNavigateToMap = { formId, blockCode ->
                        if (blockCode != null) {
                            navController.navigate(
                                Screen.GPMap.createRoute(formId, blockCode)
                            ) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onNavigateToEditOfflineSubmission = { formId, submissionId, blockCode, gpName, radius ->
                        val encodedGpName = URLEncoder.encode(gpName ?: "", "UTF-8")
                        navController.navigate(
                            Screen.FormDataCollection.createRoute(formId, blockCode, encodedGpName, radius, submissionId)
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToDgpsSettings = {
                        navController.navigate(Screen.DgpsSettings.route)
                    }
                )
            }

            composable(Screen.LocationTracking.route) {

                val database = AppDatabase.getDatabase(context)

                val geoApi = remember {
                    RetrofitClient.getGeoApi(context, preferences)
                }

                val geoRepository = remember {

                    GeoRepository(
                        geoApi,
                        database.cachedBlockAssignmentDao(),
                        database.cachedBlockSummaryDao(),
                        database.cachedUploadedSubmissionDao(),
                        database.locationDao()
                    )
                }

                val viewModel: LocationTrackingViewModel = viewModel(

                    factory = LocationTrackingViewModelFactory(
                        geoRepository
                    )
                )

                LocationTrackingScreen(

                    viewModel = viewModel,

                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.DgpsSettings.route) {
                val dgpsViewModel: DgpsViewModel = viewModel(
                    factory = DgpsViewModelFactory(preferences, dgpsManager)
                )
                DgpsHomeScreen(
                    viewModel = dgpsViewModel,
                    onBack = { navController.popBackStack() },
                    onCommunicationClick = { navController.navigate(Screen.BluetoothDeviceList.route) },
                    onRoverClick = { navController.navigate(Screen.DgpsRover.route) },
                    onBaseClick = { navController.navigate(Screen.DgpsBase.route) },
                    onStaticClick = { navController.navigate(Screen.DgpsStatic.route) },
                    onInspectionAccuracyClick = { navController.navigate(Screen.DgpsInspection.route) },
                    onDeviceInformationClick = { navController.navigate(Screen.DgpsDeviceInformation.route) },
                    onDeviceSettingsClick = { navController.navigate(Screen.DgpsDeviceSettings.route) },
                    onNmeaSettingsClick = { navController.navigate(Screen.DgpsNmeaSettings.route) },
                    onPositionInformationClick = { navController.navigate(Screen.DgpsPositionInformation.route) },
                    onGnssSystemClick = { navController.navigate(Screen.DgpsGnssSystem.route) }
                )
            }

            composable(Screen.BluetoothDeviceList.route) {
                val parentEntry = remember {
                    navController.getBackStackEntry(Screen.DgpsSettings.route)
                }
                val dgpsViewModel: DgpsViewModel = viewModel(
                    parentEntry,
                    factory = DgpsViewModelFactory(preferences, dgpsManager)
                )
                BluetoothDeviceListScreen(
                    viewModel = dgpsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.DgpsRover.route) {
                val parentEntry = remember {
                    navController.getBackStackEntry(Screen.DgpsSettings.route)
                }
                val dgpsViewModel: DgpsViewModel = viewModel(
                    parentEntry,
                    factory = DgpsViewModelFactory(preferences, dgpsManager)
                )
                RoverModeSettingsScreen(
                    viewModel = dgpsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.DgpsBase.route) {
                val parentEntry = remember {
                    navController.getBackStackEntry(Screen.DgpsSettings.route)
                }
                val dgpsViewModel: DgpsViewModel = viewModel(
                    parentEntry,
                    factory = DgpsViewModelFactory(preferences, dgpsManager)
                )
                BaseModeSettingsScreen(
                    viewModel = dgpsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.DgpsStatic.route) {
                val parentEntry = remember {
                    navController.getBackStackEntry(Screen.DgpsSettings.route)
                }
                val dgpsViewModel: DgpsViewModel = viewModel(
                    parentEntry,
                    factory = DgpsViewModelFactory(preferences, dgpsManager)
                )
                StaticSurveySettingsScreen(
                    viewModel = dgpsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.DgpsInspection.route) {
                val parentEntry = remember {
                    navController.getBackStackEntry(Screen.DgpsSettings.route)
                }
                val dgpsViewModel: DgpsViewModel = viewModel(
                    parentEntry,
                    factory = DgpsViewModelFactory(preferences, dgpsManager)
                )
                InspectionAccuracyScreen(
                    viewModel = dgpsViewModel,
                    onBack = { navController.popBackStack() },
                    onPoleCalibrationClick = { navController.navigate(Screen.DgpsPoleCalibration.route) }
                )
            }

            composable(Screen.DgpsPoleCalibration.route) {
                val parentEntry = remember {
                    navController.getBackStackEntry(Screen.DgpsSettings.route)
                }
                val dgpsViewModel: DgpsViewModel = viewModel(
                    parentEntry,
                    factory = DgpsViewModelFactory(preferences, dgpsManager)
                )
                PoleCalibrationScreen(
                    viewModel = dgpsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.DgpsDeviceInformation.route) {
                val parentEntry = remember {
                    navController.getBackStackEntry(Screen.DgpsSettings.route)
                }
                val dgpsViewModel: DgpsViewModel = viewModel(
                    parentEntry,
                    factory = DgpsViewModelFactory(preferences, dgpsManager)
                )
                DeviceInformationScreen(
                    viewModel = dgpsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.DgpsDeviceSettings.route) {
                val parentEntry = remember {
                    navController.getBackStackEntry(Screen.DgpsSettings.route)
                }
                val dgpsViewModel: DgpsViewModel = viewModel(
                    parentEntry,
                    factory = DgpsViewModelFactory(preferences, dgpsManager)
                )
                DeviceSettingsScreen(
                    viewModel = dgpsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.DgpsNmeaSettings.route) {
                val parentEntry = remember {
                    navController.getBackStackEntry(Screen.DgpsSettings.route)
                }
                val dgpsViewModel: DgpsViewModel = viewModel(
                    parentEntry,
                    factory = DgpsViewModelFactory(preferences, dgpsManager)
                )
                NmeaSettingsScreen(
                    viewModel = dgpsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.DgpsPositionInformation.route) {
                val parentEntry = remember {
                    navController.getBackStackEntry(Screen.DgpsSettings.route)
                }
                val dgpsViewModel: DgpsViewModel = viewModel(
                    parentEntry,
                    factory = DgpsViewModelFactory(preferences, dgpsManager)
                )
                PositionInformationScreen(
                    viewModel = dgpsViewModel,
                    onBack = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(Screen.DgpsGnssSystem.route) }
                )
            }

            composable(Screen.DgpsGnssSystem.route) {
                val parentEntry = remember {
                    navController.getBackStackEntry(Screen.DgpsSettings.route)
                }
                val dgpsViewModel: DgpsViewModel = viewModel(
                    parentEntry,
                    factory = DgpsViewModelFactory(preferences, dgpsManager)
                )
                GnssSystemScreen(
                    viewModel = dgpsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.SatelliteView.route) {
                val parentEntry = remember {
                    navController.getBackStackEntry(Screen.DgpsSettings.route)
                }
                val dgpsViewModel: DgpsViewModel = viewModel(
                    parentEntry,
                    factory = DgpsViewModelFactory(preferences, dgpsManager)
                )
                PositionInformationScreen(
                    viewModel = dgpsViewModel,
                    onBack = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(Screen.DgpsGnssSystem.route) }
                )
            }

            composable(
                route = Screen.FormDataCollection.route,
                arguments = listOf(
                    navArgument("formId") { type = NavType.IntType },
                    navArgument("blockCode") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("gpName") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("surveyRadius") {
                        type = NavType.IntType
                        defaultValue = -1
                    },
                    navArgument("submissionId") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { backStackEntry ->
                val formId = backStackEntry.arguments?.getInt("formId") ?: 0
                val blockCode = backStackEntry.arguments?.getString("blockCode")
                val gpNameEncoded = backStackEntry.arguments?.getString("gpName")
                val radius = backStackEntry.arguments?.getInt("surveyRadius")
                val gpName = URLDecoder.decode(gpNameEncoded ?: "", "UTF-8")
                val submissionIdArg = backStackEntry.arguments?.getInt("submissionId") ?: -1
                val submissionId = if (submissionIdArg != -1) submissionIdArg else null
                val database = AppDatabase.getDatabase(context)
                val authApi = RetrofitClient.getAuthenticatedApi(context, preferences)
                val repository = FormRepository(
                    authApi, 
                    database.formDraftDao(), 
                    database.offlineSubmissionDao(),
                    database.cachedFormDao(),
                    database.cachedFormDetailDao(),
                    database.pendingFileUploadDao()
                )
                val viewModel: FormDataCollectionViewModel = viewModel(
                    factory = FormDataCollectionViewModelFactory(
                        formId,
                        blockCode,
                        repository,
                        preferences,
                        gpName,
                        dgpsManager,
                        submissionId,
                        radius
                    )
                )
                FormDataCollectionScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToMap = { type, fieldId, initialValue, radius ->
                        navController.navigate(
                            Screen.FormMap.createRoute(
                                type,
                                fieldId,
                                initialValue,
                                radius
                            )
                        )
                    },
                    navController = navController,
                    onSubmitSuccess = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.GPMap.route,
                arguments = listOf(
                    navArgument("formId") { type = NavType.IntType },
                    navArgument("blockCode") { type = NavType.StringType }
                )
            ) { backStackEntry ->

                val formId = backStackEntry.arguments?.getInt("formId") ?: 0
                val blockCode = backStackEntry.arguments?.getString("blockCode")

                val authApi = RetrofitClient.getAuthenticatedApi(context, preferences)
                val database = AppDatabase.getDatabase(context)

                val repository = FormRepository(
                    authApi,
                    database.formDraftDao(),
                    database.offlineSubmissionDao(),
                    database.cachedFormDao(),
                    database.cachedFormDetailDao(),
                    database.pendingFileUploadDao()
                )


                val viewModel: MapViewModel = viewModel(
                    factory = MapViewModelFactory(repository)
                )

                val parentEntry = remember {
                    navController.getBackStackEntry(Screen.SurveyDashboard.route)
                }
                val surveyDashboardViewModel: SurveyDashboardViewModel = viewModel(parentEntry)

                val surveyRadius by surveyDashboardViewModel.selectedSurveyRadius.collectAsState()

                GpMapScreen(
                    viewModel = viewModel,
                    formId = formId,
                    blockCode = blockCode,
                    gpStatusList = surveyDashboardViewModel.selectedGpStatusList.collectAsState().value,
                    surveyRadius = surveyRadius,
                    onBack = {
                        navController.popBackStack()
                    },
                    onMarkerClick = { fId, bCode, gpName, surveyRadius ->
                        val encodedGpName = URLEncoder.encode(gpName ?: "", "UTF-8")

                        navController.navigate(
                            Screen.FormDataCollection.createRoute(fId, bCode, encodedGpName,surveyRadius)
                        )
                    }
                )
            }

            composable(
                route = Screen.FormMap.route,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("fieldId") { type = NavType.StringType },
                    navArgument("initialValue") { type = NavType.StringType },
                    navArgument("radius") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: "Point"
                val fieldId = backStackEntry.arguments?.getString("fieldId") ?: ""
                val initialValue = URLDecoder.decode(backStackEntry.arguments?.getString("initialValue") ?: "", "UTF-8")
                val radius = backStackEntry.arguments?.getInt("radius")

                FieldMapScreen(
                    type = type,
                    fieldId = fieldId,
                    initialValue = initialValue,
                    onBack = { navController.popBackStack() },
                    onSave = { resultFieldId, resultValue ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "map_result",
                            Pair(resultFieldId, resultValue)
                        )
                        navController.popBackStack()
                    },
                    radius = radius
                )
            }
        }
    }
}
