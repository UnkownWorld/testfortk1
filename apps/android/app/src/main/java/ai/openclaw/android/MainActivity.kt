package ai.openclaw.android

import ai.openclaw.android.ui.screens.MainNavigation
import ai.openclaw.android.ui.viewmodel.AuthViewModel
import ai.openclaw.android.ui.viewmodel.ChatViewModel
import ai.openclaw.android.ui.viewmodel.SessionsViewModel
import ai.openclaw.android.ui.viewmodel.SettingsViewModel
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主 Activity
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val chatViewModel: ChatViewModel = hiltViewModel()
                    val sessionsViewModel: SessionsViewModel = hiltViewModel()
                    val settingsViewModel: SettingsViewModel = hiltViewModel()
                    
                    MainNavigation(
                        authViewModel = authViewModel,
                        chatViewModel = chatViewModel,
                        sessionsViewModel = sessionsViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}
