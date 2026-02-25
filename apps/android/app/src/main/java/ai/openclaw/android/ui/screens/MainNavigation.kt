package ai.openclaw.android.ui.screens

import ai.openclaw.android.domain.model.Provider
import ai.openclaw.android.ui.screens.auth.AuthSelectionScreen
import ai.openclaw.android.ui.screens.auth.AuthWebViewScreen
import ai.openclaw.android.ui.viewmodel.AuthViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * 导航路由
 */
sealed class Screen(val route: String) {
    object AuthSelection : Screen("auth_selection")
    object AuthWebView : Screen("auth_webview/{providerId}") {
        fun createRoute(providerId: String) = "auth_webview/$providerId"
    }
}

/**
 * 主导航图
 */
@Composable
fun MainNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.AuthSelection.route
    ) {
        // 认证选择页面
        composable(Screen.AuthSelection.route) {
            AuthSelectionScreen(
                viewModel = authViewModel,
                onProviderSelected = { provider ->
                    authViewModel.startAuthentication(provider)
                    navController.navigate(Screen.AuthWebView.createRoute(provider.id))
                }
            )
        }
        
        // WebView 认证页面
        composable(Screen.AuthWebView.route) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
            val provider = Provider.fromId(providerId)
            
            if (provider != null) {
                AuthWebViewScreen(
                    provider = provider,
                    viewModel = authViewModel,
                    onAuthSuccess = {
                        navController.popBackStack()
                    },
                    onAuthCancelled = {
                        authViewModel.cancelAuthentication()
                        navController.popBackStack()
                    }
                )
            } else {
                // 无效的提供商，返回选择页面
                navController.popBackStack()
            }
        }
    }
}
