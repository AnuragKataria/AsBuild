package com.rbt.survey.ui.navigation

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.*
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.rbt.survey.MyApplication
import com.rbt.survey.data.local.UserPreferences
import com.rbt.survey.data.local.db.AppDatabase
import com.rbt.survey.dgps.DgpsManager
import com.rbt.survey.data.remote.RetrofitClient
import com.rbt.survey.data.repository.*
import com.rbt.survey.ui.form.*
import com.rbt.survey.ui.surveyDashboard.*
import com.rbt.survey.ui.login.*
import com.rbt.survey.ui.inventory.InventoryScreen
import com.rbt.survey.ui.login.LoginViewModelFactory
import com.rbt.survey.ui.map.MapScreen as GpMapScreen
import com.rbt.survey.ui.form.MapScreen as FieldMapScreen
import com.rbt.survey.ui.map.MapViewModel
import com.rbt.survey.ui.map.MapViewModelFactory
import com.rbt.survey.ui.dgps.*
//import com.rbt.survey.ui.dgps.DeviceSelfCheckScreen
import com.rbt.survey.ui.splash.SplashScreen
import java.net.*

import androidx.work.*
import com.rbt.survey.data.repository.AssetRepository
import com.rbt.survey.location.LocationService
import com.rbt.survey.ui.dashboard.DashboardScreen
import com.rbt.survey.ui.incidentManagement.IncidentManagementScreen
import com.rbt.survey.ui.incidentManagement.IncidentManagementViewModel
import com.rbt.survey.ui.incidentManagement.IncidentManagementViewModelFactory
import com.rbt.survey.ui.inventory.InventoryMapScreen
import com.rbt.survey.ui.inventory.InventoryMapViewModel
import com.rbt.survey.ui.inventory.InventoryMapViewModelFactory
import com.rbt.survey.ui.locationTrackingDashboard.*
import com.rbt.survey.worker.SyncWorker
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object SurveyDashboard  : Screen("survey_dashboard")
    object InventoryMap  : Screen("inventory_map")
//    object Inventory  : Screen("inventory")
//    object AssetManagement  : Screen("asset_management")
//    object AssetAddProject  : Screen("asset_add_project")
//    object ViewEditAsset  : Screen("view_edit_asset") {
//        fun createRoute(assetTypeId: Int) =
//            "asset_detail/$assetTypeId"
//    }
//    object AssetDetail : Screen("asset_detail/{assetTypeId}") {
//        fun createRoute(assetTypeId: Int) =
//            "asset_detail/$assetTypeId"
//    }
    object Dashboard : Screen("dashboard")

    object LocationTracking : Screen("location_tracking")
    object IncidentManagement : Screen("incident_management")
    object FormDataCollection : Screen("form_data/{formId}?blockCode={blockCode}&gpName={gpName}&surveyRadius={surveyRadius}&submissionId={submissionId}&lineGeometry={lineGeometry}") {
        fun createRoute(formId: Int, blockCode: String?, gpName: String?,surveyRadius: Int?, submissionId: Int? = null, lineGeometry: String? = null) : String {

            val encodedLine = URLEncoder.encode(lineGeometry ?: "","UTF-8")

            return "form_data/$formId?blockCode=${blockCode ?: ""}&gpName=${gpName ?: ""}&surveyRadius=${surveyRadius ?: -1}&submissionId=${submissionId ?: -1}&lineGeometry=$encodedLine"
        }
    }
    object GPMap : Screen("gp_map/{formId}/{blockCode}") {
        fun createRoute(formId: Int, blockCode: String) =
            "gp_map/$formId/$blockCode"
    }

    object FormMap : Screen("form_map/{type}/{fieldId}/{initialValue}?radius={radius}&refLine={refLine}") {
        fun createRoute(type: String, fieldId: String, initialValue: String, radius: Int?, refLine: String? = null) : String {

            val encodedLine = URLEncoder.encode(refLine ?: "", "UTF-8")

                    return "form_map/$type/$fieldId/${
                        URLEncoder.encode(
                            initialValue,
                            "UTF-8"
                        )
                    }?radius=${radius ?: -1}&refLine=$encodedLine"
                }
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
    object DgpsSelfCheck : Screen("dgps_self_check")
    object SatelliteView : Screen("satellite_view")
    object BluetoothDeviceList : Screen("bluetooth_device_list")
}


