package com.toffice.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.toffice.app.feature.common.PlaceholderScreen
import com.toffice.app.feature.dashboard.DashboardScreen
import com.toffice.app.feature.editor.DocumentsListScreen
import com.toffice.app.feature.editor.EditorScreen
import com.toffice.app.feature.editor.LaunchScreen
import com.toffice.app.feature.settings.SettingsScreen
import com.toffice.app.feature.tasks.TasksScreen

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.EDITOR) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(onOpenModule = { route -> navController.navigate(route) })
        }
        composable(Routes.TASKS) {
            TasksScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SHEETS) {
            PlaceholderScreen(title = "جداول بيانات (Excel)", onBack = { navController.popBackStack() })
        }
        composable(Routes.NOTES) {
            PlaceholderScreen(title = "ملاحظات (OneNote)", onBack = { navController.popBackStack() })
        }
        composable(Routes.CALENDAR) {
            PlaceholderScreen(title = "التقويم والمواعيد (Outlook)", onBack = { navController.popBackStack() })
        }
        composable(Routes.EXPENSES) {
            PlaceholderScreen(title = "الميزانية", onBack = { navController.popBackStack() })
        }
        composable(Routes.DOCUMENTS) {
            PlaceholderScreen(title = "الملفات والمستندات", onBack = { navController.popBackStack() })
        }
        composable(Routes.EDITOR) {
            // الدخول مباشرةً في المحرّر (آخر مستند أو جديد)؛ أو قائمة المستندات عند «فتح بواسطة».
            LaunchScreen(
                onReady = { id ->
                    navController.navigate(Routes.editorDoc(id)) {
                        popUpTo(Routes.EDITOR) { inclusive = true }
                    }
                },
                onExternal = {
                    navController.navigate(Routes.DOCS_LIST) {
                        popUpTo(Routes.EDITOR) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.DOCS_LIST) {
            // قائمة المستندات (مستنداتي) — تُفتح من داخل المحرّر.
            DocumentsListScreen(
                onBack = { navController.navigate(Routes.DASHBOARD) },
                onOpenDocument = { id -> navController.navigate(Routes.editorDoc(id)) },
            )
        }
        composable(
            route = Routes.EDITOR_DOC,
            arguments = listOf(navArgument("docId") { type = NavType.StringType }),
        ) {
            EditorScreen(onBack = {
                navController.navigate(Routes.DOCS_LIST) { launchSingleTop = true }
            })
        }
        composable(Routes.BACKUP) {
            PlaceholderScreen(title = "النسخ الاحتياطي", onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
