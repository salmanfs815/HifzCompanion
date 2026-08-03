package com.hifzcompanion.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hifzcompanion.backup.BackupManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    backupManager: BackupManager // Injected or via ViewModel
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        // Icon.AutoMirrored.Rounded.ArrowBack
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Backup & Restore", style = MaterialTheme.typography.titleMedium)
            
            Button(
                onClick = {
                    scope.launch {
                        // In a real app, we'd trigger the Auth flow here
                        // For now, assume we have a token or handle it in a ViewModel
                        val result = backupManager.performBackup("dummy_token")
                        if (result.isSuccess) {
                            snackbarHostState.showSnackbar("Backup successful")
                        } else {
                            snackbarHostState.showSnackbar("Backup failed: ${result.exceptionOrNull()?.message}")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Backup to Google Drive")
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        val result = backupManager.performRestore("dummy_token")
                        if (result.isSuccess) {
                            snackbarHostState.showSnackbar("Restore successful")
                        } else {
                            snackbarHostState.showSnackbar("Restore failed: ${result.exceptionOrNull()?.message}")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restore from Google Drive")
            }
        }
    }
}
