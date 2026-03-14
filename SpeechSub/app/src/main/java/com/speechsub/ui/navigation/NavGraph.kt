package com.speechsub.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.speechsub.ui.auth.LoginScreen
import com.speechsub.ui.auth.SignUpScreen
import com.speechsub.ui.editor.CaptionEditorScreen
import com.speechsub.ui.export.ExportScreen
import com.speechsub.ui.home.HomeScreen
import com.speechsub.ui.processing.ProcessingScreen
import com.speechsub.ui.settings.SettingsScreen
import com.speechsub.ui.splash.SplashScreen

/**
 * Screen — sealed class defining all navigation destinations.
 *
 * Using a sealed class keeps all routes in one place and prevents typos.
 * Parameters are passed as route arguments (e.g., /editor/42).
 */
sealed class Screen(val route: String) {
    object Splash    : Screen("splash")
    object Login     : Screen("login")
    object SignUp    : Screen("signup")
    object Home      : Screen("home")
    object Processing: Screen("processing/{projectId}") {
        fun createRoute(projectId: Long) = "processing/$projectId"
    }
    object Editor    : Screen("editor/{projectId}") {
        fun createRoute(projectId: Long) = "editor/$projectId"
    }
    object Export    : Screen("export/{projectId}") {
        fun createRoute(projectId: Long) = "export/$projectId"
    }
    object Settings  : Screen("settings")
}

/**
 * SpeechSubNavGraph — the main navigation graph for the app.
 *
 * All screen transitions use smooth slide animations.
 * Deep links can be added here in the future (e.g., from notifications).
 */
@Composable
fun SpeechSubNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination,
        enterTransition  = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec  = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition   = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(150))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec  = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(150))
        }
    ) {

        // Splash — shown on app launch, handles version check
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome  = { navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }},
                onNavigateToLogin = { navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }}
            )
        }

        // Auth screens
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }},
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = { navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.SignUp.route) { inclusive = true }
                }},
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // Home — list of projects + import button
        composable(Screen.Home.route) {
            HomeScreen(
                onProjectSelected  = { projectId ->
                    navController.navigate(Screen.Editor.createRoute(projectId))
                },
                onStartProcessing  = { projectId ->
                    navController.navigate(Screen.Processing.createRoute(projectId))
                },
                onNavigateSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        // Processing — speech recognition progress
        composable(
            route = Screen.Processing.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            ProcessingScreen(
                projectId = projectId,
                onProcessingComplete = {
                    navController.navigate(Screen.Editor.createRoute(projectId)) {
                        popUpTo(Screen.Processing.route) { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        // Caption Editor
        composable(
            route = Screen.Editor.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            CaptionEditorScreen(
                projectId = projectId,
                onNavigateExport   = { navController.navigate(Screen.Export.createRoute(projectId)) },
                onNavigateSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateBack     = { navController.popBackStack() }
            )
        }

        // Export
        composable(
            route = Screen.Export.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            ExportScreen(
                projectId      = projectId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignedOut    = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
