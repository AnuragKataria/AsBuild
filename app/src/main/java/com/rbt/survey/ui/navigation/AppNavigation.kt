package com.rbt.survey.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.rbt.survey.data.remote.RetrofitClient
import com.rbt.survey.data.repository.AuthRepository
import com.rbt.survey.data.repository.FormRepository
import com.rbt.survey.data.repository.GeoRepository
import com.rbt.survey.ui.form.FormDataCollectionScreen
import com.rbt.survey.ui.form.FormDataCollectionViewModel
import com.rbt.survey.ui.form.FormDataCollectionViewModelFactory
import com.rbt.survey.ui.home.HomeScreen
import com.rbt.survey.ui.home.HomeViewModel
import com.rbt.survey.ui.home.HomeViewModelFactory
import com.rbt.survey.ui.login.LoginScreen
import com.rbt.survey.ui.login.LoginViewModel
import com.rbt.survey.ui.login.LoginViewModelFactory
import com.rbt.survey.ui.map.MapScreen
import com.rbt.survey.ui.form.MapScreen
import com.rbt.survey.ui.map.MapViewModel
import com.rbt.survey.ui.map.MapViewModelFactory
import com.rbt.survey.ui.splash.SplashScreen
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")
    object FormDataCollection : Screen("form_data/{formId}?blockCode={blockCode}&gpName={gpName}&submissionId={submissionId}") {
        fun createRoute(formId: Int, blockCode: String?, gpName: String?, submissionId: Int? = null) = "form_data/$formId?blockCode=${blockCode ?: ""}&gpName=${gpName ?: ""}&submissionId=${submissionId ?: -1}"
    }
    object GPMap : Screen("gp_map/{formId}/{blockCode}") {
        fun createRoute(formId: Int, blockCode: String) =
            "gp_map/$formId/$blockCode"
    }

    object FormMap : Screen("form_map/{type}/{fieldId}/{initialValue}") {
        fun createRoute(type: String, fieldId: String, initialValue: String) =
            "form_map/$type/$fieldId/${URLEncoder.encode(initialValue, "UTF-8")}"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val preferences = UserPreferences(context)
    val isLoggedInState by preferences.authToken.collectAsState(initial = "LOADING")

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
                        navController.navigate(Screen.Home.route) {
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
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                val database = AppDatabase.getDatabase(context)
                val authApi = remember {
                    RetrofitClient.getAuthenticatedApi(context, preferences)
                }
                val geoApi = remember {
                    RetrofitClient.getGeoApi(context, preferences)
                }
                val repository = remember {
                    FormRepository(authApi, database.formDraftDao(), database.offlineSubmissionDao())
                }
                val geoRepository = remember {
                    GeoRepository(geoApi)
                }
                val parentEntry = remember {
                    navController.getBackStackEntry(Screen.Home.route)
                }

                val viewModel: HomeViewModel = viewModel(
                    parentEntry,
                    factory = HomeViewModelFactory(repository, geoRepository, preferences)
                )
                HomeScreen(
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
                    onNavigateToEditOfflineSubmission = { formId, submissionId, blockCode, gpName ->
                        val encodedGpName = URLEncoder.encode(gpName ?: "", "UTF-8")
                        navController.navigate(
                            Screen.FormDataCollection.createRoute(formId, blockCode, encodedGpName, submissionId)
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
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
                    navArgument("submissionId") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { backStackEntry ->
                val formId = backStackEntry.arguments?.getInt("formId") ?: 0
                val blockCode = backStackEntry.arguments?.getString("blockCode")
                val gpNameEncoded = backStackEntry.arguments?.getString("gpName")
                val gpName = URLDecoder.decode(gpNameEncoded ?: "", "UTF-8")
                val submissionIdArg = backStackEntry.arguments?.getInt("submissionId") ?: -1
                val submissionId = if (submissionIdArg != -1) submissionIdArg else null
                val database = AppDatabase.getDatabase(context)
                val authApi = RetrofitClient.getAuthenticatedApi(context, preferences)
                val repository = FormRepository(authApi, database.formDraftDao(), database.offlineSubmissionDao())
                val viewModel: FormDataCollectionViewModel = viewModel(
                    factory = FormDataCollectionViewModelFactory(
                        formId,
                        blockCode,
                        repository,
                        preferences,
                        gpName,
                        submissionId
                    )
                )
                FormDataCollectionScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToMap = { type, fieldId, initialValue ->
                        navController.navigate(
                            Screen.FormMap.createRoute(
                                type,
                                fieldId,
                                initialValue
                            )
                        )
                    },
                    navController = navController,
                    onSubmitSuccess = {
                        navController.navigate("home") {
                            popUpTo("home") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
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
                    database.offlineSubmissionDao()
                )

                val viewModel: MapViewModel = viewModel(
                    factory = MapViewModelFactory(repository)
                )

                val parentEntry = remember {
                    navController.getBackStackEntry(Screen.Home.route)
                }
                val homeViewModel: HomeViewModel = viewModel(parentEntry)

                MapScreen(
                    viewModel = viewModel,
                    formId = formId,
                    blockCode = blockCode,
                    gpStatusList = homeViewModel.selectedGpStatusList.collectAsState().value,
                    onBack = {
                        navController.popBackStack()
                    },
                    onMarkerClick = { fId, bCode,gpName ->
                        val encodedGpName = URLEncoder.encode(gpName ?: "", "UTF-8")

                        navController.navigate(
                            Screen.FormDataCollection.createRoute(fId, bCode, encodedGpName)
                        )
                    }
                )
            }

            composable(
                route = Screen.FormMap.route,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("fieldId") { type = NavType.StringType },
                    navArgument("initialValue") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: "Point"
                val fieldId = backStackEntry.arguments?.getString("fieldId") ?: ""
                val initialValue = URLDecoder.decode(backStackEntry.arguments?.getString("initialValue") ?: "", "UTF-8")

                MapScreen(
                    type = type,
                    fieldId = fieldId,
                    initialValue = initialValue,
                    onBack = { navController.popBackStack() },
                    onSave = { resultFieldId, resultValue ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("map_result", Pair(resultFieldId, resultValue))
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
