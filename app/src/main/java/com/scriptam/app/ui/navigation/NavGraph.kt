package com.scriptam.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.scriptam.app.ui.dashboard.DashboardScreen
import com.scriptam.app.ui.editor.EditorScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val EDITOR = "editor/{scriptId}"

    fun editor(scriptId: Long) = "editor/$scriptId"
}

@Composable
fun ScriptamNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onScriptClick = { scriptId ->
                    navController.navigate(Routes.editor(scriptId))
                }
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(
                navArgument("scriptId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val scriptId = backStackEntry.arguments?.getLong("scriptId") ?: return@composable
            EditorScreen(
                scriptId = scriptId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
