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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tempcontacts.ui.theme.TempContactsTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ContactViewModel by viewModels()
    private lateinit var settingsDataStore: SettingsDataStore

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted.
        } else {
            // Handle permission denial.
        }
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
                if (hasSeenOnboarding != null) {
                    val navController = rememberNavController()
                    val startDestination = if (hasSeenOnboarding == true) "contactList" else "onboarding"

                    NavHost(navController = navController, startDestination = startDestination) {
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
                                    if (contactId == 0) {
                                        navController.navigate("editContact/0")
                                    } else {
                                        navController.navigate("contactDetail/$contactId")
                                    }
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
                                onBackClick = { navController.popBackStack() }
                            )
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