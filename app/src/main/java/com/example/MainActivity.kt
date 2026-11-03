package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.api.GitHubApiClient
import com.example.data.repository.GitHubRepository
import com.example.data.repository.ZipManager
import com.example.data.security.SecureTokenStorage
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var tokenStorage: SecureTokenStorage
    private lateinit var apiClient: GitHubApiClient
    private lateinit var repository: GitHubRepository
    private lateinit var zipManager: ZipManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize core data & network dependencies
        tokenStorage = SecureTokenStorage(applicationContext)
        apiClient = GitHubApiClient(tokenStorage)
        repository = GitHubRepository(tokenStorage, apiClient)
        zipManager = ZipManager(applicationContext, apiClient)

        // Validate any existing saved token on startup
        lifecycleScope.launch {
            repository.checkExistingAuth()
        }

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        repository = repository,
                        zipManager = zipManager
                    )
                }
            }
        }
    }
}