@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val preferences = UserPreferences(context)
    val dgpsManager = remember(context.applicationContext) {
        (context.applicationContext as MyApplication).dgpsManager
    }
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
                        navController.navigate(Screen.InventoryMap.route)
                    },
                    onSurveyClick = {
                        navController.navigate(Screen.SurveyDashboard.route)
                    },
                    onLocationTrackingClick = {
                        navController.navigate(Screen.LocationTracking.route)
                    },
                    onIncidentManagementClick = {
                        navController.navigate(Screen.IncidentManagement.route)
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

            composable(Screen.InventoryMap.route) {

                val assetApi = remember {
                    RetrofitClient.getAssetApi(context, preferences)
                }
                val assetRepository = remember {
                    AssetRepository(assetApi)
                }
                val viewModel: InventoryMapViewModel = viewModel(
                    factory = InventoryMapViewModelFactory(assetRepository)
                )

                InventoryMapScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    viewModel = viewModel,
                )
            }

//            composable(Screen.Inventory.route) {
//                InventoryScreen(
//                    onAssetManagementClick = {
//                        navController.navigate(Screen.AssetManagement.route)
//                    },
//                    onAddAssetsToProjectClick = {
//                        navController.navigate(Screen.AssetAddProject.route)
//                    },
//                    onBackClick = {
//                        navController.popBackStack()
//                    }
//                )
//            }
//
//            composable(Screen.AssetManagement.route) {
//
//                val assetApi = remember {
//                    RetrofitClient.getAssetApi(context, preferences)
//                }
//                val assetRepository = remember {
//                    AssetRepository(assetApi)
//                }
//                val viewModel: AssetManagementViewModel = viewModel(
//                    factory = AssetManagementViewModelFactory(assetRepository)
//                )
//
//                AssetManagementScreen(
//                    onBackClick = {
//                        navController.popBackStack()
//                    },
//                    onAssetClick = { assetTypeId ->
//
//                        navController.navigate(
//                            Screen.AssetDetail.createRoute(
//                                assetTypeId
//                            )
//                        )
//                    },
//                    viewModel = viewModel
//                )
//            }
//
//            composable(
//                Screen.AssetDetail.route
//            ) { backStackEntry ->
//
//                val assetTypeId =
//                    backStackEntry.arguments
//                        ?.getString("assetTypeId")
//                        ?.toInt() ?: 0
//
//                val assetApi = remember {
//                    RetrofitClient.getAssetApi(context, preferences)
//                }
//                val assetRepository = remember {
//                    AssetRepository(assetApi)
//                }
//                val viewModel: AssetDetailViewModel = viewModel(
//                    factory = AssetDetailViewModelFactory(assetRepository)
//                )
//
//                AssetDetailScreen(
//                    assetTypeId = assetTypeId,
//                    onBackClick = {
//                        navController.popBackStack()
//                    },
//                    viewModel = viewModel
//                )
//            }
//
//            composable(Screen.AssetAddProject.route) {
//
//                val assetApi = remember {
//                    RetrofitClient.getAssetApi(context, preferences)
//                }
//                val assetRepository = remember {
//                    AssetRepository(assetApi)
//                }
//                val viewModel: AssetAddProjectViewModel = viewModel(
//                    factory = AssetAddProjectViewModelFactory(assetRepository)
//                )
//
//                AssetAddProjectScreen(
//                    onBackClick = {
//                        navController.popBackStack()
//                    },
//                    viewModel = viewModel,
////                    onvieweditassetClick = {
////                        navController.navigate(
////                            Screen.ViewEditAsset.createRoute(
////                                assetTypeId
////                            )
////                        )
////                    },
//                    onaddassetClick = {
//                        navController.popBackStack()
//                    },
//                )
//            }

            composable(Screen.SurveyDashboard.route) {
                val database = AppDatabase.getDatabase(context)
                val authApi = remember {
                    RetrofitClient.getAuthenticatedApi(context, preferences)
                }
                val tnctApi = RetrofitClient.getTnctApi(context)
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
                        database.pendingFileUploadDao(),
                        tnctApi,
                        database.cachedOptionSegmentDao()
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
                    },
                    onBackClick = {
                        navController.popBackStack()
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

            composable(Screen.IncidentManagement.route) {

                val assetApi = remember {
                    RetrofitClient.getAssetApi(context, preferences)
                }
                val assetRepository = remember {
                    AssetRepository(assetApi)
                }
                val viewModel: IncidentManagementViewModel = viewModel(
                    factory = IncidentManagementViewModelFactory(assetRepository)
                )

                IncidentManagementScreen(
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
                    onSelfCheckClick = { navController.navigate(Screen.DgpsSelfCheck.route) },
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

//            composable(Screen.DgpsSelfCheck.route) {
//                val parentEntry = remember {
//                    navController.getBackStackEntry(Screen.DgpsSettings.route)
//                }
//                val dgpsViewModel: DgpsViewModel = viewModel(
//                    parentEntry,
//                    factory = DgpsViewModelFactory(preferences, dgpsManager)
//                )
//                DeviceSelfCheckScreen(
//                    viewModel = dgpsViewModel,
//                    onBack = { navController.popBackStack() },
//                    onOpenCommunication = { navController.navigate(Screen.BluetoothDeviceList.route) }
//                )
//            }

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
                    },
                            navArgument("lineGeometry") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
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
                val lineGeometryEncoded = backStackEntry.arguments?.getString("lineGeometry")
                val lineGeometry = URLDecoder.decode(lineGeometryEncoded ?: "", "UTF-8")
                val database = AppDatabase.getDatabase(context)
                val authApi = RetrofitClient.getAuthenticatedApi(context, preferences)
                val tnctApi = RetrofitClient.getTnctApi(context)
                val repository = FormRepository(
                    authApi, 
                    database.formDraftDao(), 
                    database.offlineSubmissionDao(),
                    database.cachedFormDao(),
                    database.cachedFormDetailDao(),
                    database.pendingFileUploadDao(),
                    tnctApi,
                    database.cachedOptionSegmentDao()
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
                        radius,
                        lineGeometry
                    )
                )
                FormDataCollectionScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToMap = { type, fieldId, initialValue, radius, lineGeometry ->
                        navController.navigate(
                            Screen.FormMap.createRoute(
                                type,
                                fieldId,
                                initialValue,
                                radius,
                                lineGeometry
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
                val tnctApi = RetrofitClient.getTnctApi(context)
                val database = AppDatabase.getDatabase(context)

                val repository = FormRepository(
                    authApi,
                    database.formDraftDao(),
                    database.offlineSubmissionDao(),
                    database.cachedFormDao(),
                    database.cachedFormDetailDao(),
                    database.pendingFileUploadDao(),
                    tnctApi,
                    database.cachedOptionSegmentDao()
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
                    },
                    onLineClick = { fId, bCode, geometry, surveyRadius ->

                        navController.navigate(
                            Screen.FormDataCollection.createRoute(
                                formId = fId,
                                blockCode = bCode,
                                gpName = null,
                                surveyRadius = surveyRadius,
                                lineGeometry = geometry
                            )
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
                    navArgument("radius") { type = NavType.IntType },
                    navArgument("refLine") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: "Point"
                val fieldId = backStackEntry.arguments?.getString("fieldId") ?: ""
                val initialValue = URLDecoder.decode(backStackEntry.arguments?.getString("initialValue") ?: "", "UTF-8")
                val radius = backStackEntry.arguments?.getInt("radius")
                val reflineEncoded = backStackEntry.arguments?.getString("refLine")
                val refline = URLDecoder.decode(reflineEncoded ?: "", "UTF-8")

                FieldMapScreen(
                    type = type,
                    fieldId = fieldId,
                    initialValue = initialValue,
                    refLine = refline,
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
