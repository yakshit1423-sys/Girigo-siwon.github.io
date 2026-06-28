package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.WishDatabase
import com.example.data.WishEntity
import com.example.data.WishRepository
import com.example.receiver.WishNotificationHelper
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.JetBlack
import com.example.ui.theme.BloodRed
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.GhostWhite
import com.example.ui.theme.SoftGray
import com.example.ui.theme.CreepyGreen
import com.example.ui.theme.OccultGold
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.WishViewModel
import com.example.viewmodel.WishViewModelFactory
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = WishDatabase.getDatabase(applicationContext)
        val repository = WishRepository(database.wishDao())

        setContent {
            MyApplicationTheme {
                val viewModel: WishViewModel = viewModel(
                    factory = WishViewModelFactory(
                        application = application,
                        repository = repository
                    )
                )

                // Scaffold with safe drawing padding
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    GirigoWishAppScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun GirigoWishAppScreen(
    viewModel: WishViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeWish by viewModel.activeWish.collectAsStateWithLifecycle()
    val allWishes by viewModel.allWishes.collectAsStateWithLifecycle()
    val remainingTimeMs by viewModel.remainingTimeMs.collectAsStateWithLifecycle()
    val isCreepyMode by viewModel.isCreepyMode.collectAsStateWithLifecycle()
    val spookyMessage by viewModel.spookyMessage.collectAsStateWithLifecycle()
    val heartbeatRateMs by viewModel.heartbeatRateMs.collectAsStateWithLifecycle()
    val hasJustExpired by viewModel.hasJustExpired.collectAsStateWithLifecycle()

    var wishInput by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    var showDeveloperPanel by remember { mutableStateOf(false) }
    var isBreakingPactPromptOpen by remember { mutableStateOf(false) }

    // Dynamic scale for the creepy pulse
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isCreepyMode) 1.12f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = heartbeatRateMs.toInt(),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Dynamic red heartbeat overlay alpha
    val pulseOverlayAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = if (isCreepyMode) 0.35f else 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = heartbeatRateMs.toInt(),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Glitch offset state for creepy mode
    var glitchOffset by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(isCreepyMode) {
        if (isCreepyMode) {
            while (true) {
                glitchOffset = if (Random.nextInt(5) == 0) {
                    Offset(
                        x = Random.nextInt(-8, 9).toFloat(),
                        y = Random.nextInt(-8, 9).toFloat()
                    )
                } else {
                    Offset.Zero
                }
                delay(120L)
            }
        } else {
            glitchOffset = Offset.Zero
        }
    }

    // Dynamic blood drips simulation state
    val bloodDrips = remember { mutableStateListOf<BloodDrip>() }
    LaunchedEffect(isCreepyMode) {
        if (isCreepyMode) {
            // Seed blood drips
            if (bloodDrips.isEmpty()) {
                repeat(8) {
                    bloodDrips.add(
                        BloodDrip(
                            x = Random.nextFloat(),
                            y = Random.nextFloat() * -0.5f,
                            length = Random.nextFloat() * 100f + 50f,
                            speed = Random.nextFloat() * 4f + 2f
                        )
                    )
                }
            }
            while (true) {
                for (i in bloodDrips.indices) {
                    val drip = bloodDrips[i]
                    var newY = drip.y + (drip.speed / 1000f)
                    if (newY > 1.2f) {
                        newY = -0.1f
                    }
                    bloodDrips[i] = drip.copy(y = newY)
                }
                delay(16)
            }
        } else {
            bloodDrips.clear()
        }
    }

    // Initialize notification channel
    LaunchedEffect(Unit) {
        WishNotificationHelper.initChannel(context)
    }

    // Handle Notification Permission (Android 13+)
    var hasNotificationPermission by remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            )
        } else {
            mutableStateOf(true)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JetBlack)
    ) {
        // Atmospheric Gothic Background Image with custom blood red overlay
        Image(
            painter = painterResource(id = R.drawable.img_gothic_well),
            contentDescription = "Gothic Well",
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (isCreepyMode) 0.15f else 0.3f),
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(
                color = if (isCreepyMode) BloodRed else Color.Unspecified,
                blendMode = androidx.compose.ui.graphics.BlendMode.ColorBurn
            )
        )

        // Vignette Blood Glow overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val center = Offset(size.width / 2, size.height / 2)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                BloodRed.copy(alpha = pulseOverlayAlpha)
                            ),
                            center = center,
                            radius = size.width * 1.2f
                        )
                    )
                }
        )

        // Dripping blood Canvas overlay
        if (isCreepyMode) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                bloodDrips.forEach { drip ->
                    val dripX = drip.x * size.width
                    val dripY = drip.y * size.height
                    drawLine(
                        color = CrimsonRed.copy(alpha = 0.85f),
                        start = Offset(dripX, dripY),
                        end = Offset(dripX, dripY + drip.length),
                        strokeWidth = 6f
                    )
                    drawCircle(
                        color = CrimsonRed,
                        radius = 8f,
                        center = Offset(dripX, dripY + drip.length)
                    )
                }
            }
        }

        // Main Scrollable Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Spooky Spacing
            item { Spacer(modifier = Modifier.height(24.dp)) }

            // App Header (Bold Typography Theme)
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .background(Color(0xFF2D0000), RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0xFF4D0000), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (activeWish != null) "Wish #8,812 Granted" else "AWAITING LONGING",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFFFF3131),
                                letterSpacing = 2.sp
                            )
                        )
                    }
                    Text(
                        text = "MANIFESTATION IN PROGRESS",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Light,
                            fontSize = 18.sp,
                            color = Color(0xFFE0E0E0),
                            letterSpacing = 4.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .testTag("app_title")
                    )
                    Text(
                        text = "GIRIGO: THE WELL OF PACTS",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = Color(0xFFFF3131),
                            letterSpacing = 5.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            // Central State Handling
            val currentActive = activeWish
            if (currentActive == null) {
                // STATE A: Input New Wish
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, Color(0xFF4D0000), RoundedCornerShape(16.dp))
                            .testTag("input_card"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0000))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "WHISPER INTO THE ABYSS",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color(0xFFE0E0E0),
                                    letterSpacing = 2.sp
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "Girigo hears your deepest longing. Write your desire below, and it will be granted instantly. However, a pact is forever, and the 24-hour countdown will commence.",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.sp,
                                    color = Color(0xFFFFBABA),
                                    lineHeight = 18.sp,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.padding(bottom = 20.dp)
                            )

                            // Custom Text Field
                            TextField(
                                value = wishInput,
                                onValueChange = { wishInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .border(1.dp, Color(0xFF4D0000), RoundedCornerShape(8.dp))
                                    .testTag("wish_input"),
                                placeholder = {
                                    Text(
                                        text = "E.g., I wish to excel in my endeavors...",
                                        color = Color(0xFFFFBABA).copy(alpha = 0.5f),
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 14.sp
                                    )
                                },
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 15.sp,
                                    color = Color(0xFFE0E0E0)
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0A0000),
                                    unfocusedContainerColor = Color(0xFF0A0000),
                                    focusedIndicatorColor = Color(0xFFFF3131),
                                    unfocusedIndicatorColor = Color(0xFF4D0000),
                                    cursorColor = Color(0xFFFF3131)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // SEAL THE PACT Button (Heavy, Premium Bold Typography Style)
                            Button(
                                onClick = {
                                    if (wishInput.isNotBlank()) {
                                        viewModel.makeWish(wishInput)
                                        wishInput = ""
                                    }
                                },
                                enabled = wishInput.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("seal_pact_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF3131),
                                    contentColor = Color(0xFF000000),
                                    disabledContainerColor = Color(0xFF4D0000),
                                    disabledContentColor = Color(0xFFFFBABA).copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (wishInput.isNotBlank()) Color(0xFF000000) else Color(0xFFFFBABA).copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SEAL THE PACT",
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // STATE B: Active Wish & Countdown (Bold Typography Style)
                item {
                    val progress = remainingTimeMs.toDouble() / (24 * 60 * 60 * 1000L)
                    val displayTime = formatTime(remainingTimeMs)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.5.dp,
                                color = if (isCreepyMode) Color(0xFFFF0000) else Color(0xFF4D0000),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .testTag("active_wish_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCreepyMode) Color(0xFF130303) else Color(0xFF1A0000)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Pact Status Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFFFF3131), RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isCreepyMode) "CURSE SEED IS FULLY DETONATED" else "PACT ACTIVE & DESIRE SEALED",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = Color(0xFFFF3131),
                                        letterSpacing = 1.5.sp
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Wish Text Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = Color(0xFF0A0000),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFF4D0000),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "YOUR WHISPERED REQUEST",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = Color(0xFFFFBABA),
                                            letterSpacing = 1.sp
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "\"${currentActive.wishText}\"",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 15.sp,
                                            color = Color(0xFFE0E0E0),
                                            textAlign = TextAlign.Center,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .offset(
                                                x = glitchOffset.x.dp,
                                                y = glitchOffset.y.dp
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Dual-Ring Circular Countdown Timer (Exact Bold Typography styling)
                            Box(
                                modifier = Modifier
                                    .size(220.dp)
                                    .align(Alignment.CenterHorizontally),
                                contentAlignment = Alignment.Center
                            ) {
                                // Outer border ring
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(1.dp, Color(0xFF4D0000).copy(alpha = 0.5f), RoundedCornerShape(110.dp))
                                )
                                // Inner pulsing ring
                                Box(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxSize()
                                        .border(2.dp, Color(0xFFFF0000).copy(alpha = pulseOverlayAlpha), RoundedCornerShape(102.dp))
                                )

                                // Centered Timer Details
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .offset(x = glitchOffset.x.dp, y = glitchOffset.y.dp)
                                ) {
                                    Text(
                                        text = if (isCreepyMode) "TIME TILL CLAIM" else "FINAL COUNTDOWN",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            color = Color(0xFFFF3131).copy(alpha = 0.7f),
                                            letterSpacing = 3.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = displayTime,
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 32.sp,
                                            color = Color(0xFFFF3131),
                                            letterSpacing = (-0.5).sp,
                                            textAlign = TextAlign.Center
                                        ),
                                        modifier = Modifier
                                            .scale(pulseScale)
                                            .testTag("countdown_timer")
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFFFF0000), RoundedCornerShape(3.dp))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Synchronizing...",
                                            style = TextStyle(
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp,
                                                color = Color(0xFFFFBABA),
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Dynamic Warning Message
                            Text(
                                text = spookyMessage,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF3131),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (isCreepyMode) 1.0f else 0.8f)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Simple linear timeline of remaining pact
                            LinearProgressIndicator(
                                progress = { progress.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = Color(0xFFFF3131),
                                trackColor = Color(0xFF0A0000)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Sever Pact Action Button (Heavy Outlined Black Style)
                            OutlinedButton(
                                onClick = { isBreakingPactPromptOpen = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("sever_pact_button"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFF3131)
                                ),
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    color = Color(0xFF4D0000)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFFFF3131)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SEVER THE PACT",
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }

            // Collapsible Past Chronicles (History)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color(0xFF4D0000), RoundedCornerShape(12.dp))
                        .testTag("history_collapsible_header"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0000)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHistory = !showHistory }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = null,
                                tint = Color(0xFFFF3131),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "CHRONICLE OF GRANTED DESIRES",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.sp,
                                color = Color(0xFFE0E0E0),
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            )
                        }
                        Icon(
                            imageVector = if (showHistory) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFFFFBABA)
                        )
                    }
                }
            }

            if (showHistory) {
                if (allWishes.isEmpty()) {
                    item {
                        Text(
                            text = "No pacts have been sealed in the ledger yet...",
                            color = Color(0xFFFFBABA),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(allWishes) { wish ->
                        HistoryWishItem(wish)
                    }
                    item {
                        Button(
                            onClick = { viewModel.clearAllHistory() },
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .testTag("clear_history_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFFFFBABA)
                            )
                        ) {
                            Text(
                                text = "WIPE CHRONICLE CLEAN",
                                textDecoration = TextDecoration.Underline,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            // Collapsible Developer/Reviewer Curse Control Room (EXCELLENT FOR TESTING)
            if (activeWish != null) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dev_portal_header"),
                        colors = CardDefaults.cardColors(containerColor = CreepyGreen.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDeveloperPanel = !showDeveloperPanel }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Green,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Curse Control Room (Demo Mode)",
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 14.sp,
                                    color = Color.Green,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = if (showDeveloperPanel) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                tint = Color.Green,
                                contentDescription = null
                            )
                        }
                    }
                }

                if (showDeveloperPanel) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, Color.Green.copy(alpha = 0.4f), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)),
                            colors = CardDefaults.cardColors(containerColor = JetBlack.copy(alpha = 0.9f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "To test the psychological impact without waiting 24 hours, warp the time remaining below:",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 12.sp,
                                        color = Color.Green,
                                        lineHeight = 16.sp
                                    ),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ControlWarpButton("24 Hours", "STANDARD", viewModel)
                                    ControlWarpButton("10 Minutes", "TEN_MINUTES", viewModel)
                                    ControlWarpButton("5m 10s (Transition)", "FIVE_MINUTES_TEN_SECONDS", viewModel)
                                    ControlWarpButton("10 Seconds (Claim)", "TEN_SECONDS", viewModel)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Spacer to prevent gesture bar occlusion
            item { Spacer(modifier = Modifier.height(48.dp)) }
        }

        // FULL SCREEN OVERLAY: PACT COMPLETED (EXPIRED CURSE)
        AnimatedVisibility(
            visible = hasJustExpired,
            enter = fadeIn(animationSpec = tween(1200)),
            exit = fadeOut(animationSpec = tween(800))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF030000))
                    .testTag("expired_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "💀 THE DEBT IS CLAIMED 💀",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = 26.sp,
                            color = Color(0xFFFF3131),
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "The 24-hour cycle has finished.\n\nGirigo has fulfilled your wish completely.\nThe price has been claimed in full.\n\nYour pact is permanently sealed.",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 15.sp,
                            color = Color(0xFFE0E0E0),
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Center
                        )
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = { viewModel.dismissExpirationScreen() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("dismiss_expiry_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF3131),
                            contentColor = Color(0xFF000000)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "RETURN TO THE WELL",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }

        // DIALOG: SEVER PACT WARNING
        if (isBreakingPactPromptOpen) {
            AlertDialog(
                onDismissRequest = { isBreakingPactPromptOpen = false },
                title = {
                    Text(
                        text = "🪓 SEVER THE CONTRACT?",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF3131),
                        letterSpacing = 1.sp
                    )
                },
                text = {
                    Text(
                        text = "Severing the pact will permanently retract your granted desire, and may invite the wrath of the spirits. Do you dare anger Girigo?",
                        fontFamily = FontFamily.SansSerif,
                        color = Color(0xFFFFBABA)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.breakPact()
                            isBreakingPactPromptOpen = false
                        },
                        modifier = Modifier.testTag("confirm_break_pact")
                    ) {
                        Text(
                            text = "YES, BREAK PACT",
                            color = Color(0xFFFF3131),
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { isBreakingPactPromptOpen = false }
                    ) {
                        Text(
                            text = "NO, STAY LOYAL",
                            color = Color(0xFFE0E0E0),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                containerColor = Color(0xFF1A0000),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun HistoryWishItem(wish: WishEntity) {
    val dateString = remember(wish.creationTime) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(wish.creationTime))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, Color(0xFF4D0000), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0000))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (wish.isCompleted) Icons.Default.CheckCircle else Icons.Default.Refresh,
                contentDescription = null,
                tint = if (wish.isCompleted) Color(0xFFFF3131) else Color(0xFFFFBABA),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "\"${wish.wishText}\"",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        color = Color(0xFFE0E0E0),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Sealed: $dateString",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 11.sp,
                        color = Color(0xFFFFBABA)
                    )
                )
            }
        }
    }
}

@Composable
fun ControlWarpButton(
    label: String,
    option: String,
    viewModel: WishViewModel
) {
    Button(
        onClick = { viewModel.simulateTimeRemaining(option) },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1B3B1B),
            contentColor = Color.Green
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier
            .height(32.dp)
            .testTag("warp_${option.lowercase()}")
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium
        )
    }
}

// Simple FlowRow helper for wrapping controls
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}

// Helper to format remaining milliseconds into hh:mm:ss
fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

// Data class for Blood Drips animation
data class BloodDrip(
    val x: Float,
    val y: Float,
    val length: Float,
    val speed: Float
)

// Helper Modifier Extension for scaling
fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(
        scaleX = scale,
        scaleY = scale
    )
)
