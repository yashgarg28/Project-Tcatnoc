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
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.appdistribution.InterruptionLevel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.text.style.TextOverflow
import kotlin.collections.firstOrNull
import kotlinx.coroutines.launch

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
        setupFirebaseFeedback()

        setContent {
            val theme by settingsDataStore.themeFlow.collectAsState(initial = "System")
            val useDarkTheme = when (theme) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            val hasSeenOnboarding by settingsDataStore.hasSeenOnboardingFlow.collectAsState(initial = null)
            val snackbarHostState = remember { SnackbarHostState() }

            TempContactsTheme(darkTheme = useDarkTheme) {
                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { padding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = padding.calculateBottomPadding()),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (hasSeenOnboarding != null) {
                            val navController = rememberNavController()
                            val startDestination = if (hasSeenOnboarding == true) "contactList" else "onboarding"

                            NavHost(
                                navController = navController,
                                startDestination = startDestination
                            ) {
                                composable("onboarding") {
                                    val onboardingViewModel: OnboardingViewModel = viewModel(
                                        factory = OnboardingViewModelFactory(settingsDataStore)
                                    )
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
                                        isDarkTheme = useDarkTheme,
                                        settingsDataStore = settingsDataStore, // ✅ PASS IT
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
                                        settingsDataStore = settingsDataStore,
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
                                        navController = navController,
                                        isDarkTheme = useDarkTheme,
                                        onBackClick = { navController.popBackStack() },
                                        onPrivacyPolicyClick = { navController.navigate("privacy_policy_screen") }
                                    )
                                }

                                composable("privacy_policy_screen") {
                                    PrivacyPolicyScreen { navController.popBackStack() }
                                }
                                composable("licenses") {
                                    LicensesScreen { navController.popBackStack() }
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
    }

    private fun setupFirebaseFeedback() {
        val appDistro = FirebaseAppDistribution.getInstance()
        appDistro.showFeedbackNotification(
            "Found a bug? Tap to send feedback!",
            InterruptionLevel.DEFAULT
        )
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
            manager.createNotificationChannel(
                NotificationChannel("contact_deletion_channel", "Contact Deletion", NotificationManager.IMPORTANCE_HIGH)
            )
            manager.createNotificationChannel(
                NotificationChannel("brnbook_caller_id_channel", "BrnBook Caller ID", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactListScreen(
    viewModel: ContactViewModel,
    isDarkTheme: Boolean,
    settingsDataStore: SettingsDataStore,
    onContactClick: (Int) -> Unit,
    onSettingsClick: () -> Unit
) {
    val groupedContacts by viewModel.groupedContacts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var showWhatsAppDialog by remember { mutableStateOf(false) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(60000)
        }
    }
    // Updated filtering logic including notes
    val filteredContacts = if (searchQuery.isEmpty()) groupedContacts else {
        groupedContacts.mapValues { (_, c) ->
            c.filter { it.name.contains(searchQuery, true) || it.phone.contains(searchQuery) || it.notes.contains(searchQuery, true) }
        }.filterValues { it.isNotEmpty() }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search name, phone, or notes...") },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                        )
                    } else { Text("Contacts") }
                },
                actions = {
                    if (isSearching) {
                        IconButton(onClick = { isSearching = false; searchQuery = "" }) { Icon(Icons.Default.Close, null) }
                    } else if (groupedContacts.isNotEmpty()) {
                        IconButton(onClick = { isSearching = true }) { Icon(Icons.Default.Search, null) }
                        IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, null) }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.End) {
                FloatingActionButton(onClick = { showWhatsAppDialog = true }) { Icon(painterResource(R.drawable.ic_whatsapp), "WhatsApp", tint = Color.Unspecified) }
                FloatingActionButton(onClick = { onContactClick(0) }) { Icon(Icons.Default.Add, null) }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (filteredContacts.isEmpty()) {
                if (searchQuery.isNotEmpty()) Text("No results found", modifier = Modifier.align(Alignment.Center))
                else EmptyListBranding(isDarkTheme)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    filteredContacts.forEach { (letter, contacts) ->
                        stickyHeader { Text(letter.toString(), Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp, 8.dp), fontWeight = FontWeight.Bold) }
                        item {
                            Card(Modifier.padding(16.dp, 8.dp), shape = RoundedCornerShape(12.dp)) {
                                Column {
                                    contacts.forEachIndexed { i, c ->
                                        ContactCard(
                                            contact = c,
                                            isDarkTheme = isDarkTheme,
                                            currentTime = currentTime,
                                            searchQuery = searchQuery, // ✅ Pass the search query here
                                            onClick = { onContactClick(c.id) }
                                        )
                                        if (i < contacts.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showWhatsAppDialog) WhatsAppDialog(settingsDataStore) { showWhatsAppDialog = false }
}

@Composable
fun EmptyListBranding(isDarkTheme: Boolean) {
    var isAnimated by remember { mutableStateOf(false) }

    // Logic: Black text for Light Mode, White text for Dark Mode
    val brandColor = if (isDarkTheme) Color.White else Color.Black

    LaunchedEffect(Unit) { isAnimated = true }

    val alpha by animateFloatAsState(if (isAnimated) 1f else 0f, tween(1000), label = "")
    val scale by animateFloatAsState(if (isAnimated) 1f else 0.8f, tween(1000), label = "")

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Image(
            painter = painterResource(id = if (isDarkTheme) R.mipmap.burnerlogowhite else R.mipmap.burnerlogoblue),
            contentDescription = "Logo",
            modifier = Modifier.size(200.dp).graphicsLayer(alpha = alpha, scaleX = scale, scaleY = scale),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Your Contact Book is Empty",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = brandColor,
            modifier = Modifier.graphicsLayer(alpha = alpha)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start by adding a temporary contact.",
            color = brandColor.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer(alpha = alpha)
        )
    }

    Box(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp, end = 32.dp)) {
        Row(
            Modifier.align(Alignment.BottomEnd).graphicsLayer(alpha = alpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Start by adding a contact",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = brandColor.copy(alpha = 0.8f)
            )
            Icon(
                Icons.Default.ArrowForward,
                null,
                modifier = Modifier.size(40.dp).rotate(45f),
                tint = brandColor
            )
        }
    }
}

@Composable
fun getTimerColor(deletionTimestamp: Long?, currentTime: Long, isDarkTheme: Boolean): Color {
    if (deletionTimestamp == null) return MaterialTheme.colorScheme.primary

    val remainingMillis = deletionTimestamp - currentTime

    // NEW SCALING:
    val oneDay = 24 * 60 * 60 * 1000L      // 24 Hours
    val fiveDays = 5 * oneDay              // 5 Days

    return when {
        remainingMillis <= 0 -> Color.Gray

        // 🔴 Red if less than 24 hours left
        remainingMillis < oneDay -> Color(0xFFFF5252)

        // 🟠 Orange if less than 5 days left
        remainingMillis < fiveDays -> Color(0xFFFFB74D)

        // 🔵/⚪ Brand color if more than 5 days left
        else -> if (isDarkTheme) Color.White else Color(0xFF2196F3)
    }
}

@Composable
fun ContactCard(
    contact: Contact,
    isDarkTheme: Boolean,
    currentTime: Long,
    searchQuery: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- 1. Circle Avatar ---
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.firstOrNull()?.toString() ?: "?",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // --- 2. Contact Details ---
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            // ✅ Match logic: Is searching AND note contains query?
            val isMatchInNote = searchQuery.isNotBlank() &&
                    contact.notes.contains(searchQuery, ignoreCase = true)

            if (isMatchInNote) {
                Spacer(modifier = Modifier.height(4.dp))
                // 📦 The Note Box (Styled like the Detail Screen)
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(4.dp), // Slightly smaller corner for the list
                ) {
                    Text(
                        text = contact.notes,
                        style = MaterialTheme.typography.labelSmall, // Smaller text for the list
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = contact.phone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // --- 3. Dynamic Timer Icon ---
        if (contact.deletionTimestamp != null) {
            Icon(
                painter = painterResource(id = R.drawable.ic_timer),
                contentDescription = "Expiration Timer",
                tint = getTimerColor(contact.deletionTimestamp, currentTime, isDarkTheme)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppDialog(
    settingsDataStore: SettingsDataStore,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var countryCodeExpanded by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }

    // 🌍 Load saved country
    val savedCountryCode by settingsDataStore
        .lastCountryCodeFlow
        .collectAsState(initial = null)

    // 🌍 Selected country (default India)
    var country by remember {
        mutableStateOf(
            countryList.find { it.code == "+91" } ?: countryList.first()
        )
    }

    // 🔁 Restore last used country
    LaunchedEffect(savedCountryCode) {
        savedCountryCode?.let { code ->
            countryList.find { it.code == code }?.let {
                country = it
            }
        }
    }

    val isPhoneNumberValid = phone.length == country.phoneLength

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open WhatsApp Chat") },
        text = {
            Column {

                Text("Enter phone number")

                Spacer(Modifier.height(8.dp))

                // 🌍 Country Picker
                ExposedDropdownMenuBox(
                    expanded = countryCodeExpanded,
                    onExpandedChange = { countryCodeExpanded = !countryCodeExpanded }
                ) {
                    OutlinedTextField(
                        value = "${country.flagEmoji} ${country.code}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Country") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = countryCodeExpanded
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = countryCodeExpanded,
                        onDismissRequest = { countryCodeExpanded = false }
                    ) {
                        countryList.forEach { c ->
                            DropdownMenuItem(
                                text = {
                                    Text("${c.flagEmoji} ${c.name} (${c.code})")
                                },
                                onClick = {
                                    country = c
                                    countryCodeExpanded = false

                                    scope.launch {
                                        settingsDataStore.saveLastCountryCode(c.code)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 📞 Phone input
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                            .filter(Char::isDigit)
                            .take(country.phoneLength)
                    },
                    label = { Text("Phone") },
                    isError = phone.isNotEmpty() && !isPhoneNumberValid,
                    supportingText = {
                        if (phone.isNotEmpty() && !isPhoneNumberValid) {
                            Text("Must be ${country.phoneLength} digits")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = isPhoneNumberValid,
                onClick = {
                    val fullNumber =
                        "${country.code.replace("+", "")}$phone"

                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://wa.me/$fullNumber")
                    )

                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "WhatsApp not installed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    onDismiss()
                }
            ) {
                Text("Open WhatsApp")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
