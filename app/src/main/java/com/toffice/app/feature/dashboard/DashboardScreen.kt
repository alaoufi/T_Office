package com.toffice.app.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toffice.app.R
import com.toffice.app.core.navigation.Routes

private data class ModuleItem(val labelRes: Int, val icon: ImageVector, val route: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onOpenModule: (String) -> Unit) {
    val modules = listOf(
        ModuleItem(R.string.module_editor, Icons.Default.Description, Routes.EDITOR),
        ModuleItem(R.string.module_tasks, Icons.Default.CheckCircle, Routes.TASKS),
        ModuleItem(R.string.module_notes, Icons.AutoMirrored.Filled.Notes, Routes.NOTES),
        ModuleItem(R.string.module_calendar, Icons.Default.CalendarMonth, Routes.CALENDAR),
        ModuleItem(R.string.module_expenses, Icons.Default.AccountBalanceWallet, Routes.EXPENSES),
        ModuleItem(R.string.module_documents, Icons.Default.Folder, Routes.DOCUMENTS),
        ModuleItem(R.string.module_backup, Icons.Default.Backup, Routes.BACKUP),
        ModuleItem(R.string.module_settings, Icons.Default.Settings, Routes.SETTINGS),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.dashboard_title))
                        Text(
                            stringResource(R.string.dashboard_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(modules) { item ->
                ModuleCard(item, onClick = { onOpenModule(item.route) })
            }
        }
    }
}

@Composable
private fun ModuleCard(item: ModuleItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(1.1f),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = stringResource(item.labelRes),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
