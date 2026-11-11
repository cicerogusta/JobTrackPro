package com.app.jobtrackpro

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.app.designsystem.R as DesignResources
import androidx.fragment.compose.AndroidFragment
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.app.core.model.*
import com.app.designsystem.*
import com.app.designsystem.navigationbar.JobTrackTopAppBar
import com.app.feature.auth.AuthScreen
import com.app.feature.applications.*
import com.app.feature.jobs.JobsScreen
import com.app.feature.settings.SettingsScreen
import com.app.home.HomeScreen

@Composable
fun JobTrackApp(viewModel: AppViewModel, activity: MainActivity) {
    val session = viewModel.session.collectAsStateWithLifecycle().value
    val theme = viewModel.theme.collectAsStateWithLifecycle().value
    LaunchedEffect(theme) {
        AppCompatDelegate.setDefaultNightMode(when (theme) {
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        })
    }
    JobTrackTheme(theme) {
        Surface(Modifier.fillMaxSize()) {
            when (val current = session) {
                Session.Loading -> JobTrackLoading()
                Session.Unconfigured -> Column(Modifier.safeDrawingPadding().padding(Spacing.large),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                    Text(stringResource(R.string.firebase_setup_title), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.firebase_setup_description))
                }
                Session.SignedOut -> Box(Modifier.safeDrawingPadding()) { AuthScreen(hiltViewModel()) }
                is Session.Authenticated -> key(current.user.id) { AuthenticatedApp(activity) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedApp(activity: MainActivity) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tabs = listOf(Triple("home", stringResource(R.string.tab_home), DesignResources.drawable.ic_home),
        Triple("applications", stringResource(R.string.tab_applications), DesignResources.drawable.ic_applications),
        Triple("jobs", stringResource(R.string.tab_jobs), DesignResources.drawable.ic_search), Triple("settings", stringResource(R.string.tab_settings), DesignResources.drawable.ic_settings))
    val isTab = tabs.any { it.first == route }
    DisposableEffect(nav, activity) {
        activity.applicationNavigation = { id -> nav.navigate("detail/$id") }
        onDispose { activity.applicationNavigation = null }
    }
    Scaffold(
        topBar = { JobTrackTopAppBar(onBack = if (isTab) null else ({ nav.popBackStack() })) },
        bottomBar = {
            if (isTab) NavigationBar {
                tabs.forEach { (destination, label, icon) ->
                    NavigationBarItem(selected = route == destination,
                        onClick = { nav.navigate(destination) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(painterResource(icon), contentDescription = null) }, label = { Text(label) })
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(hiltViewModel(), { nav.navigate("form") }, { nav.navigate("detail/$it") }) }
            composable("applications") { AndroidFragment<ApplicationsFragment>(modifier = Modifier.fillMaxSize()) }
            composable("jobs") { JobsScreen(hiltViewModel()) }
            composable("settings") { SettingsScreen(hiltViewModel()) }
            composable("form") { ApplicationFormScreen(hiltViewModel(), { nav.popBackStack() }) }
            composable("edit/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                ApplicationFormScreen(hiltViewModel(), { nav.popBackStack() })
            }
            composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                ApplicationDetailScreen(hiltViewModel(), { id -> nav.navigate("edit/$id") }, { nav.popBackStack() })
            }
        }
    }
}
