package com.toffice.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Slideshow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toffice.app.R
import com.toffice.app.core.navigation.Routes

private data class ModuleItem(
    val labelRes: Int,
    val officeNameRes: Int?,
    val icon: ImageVector,
    val color: Color,
    val route: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onOpenModule: (String) -> Unit) {
    // بترتيب مجموعة أوفيس مع ألوان كل تطبيق المميزة
    val modules = listOf(
        ModuleItem(R.string.module_editor, R.string.office_word, Icons.Default.Description, Color(0xFF2B579A), Routes.EDITOR),
        ModuleItem(R.string.module_sheets, R.string.office_excel, Icons.Default.GridOn, Color(0xFF217346), Routes.SHEETS),
        ModuleItem(R.string.module_slides, R.string.office_powerpoint, Icons.Default.Slideshow, Color(0xFFC43E1C), Routes.SLIDES),
        ModuleItem(R.string.module_notes, R.string.office_onenote, Icons.AutoMirrored.Filled.Notes, Color(0xFF7719AA), Routes.NOTES),
        ModuleItem(R.string.module_calendar, R.string.office_outlook, Icons.Default.CalendarMonth, Color(0xFF0078D4), Routes.CALENDAR),
        ModuleItem(R.string.module_tasks, R.string.office_todo, Icons.Default.CheckCircle, Color(0xFF2564CF), Routes.TASKS),
        ModuleItem(R.string.module_expenses, R.string.office_money, Icons.Default.AccountBalanceWallet, Color(0xFF008272), Routes.EXPENSES),
        ModuleItem(R.string.module_documents, R.string.office_onedrive, Icons.Default.Folder, Color(0xFF0364B8), Routes.DOCUMENTS),
        ModuleItem(R.string.module_backup, null, Icons.Default.Backup, Color(0xFF5D6B7A), Routes.BACKUP),
        ModuleItem(R.string.module_settings, null, Icons.Default.Settings, Color(0xFF455A64), Routes.SETTINGS),
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
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(item.color, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
            Text(
                text = stringResource(item.labelRes),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 10.dp),
            )
            if (item.officeNameRes != null) {
                Text(
                    text = stringResource(item.officeNameRes),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = item.color,
                )
            }
        }
    }
}
