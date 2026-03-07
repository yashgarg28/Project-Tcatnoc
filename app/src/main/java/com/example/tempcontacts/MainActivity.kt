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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.style.TextOverflow
import kotlin.collections.firstOrNull
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.blur

const val ABOUT_ROUTE = "about_page"

class MainActivity : ComponentActivity() {

    private val viewModel: ContactViewModel by viewModels()
    private lateinit var settingsDataStore: SettingsDataStore

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
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
                                        settingsDataStore = settingsDataStore,
                                        onEditClick = { contactId ->
                                            navController.navigate("editContact/$contactId")
                                        },
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
                                        isDarkTheme = useDarkTheme,
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
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val needsReadContacts = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED

            val needsWriteContacts = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED

            if (needsReadContacts || needsWriteContacts) {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS
                    )
                )
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
    onEditClick: (Int) -> Unit,
    onContactClick: (Int) -> Unit,
    onSettingsClick: () -> Unit
) {
    val groupedContacts by viewModel.groupedContacts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var showWhatsAppDialog by remember { mutableStateOf(false) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedContactId by remember { mutableStateOf<Int?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contactToDelete by remember { mutableStateOf<Contact?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var menuPosition by remember { mutableStateOf(Offset.Zero) }

    var selectedFilterTag by remember { mutableStateOf("All") }
    val databaseTags by viewModel.allExistingTags.collectAsState()
    val filterOptions = remember(databaseTags) {
        listOf("All") + (listOf("Work", "Delivery", "Social", "Personal") + databaseTags).distinct()
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(60000)
        }
    }

    val filteredContacts = groupedContacts.mapValues { (_, contacts) ->
        contacts.filter { contact ->
            val matchesSearch = searchQuery.isEmpty() ||
                    contact.name.contains(searchQuery, true) ||
                    contact.phone.contains(searchQuery) ||
                    contact.notes.contains(searchQuery, true)

            val matchesTag = selectedFilterTag == "All" || contact.tag == selectedFilterTag

            matchesSearch && matchesTag
        }
    }.filterValues { it.isNotEmpty() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .blur(radius = if (showContextMenu) 25.dp else 0.dp)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (isSearching) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search...") },
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        } else { Text("Contacts") }
                    },
                    actions = {
                        if (isSearching) {
                            IconButton(onClick = { isSearching = false; searchQuery = "" }) {
                                Icon(Icons.Default.Close, null)
                            }
                        } else {
                            if (groupedContacts.isNotEmpty()) {
                                IconButton(onClick = { isSearching = true }) {
                                    Icon(Icons.Default.Search, null)
                                }
                            }
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Default.Settings, null)
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.End) {
                    FloatingActionButton(onClick = { showWhatsAppDialog = true }) {
                        Icon(painterResource(R.drawable.ic_whatsapp), "WhatsApp", tint = Color.Unspecified)
                    }
                    FloatingActionButton(onClick = { onContactClick(0) }) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                if (groupedContacts.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filterOptions) { tag ->
                            val isSelected = selectedFilterTag == tag
                            val tagAttrs = if (tag != "All") getTagAttributes(tag) else null

                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilterTag = tag },
                                label = { Text(tag) },
                                leadingIcon = if (tagAttrs != null) {
                                    {
                                        Icon(
                                            imageVector = tagAttrs.third,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else tagAttrs.second
                                        )
                                    }
                                } else null,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    if (filteredContacts.isEmpty()) {
                        if (searchQuery.isNotEmpty() || selectedFilterTag != "All") {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No results found for \"$searchQuery\""
                                    else "No contacts tagged as '$selectedFilterTag'",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(32.dp)
                                )
                            }
                        } else {
                            EmptyListBranding(isDarkTheme)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            filteredContacts.forEach { (letter, contacts) ->
                                stickyHeader {
                                    Text(
                                        letter.toString(),
                                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp, 8.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                item {
                                    Card(Modifier.padding(16.dp, 8.dp), shape = RoundedCornerShape(12.dp)) {
                                        Column {
                                            contacts.forEachIndexed { i, c ->
                                                ContactCard(
                                                    contact = c,
                                                    isDarkTheme = isDarkTheme,
                                                    currentTime = currentTime,
                                                    searchQuery = searchQuery,
                                                    onClick = { onContactClick(c.id) },
                                                    onLongPress = { contactId, offset ->
                                                        selectedContactId = contactId
                                                        menuPosition = offset
                                                        showContextMenu = true
                                                    }
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
        }
    }

    // ✅ CONTEXT MENU OVERLAY
    if (showContextMenu && selectedContactId != null) {
        val allContacts by viewModel.allContacts.collectAsState()
        val selectedContact = allContacts.find { it.id == selectedContactId }
        val contextForMenu = LocalContext.current

        if (selectedContact != null) {
            // Calculate remaining time
            val remainingMillis = if (selectedContact.deletionTimestamp != null) {
                selectedContact.deletionTimestamp!! - currentTime
            } else {
                0L
            }

            val remainingDays = (remainingMillis / (1000 * 60 * 60 * 24)).toInt()
            val remainingHours = ((remainingMillis % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)).toInt()
            val timeRemaining = if (remainingDays > 0) {
                "$remainingDays days, $remainingHours hours"
            } else {
                "$remainingHours hours"
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showContextMenu = false }
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(300.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ✅ TIME REMAINING - Displayed above card
                    if (selectedContact.deletionTimestamp != null && remainingMillis > 0) {
                        Text(
                            text = "Time remaining: $timeRemaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(bottom = 8.dp)
                        )
                    }

                    // Selected contact card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedContact.name.firstOrNull()?.toString() ?: "?",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedContact.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = selectedContact.phone,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (selectedContact.tag != "None") {
                                    val (bgColor, contentColor, icon) = getTagAttributes(selectedContact.tag)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = bgColor,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(22.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = contentColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = selectedContact.tag,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = contentColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }

                            if (selectedContact.deletionTimestamp != null) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_timer),
                                    contentDescription = "Expiration Timer",
                                    tint = getTimerColor(selectedContact.deletionTimestamp, currentTime, isDarkTheme)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ✅ CONTEXT MENU
                    ContactContextMenu(
                        contact = selectedContact,
                        position = Offset.Zero,
                        onDismiss = { showContextMenu = false },
                        onEdit = {
                            showContextMenu = false
                            onEditClick(selectedContact.id)
                        },
                        onCall = {
                            showContextMenu = false
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${selectedContact.phone}"))
                            contextForMenu.startActivity(intent)
                        },
                        onMessage = {
                            showContextMenu = false
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${selectedContact.phone}"))
                            contextForMenu.startActivity(intent)
                        },
                        onDelete = {
                            showContextMenu = false
                            showDeleteConfirmation = true
                            contactToDelete = selectedContact
                        },
                        onExtendTimer = { days: Int ->
                            showContextMenu = false
                            // Get the fresh contact data to ensure we have the latest timer
                            val freshContact = allContacts.find { it.id == selectedContact.id }
                            if (freshContact != null) {
                                // Add days to the EXISTING deletion timestamp
                                val currentDeletionTime = freshContact.deletionTimestamp ?: System.currentTimeMillis()
                                val newDeletionTime = currentDeletionTime + (days * 24 * 60 * 60 * 1000L)
                                val updatedContact = freshContact.copy(deletionTimestamp = newDeletionTime)
                                viewModel.update(updatedContact)
                            }
                        },
                        onSaveForever = {
                            showContextMenu = false
                            ContactExporter.exportToPhoneBook(contextForMenu, selectedContact)
                        }
                    )
                }
            }
        }
    }

    // ✅ DELETE CONFIRMATION DIALOG
    if (showDeleteConfirmation && contactToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Contact?") },
            text = { Text("Are you sure you want to delete \"${contactToDelete!!.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(contactToDelete!!)
                        showDeleteConfirmation = false
                        contactToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showWhatsAppDialog) WhatsAppDialog(settingsDataStore) { showWhatsAppDialog = false }
}

@Composable
fun EmptyListBranding(isDarkTheme: Boolean) {
    var isAnimated by remember { mutableStateOf(false) }

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

    Box(modifier = Modifier.fillMaxSize().padding(bottom = 90.dp, end = 90.dp)) {
        Row(
            Modifier.align(Alignment.BottomEnd).graphicsLayer(alpha = alpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Start by adding a contact",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = brandColor.copy(alpha = 0.8f)
                )
                Text(
                    text = "or message on WhatsApp",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = brandColor.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.ArrowForward,
                null,
                modifier = Modifier.size(32.dp),
                tint = brandColor
            )
        }
    }
}

@Composable
fun getTimerColor(deletionTimestamp: Long?, currentTime: Long, isDarkTheme: Boolean): Color {
    if (deletionTimestamp == null) return MaterialTheme.colorScheme.primary

    val remainingMillis = deletionTimestamp - currentTime

    val oneDay = 24 * 60 * 60 * 1000L
    val fiveDays = 5 * oneDay

    return when {
        remainingMillis <= 0 -> Color.Gray
        remainingMillis < oneDay -> Color(0xFFFF5252)
        remainingMillis < fiveDays -> Color(0xFFFFB74D)
        else -> if (isDarkTheme) Color.White else Color(0xFF2196F3)
    }
}

@Composable
fun ContactCard(
    contact: Contact,
    isDarkTheme: Boolean,
    currentTime: Long,
    searchQuery: String,
    onClick: () -> Unit,
    onLongPress: ((Int, Offset) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        onLongPress?.invoke(contact.id, offset)
                    },
                    onTap = { onClick() }
                )
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = contact.phone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (contact.tag != "None") {
                val (bgColor, contentColor, icon) = getTagAttributes(contact.tag)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(22.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = contact.tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            val isMatchInNote = searchQuery.isNotBlank() &&
                    contact.notes.contains(searchQuery, ignoreCase = true)

            if (isMatchInNote) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = contact.notes,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

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

    val savedCountryCode by settingsDataStore
        .lastCountryCodeFlow
        .collectAsState(initial = null)

    var country by remember {
        mutableStateOf(
            countryList.find { it.code == "+91" } ?: countryList.first()
        )
    }

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