package com.example.tempcontacts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tempcontacts.ui.theme.TempContactsTheme
import androidx.navigation.NavType
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing

const val ABOUT_ROUTE = "about_page"
const val PRIVACY_ROUTE = "privacy_policy_screen"

class MainActivity : ComponentActivity() {

    private val viewModel: ContactViewModel by viewModels()
    private lateinit var settingsDataStore: SettingsDataStore

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        settingsDataStore = SettingsDataStore(this)
        createNotificationChannels()
        askForPermissions()

        setContent {
            val theme by settingsDataStore.themeFlow.collectAsState(initial = "System")
            val useDarkTheme = when (theme) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            val hasSeenOnboarding by settingsDataStore.hasSeenOnboardingFlow.collectAsState(initial = null)

            TempContactsTheme(darkTheme = useDarkTheme) {
                if (hasSeenOnboarding != null) {
                    val navController = rememberNavController()
                    val startDestination =
                        if (hasSeenOnboarding == true) "contactList" else "onboarding"

                    // The Surface wrapper provides the background color during animations
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                            enterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { 1000 },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(300))
                            },
                            exitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { -1000 },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(300))
                            },
                            popEnterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { -1000 },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(300))
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { 1000 },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(300))
                            }
                        ) {
                            composable("onboarding") {
                                val onboardingViewModel: OnboardingViewModel =
                                    viewModel(factory = OnboardingViewModelFactory(settingsDataStore))
                                OnboardingScreen(onOnboardingComplete = {
                                    onboardingViewModel.saveOnboardingSeen()
                                    navController.navigate("contactList") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                })
                            }

                            composable("contactList") {
                                ContactListScreen(
                                    viewModel = viewModel,
                                    onContactClick = { id ->
                                        if (id == 0) navController.navigate("editContact/0")
                                        else navController.navigate("contactDetail/$id")
                                    },
                                    onSettingsClick = { navController.navigate("settings") }
                                )
                            }

                            composable(
                                "contactDetail/{contactId}",
                                arguments = listOf(navArgument("contactId") {
                                    type = NavType.IntType
                                })
                            ) { backStackEntry ->
                                val contactId = backStackEntry.arguments?.getInt("contactId") ?: 0
                                ContactDetailScreen(
                                    viewModel = viewModel,
                                    contactId = contactId,
                                    onBackClick = { navController.popBackStack() },
                                    onEditClick = {
                                        navController.navigate("editContact/$contactId")
                                    }
                                )
                            }

                            composable(
                                "editContact/{contactId}",
                                arguments = listOf(navArgument("contactId") {
                                    type = NavType.IntType; defaultValue = 0
                                })
                            ) { entry ->
                                EditContactScreen(
                                    viewModel = viewModel,
                                    contactId = entry.arguments?.getInt("contactId") ?: 0,
                                    onContactUpdated = { navController.popBackStack() },
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable("settings") {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    settingsDataStore = settingsDataStore,
                                    onBackClick = { navController.popBackStack() },
                                    onAboutClick = { navController.navigate(ABOUT_ROUTE) }
                                )
                            }

                            composable(ABOUT_ROUTE) {
                                AboutScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onPrivacyPolicyClick = { navController.navigate(PRIVACY_ROUTE) }
                                )
                            }

                            composable(PRIVACY_ROUTE) {
                                PrivacyPolicyScreen(onBackClick = { navController.popBackStack() })
                            }
                        } // End NavHost
                    } // End Surface
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
    private fun askForPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel("contact_deletion_channel", "Deletions", NotificationManager.IMPORTANCE_HIGH))
            manager.createNotificationChannel(NotificationChannel("brnbook_caller_id_channel", "Caller ID", NotificationManager.IMPORTANCE_HIGH))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactListScreen(viewModel: ContactViewModel, onContactClick: (Int) -> Unit, onSettingsClick: () -> Unit) {
    val groupedContacts by viewModel.groupedContacts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val filteredContacts = if (searchQuery.isEmpty()) groupedContacts else {
        groupedContacts.mapValues { (_, list) ->
            list.filter { it.name.contains(searchQuery, true) || it.phone.contains(searchQuery, true) }
        }.filterValues { it.isNotEmpty() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = searchQuery, onValueChange = { searchQuery = it },
                            placeholder = { Text("Search") }, modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                            singleLine = true, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                        )
                    } else Text("Contacts")
                },
                actions = {
                    if (isSearching) {
                        IconButton(onClick = { isSearching = false; searchQuery = "" }) { Icon(Icons.Default.Close, null) }
                    } else {
                        IconButton(onClick = { isSearching = true }) { Icon(Icons.Default.Search, null) }
                        IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, null) }
                    }
                }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { onContactClick(0) }) { Icon(Icons.Default.Add, null) } }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            filteredContacts.forEach { (letter, contacts) ->
                stickyHeader {
                    Text(text = letter.toString(), modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp, 8.dp), fontWeight = FontWeight.Bold)
                }
                item {
                    Card(modifier = Modifier.padding(16.dp, 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column {
                            contacts.forEachIndexed { index, contact ->
                                ContactRow(contact = contact, onClick = { onContactClick(contact.id) })
                                if (index < contacts.lastIndex) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactRow(contact: Contact, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
            Text(text = contact.name.first().toString(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = contact.name, fontWeight = FontWeight.SemiBold)
            Text(text = contact.phone, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (contact.deletionTimestamp != null) {
            Icon(painter = painterResource(id = R.drawable.ic_timer), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}