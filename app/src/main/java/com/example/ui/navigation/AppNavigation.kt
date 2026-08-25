package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.SignInScreen
import com.example.ui.auth.SignUpScreen
import com.example.ui.home.HomeScreen

object Routes {
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val HOME = "home"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SIGN_IN
    ) {
        composable(Routes.SIGN_IN) {
            SignInScreen(
                viewModel = authViewModel,
                onNavigateToSignUp = {
                    navController.navigate(Routes.SIGN_UP)
                },
                onSignInSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SIGN_IN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SIGN_UP) {
            SignUpScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSignUpSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SIGN_IN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                viewModel = authViewModel,
                onSignOut = {
                    navController.navigate(Routes.SIGN_IN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}
