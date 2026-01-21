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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

const val ABOUT_ROUTE = "about_page"

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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (hasSeenOnboarding != null) {
                        val navController = rememberNavController()
                        val startDestination = if (hasSeenOnboarding == true) "contactList" else "onboarding"

                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                            enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(500)) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(500)) + fadeOut() },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(500)) + fadeIn() },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(500)) + fadeOut() }
                        ) {
                            composable("onboarding") {
                                val onboardingViewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModelFactory(settingsDataStore))
                                OnboardingScreen(onOnboardingComplete = {
                                    onboardingViewModel.saveOnboardingSeen()
                                    navController.navigate("contactList") { popUpTo("onboarding") { inclusive = true } }
                                })
                            }

                            composable("contactList") {
                                ContactListScreen(
                                    viewModel = viewModel,
                                    onContactClick = { contactId ->
                                        if (contactId == 0) navController.navigate("editContact/0")
                                        else navController.navigate("contactDetail/$contactId")
                                    },
                                    onSettingsClick = { navController.navigate("settings") }
                                )
                            }

                            composable(
                                "contactDetail/{contactId}",
                                arguments = listOf(navArgument("contactId") { defaultValue = 0 })
                            ) { backStackEntry ->
                                val contactId = backStackEntry.arguments?.getInt("contactId") ?: 0
                                ContactDetailScreen(
                                    viewModel = viewModel,
                                    contactId = contactId,
                                    onBackClick = { navController.popBackStack() },
                                    onEditClick = { navController.navigate("editContact/$contactId") }
                                )
                            }

                            composable(
                                "editContact/{contactId}",
                                arguments = listOf(navArgument("contactId") { defaultValue = 0 })
                            ) { backStackEntry ->
                                val contactId = backStackEntry.arguments?.getInt("contactId") ?: 0
                                EditContactScreen(
                                    viewModel = viewModel,
                                    contactId = contactId,
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
                                    navController = navController, // <--- ADD THIS LINE
                                    onBackClick = { navController.popBackStack() },
                                    onPrivacyPolicyClick = { navController.navigate("privacy_policy_screen") }
                                )
                            }

                            composable("privacy_policy_screen") {
                                PrivacyPolicyScreen(onBackClick = { navController.popBackStack() })
                            }

                            composable("licenses") {
                                LicensesScreen(onBackClick = { navController.popBackStack() })
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    private fun askForPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val deletionChannel = NotificationChannel(
                "contact_deletion_channel",
                "Contact Deletion",
                NotificationManager.IMPORTANCE_HIGH
            )
            val callerIdChannel = NotificationChannel(
                "brnbook_caller_id_channel",
                "BrnBook Caller ID",
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(deletionChannel)
            notificationManager.createNotificationChannel(callerIdChannel)
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

    // Logic for filtering by name OR phone number
    val filteredContacts = if (searchQuery.isEmpty()) {
        groupedContacts
    } else {
        groupedContacts.mapValues { (_, contacts) ->
            contacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.phone.contains(searchQuery)
            }
        }.filterValues { it.isNotEmpty() }
    }

    LaunchedEffect(isSearching) {
        if (isSearching) focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search name or number") },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            )
                        )
                    } else {
                        Text("Contacts")
                    }
                },
                actions = {
                    if (isSearching) {
                        IconButton(onClick = { isSearching = false; searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    } else {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onContactClick(0) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (filteredContacts.isEmpty()) {
                if (searchQuery.isNotEmpty()) {
                    // --- CASE 1: NO SEARCH RESULTS FOUND ---
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No results found for \"$searchQuery\"",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // --- CASE 2: EMPTY LIST BRANDING ---
                    EmptyListBranding()
                }
            } else {
                // --- CASE 3: DISPLAY LIST ---
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    filteredContacts.forEach { (letter, contacts) ->
                        stickyHeader {
                            Text(
                                text = letter.toString(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            Card(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column {
                                    contacts.forEachIndexed { index, contact ->
                                        ContactCard(contact = contact, onClick = { onContactClick(contact.id) })
                                        if (index < contacts.lastIndex) {
                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyListBranding() {
    var isAnimated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isAnimated = true }

    val alpha by animateFloatAsState(targetValue = if (isAnimated) 1f else 0f, animationSpec = tween(1000), label = "")
    val scale by animateFloatAsState(targetValue = if (isAnimated) 1f else 0.8f, animationSpec = tween(1000), label = "")

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.logo_png),
            contentDescription = null,
            modifier = Modifier.size(120.dp).graphicsLayer(alpha = alpha * 0.4f, scaleX = scale, scaleY = scale),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Burner Book™", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.graphicsLayer(alpha = alpha))
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Your Contact Book is Empty", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.graphicsLayer(alpha = alpha))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Start by adding a temporary contact.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), textAlign = TextAlign.Center, modifier = Modifier.graphicsLayer(alpha = alpha))
    }

    Box(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp, end = 32.dp)) {
        Row(modifier = Modifier.align(Alignment.BottomEnd).graphicsLayer(alpha = alpha), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Start by adding a contact", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(40.dp).rotate(45f), tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
fun ContactCard(contact: Contact, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
            Text(text = contact.name.first().toString(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = contact.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(text = contact.phone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // --- UPDATED TIMER ICON LOGIC ---
        if (contact.deletionTimestamp != null) {
            Icon(
                painter = painterResource(id = R.drawable.ic_timer),
                contentDescription = "Timer Active",
                // This ensures it uses the theme's primary color
                // (which is bright/high contrast in Dark Mode)
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}