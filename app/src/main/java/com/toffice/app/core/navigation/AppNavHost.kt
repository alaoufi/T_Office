package com.toffice.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.toffice.app.feature.common.PlaceholderScreen
import com.toffice.app.feature.dashboard.DashboardScreen
import com.toffice.app.feature.tasks.TasksScreen

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(onOpenModule = { route -> navController.navigate(route) })
        }
        composable(Routes.TASKS) {
            TasksScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.NOTES) {
            PlaceholderScreen(title = "الملاحظات", onBack = { navController.popBackStack() })
        }
        composable(Routes.CALENDAR) {
            PlaceholderScreen(title = "المواعيد", onBack = { navController.popBackStack() })
        }
        composable(Routes.EXPENSES) {
            PlaceholderScreen(title = "المصروفات", onBack = { navController.popBackStack() })
        }
        composable(Routes.DOCUMENTS) {
            PlaceholderScreen(title = "المستندات", onBack = { navController.popBackStack() })
        }
        composable(Routes.EDITOR) {
            PlaceholderScreen(title = "محرر النصوص", onBack = { navController.popBackStack() })
        }
        composable(Routes.BACKUP) {
            PlaceholderScreen(title = "النسخ الاحتياطي", onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            PlaceholderScreen(title = "الإعدادات", onBack = { navController.popBackStack() })
        }
    }
}
