package com.androidfactory.simplerick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.androidfactory.network.KtorClient
import com.androidfactory.simplerick.screens.AllEpisodesScreen
import com.androidfactory.simplerick.screens.CharacterDetailsScreen
import com.androidfactory.simplerick.screens.CharacterEpisodeScreen
import com.androidfactory.simplerick.screens.HomeScreen
import com.androidfactory.simplerick.screens.SearchScreen
import com.androidfactory.simplerick.ui.theme.RickAction
import com.androidfactory.simplerick.ui.theme.RickPrimary
import com.androidfactory.simplerick.ui.theme.SimpleRickTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable
import javax.inject.Inject

sealed class NavDestination(val title: String, val route: Any, val icon: ImageVector) {
    object Home : NavDestination(title = "Home", route = HomeRoute, icon = Icons.Filled.Home)
    object Episodes :
        NavDestination(title = "Episodes", route = AllEpisodesRoute, icon = Icons.Filled.PlayArrow)

    object Search :
        NavDestination(title = "Search", route = SearchRoute, icon = Icons.Filled.Search)
}

// define typed routes
@Serializable
object HomeRoute

@Serializable
object AllEpisodesRoute

@Serializable
object SearchRoute

@Serializable
data class CharacterDetailsRoute(val characterId: Int)

@Serializable
data class CharacterEpisodeRoute(val characterId: Int)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var ktorClient: KtorClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            val navController = rememberNavController()
            val items = listOf(
                NavDestination.Home, NavDestination.Episodes, NavDestination.Search
            )
            var selectedIndex by remember { mutableIntStateOf(0) }

            SimpleRickTheme {
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = RickPrimary
                        ) {
                            items.forEachIndexed { index, screen ->
                                NavigationBarItem(
                                    icon = {
                                        Icon(imageVector = screen.icon, contentDescription = null)
                                    },
                                    label = { Text(screen.title) },
                                    selected = index == selectedIndex,
                                    onClick = {
                                        selectedIndex = index
                                        navController.navigate(screen.route) {
                                            // Pop up to the start destination of the graph to
                                            // avoid building up a large stack of destinations
                                            // on the back stack as users select items
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            // Avoid multiple copies of the same destination when
                                            // re-selecting the same item
                                            launchSingleTop = true
                                            // Restore state when re-selecting a previously selected item
                                            restoreState = true
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = RickAction,
                                        selectedTextColor = RickAction,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavigationHost(
                        navController = navController,
                        ktorClient = ktorClient,
                        innerPadding = innerPadding
                    )
                }
            }
        }
    }

    @Composable
    private fun NavigationHost(
        navController: NavHostController,
        ktorClient: KtorClient,
        innerPadding: PaddingValues
    ) {
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier
                .background(color = RickPrimary)
                .padding(innerPadding)
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onCharacterSelected = { characterId ->
                        navController.navigate(CharacterDetailsRoute(characterId))
                    }
                )
            }

            composable<CharacterDetailsRoute>
            { backStackEntry ->
                val characterDetails: CharacterDetailsRoute = backStackEntry.toRoute()
                val characterId: Int = characterDetails.characterId
                CharacterDetailsScreen(
                    characterId = characterId,
                    onEpisodeClicked = { navController.navigate(CharacterEpisodeRoute(characterId)) },
                    onBackClicked = { navController.navigateUp() }
                )
            }
            composable<CharacterEpisodeRoute> { backStackEntry ->
                val characterEpisodeRoute: CharacterEpisodeRoute = backStackEntry.toRoute()
                val characterId: Int = characterEpisodeRoute.characterId
                CharacterEpisodeScreen(
                    characterId = characterId,
                    ktorClient = ktorClient,
                    onBackClicked = { navController.navigateUp() }
                )
            }
            composable<AllEpisodesRoute> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AllEpisodesScreen()
                }
            }
            composable<SearchRoute> {
                SearchScreen(
                    onCharacterClicked = { characterId ->
                        navController.navigate(CharacterDetailsRoute(characterId))
                    }
                )
            }
        }
    }
}