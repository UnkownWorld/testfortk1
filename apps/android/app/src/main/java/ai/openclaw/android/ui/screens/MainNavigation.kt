package ai.openclaw.android.ui.screens

import ai.openclaw.android.domain.model.AuthState
import ai.openclaw.android.domain.model.Provider
import ai.openclaw.android.ui.screens.auth.AuthSelectionScreen
import ai.openclaw.android.ui.screens.auth.AuthWebViewScreen
import ai.openclaw.android.ui.screens.chat.ChatScreen
import ai.openclaw.android.ui.screens.sessions.SessionsScreen
import ai.openclaw.android.ui.screens.settings.SettingsScreen
import ai.openclaw.android.ui.viewmodel.AuthViewModel
import ai.openclaw.android.ui.viewmodel.ChatViewModel
import ai.openclaw.android.ui.viewmodel.SessionsViewModel
import ai.openclaw.android.ui.viewmodel.SettingsViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    object Chat : Screen("chat")
    object AuthSelection : Screen("auth_selection")
    object AuthWebView : Screen("auth_webview/{providerId}") {
        fun createRoute(providerId: String) = "auth_webview/$providerId"
    }
    object Sessions : Screen("sessions")
    object Settings : Screen("settings")
}

/**
 * 主导航图
 */
@Composable
fun MainNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel,
    chatViewModel: ChatViewModel,
    sessionsViewModel: SessionsViewModel,
    settingsViewModel: SettingsViewModel
) {
    val authenticatedProviders by authViewModel.providerAuthStates.collectAsStateWithLifecycle()
    val hasAuth = authenticatedProviders.values.any { it is AuthState.Authenticated }
    
    // 如果没有认证，跳转到登录页面
    LaunchedEffect(hasAuth) {
        if (!hasAuth && navController.currentDestination?.route != Screen.AuthSelection.route) {
            navController.navigate(Screen.AuthSelection.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = if (hasAuth) Screen.Chat.route else Screen.AuthSelection.route
    ) {
        // 聊天页面
        composable(Screen.Chat.route) {
            ChatScreen(
                viewModel = chatViewModel,
                onNewChat = {
                    chatViewModel.createNewSession()
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
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
                        navController.navigate(Screen.Chat.route) {
                            popUpTo(Screen.AuthSelection.route) { inclusive = true }
                        }
                    },
                    onAuthCancelled = {
                        authViewModel.cancelAuthentication()
                        navController.popBackStack()
                    }
                )
            } else {
                navController.popBackStack()
            }
        }
        
        // 会话历史页面
        composable(Screen.Sessions.route) {
            SessionsScreen(
                viewModel = sessionsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSessionSelected = { sessionId ->
                    chatViewModel.loadSession(sessionId)
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Chat.route) { inclusive = true }
                    }
                }
            )
        }
        
        // 设置页面
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLoginClick = { provider ->
                    authViewModel.startAuthentication(provider)
                    navController.navigate(Screen.AuthWebView.createRoute(provider.id))
                }
            )
        }
    }
}
