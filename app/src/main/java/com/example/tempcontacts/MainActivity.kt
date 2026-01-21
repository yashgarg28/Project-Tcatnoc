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
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

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
                // --- FIX: Surface Wrapper ---
                // This Surface ensures the background behind the sliding animations
                // matches the theme (Dark/Light) instead of the default white Window.
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
                            // --- GLOBAL SWIPE ANIMATIONS ---
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
                                    onBackClick = { navController.popBackStack() },
                                    onPrivacyPolicyClick = { navController.navigate("privacy_policy_screen") }
                                )
                            }

                            composable("privacy_policy_screen") {
                                PrivacyPolicyScreen(onBackClick = { navController.popBackStack() })
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
            ).apply {
                description = "Notifies when a contact is deleted."
            }

            val callerIdChannel = NotificationChannel(
                "brnbook_caller_id_channel",
                "BrnBook Caller ID",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Displays an overlay for incoming calls from temporary contacts."
            }

            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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

    val filteredContacts = if (searchQuery.isEmpty()) {
        groupedContacts
    } else {
        groupedContacts.mapValues { (_, contacts) ->
            contacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.phone.contains(searchQuery, ignoreCase = true)
            }
        }.filterValues { it.isNotEmpty() }
    }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search contacts") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
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
                        IconButton(onClick = {
                            isSearching = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search")
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
        if (groupedContacts.isEmpty() && searchQuery.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {

                // --- 1. Animation State ---
                var isAnimated by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { isAnimated = true }

                val alpha by animateFloatAsState(
                    targetValue = if (isAnimated) 1f else 0f,
                    animationSpec = tween(1000),
                    label = "Alpha"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isAnimated) 1f else 0.8f,
                    animationSpec = tween(1000),
                    label = "Scale"
                )

                // --- 2. Central Branding & Text ---
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.logo_png),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(120.dp)
                            .graphicsLayer(
                                alpha = alpha * 0.4f,
                                scaleX = scale,
                                scaleY = scale
                            ),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Burner Book™",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        // FIX: Use onBackground so it's black in light mode and white in dark mode
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.graphicsLayer(alpha = alpha, scaleX = scale, scaleY = scale)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Your Contact Book is Empty",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.graphicsLayer(alpha = alpha, scaleX = scale, scaleY = scale)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Start by adding a temporary contact.",
                        style = MaterialTheme.typography.bodyMedium,
                        // FIX: Use onBackground with alpha for the subtext
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer(alpha = alpha, scaleX = scale, scaleY = scale)
                    )
                }

// --- 3. Arrow pointing to the Button ---
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 100.dp, end = 32.dp)
                        .graphicsLayer(alpha = alpha),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Start by adding a contact",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.offset(y = (-10).dp)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .rotate(45f),
                        // FIX: Ensure icon color also follows the theme
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
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
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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

@Composable
fun ContactCard(contact: Contact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = contact.name.first().toString(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = contact.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(text = contact.phone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (contact.deletionTimestamp != null) {
            Icon(painter = painterResource(id = R.drawable.ic_timer), contentDescription = "Timer Active", tint = MaterialTheme.colorScheme.primary)
        }
    }
}