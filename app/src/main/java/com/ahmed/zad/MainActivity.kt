package com.ahmed.zad

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val defaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                java.io.File(filesDir, "last_crash.txt").writeText(android.util.Log.getStackTraceString(error))
            } catch (_: Exception) {
            }
            defaultCrashHandler?.uncaughtException(thread, error)
        }
        enableEdgeToEdge()
        ZadNotifications.createChannels(this)
        BalanceWidgetProvider.refresh(this)
        setContent {
            val vm: MainViewModel = viewModel()
            val themeId by vm.themeId.collectAsState()
            val darkOverride by vm.darkOverride.collectAsState()

            // Theme transition: a soft veil in the new theme's colors fades in,
            // the theme swaps beneath it, and the veil fades out. The app is
            // composed once at all times, so it stays smooth on weak hardware.
            var baseTheme by remember { mutableStateOf(themeId) }
            var baseDark by remember { mutableIntStateOf(darkOverride) }
            val veil = remember { Animatable(0f) }
            val currentName by vm.userName.collectAsState()

            LaunchedEffect(themeId, darkOverride) {
                if (themeId == baseTheme && darkOverride == baseDark) return@LaunchedEffect
                val slow = currentName.isEmpty()
                veil.animateTo(1f, tween(if (slow) 750 else 550, easing = FastOutSlowInEasing))
                baseTheme = themeId
                baseDark = darkOverride
                veil.animateTo(0f, tween(if (slow) 950 else 700, easing = FastOutSlowInEasing))
            }

            Box(Modifier.fillMaxSize()) {
                ZadAppTheme(baseTheme, baseDark) {
                    ZadAppContent(vm)
                }
                if (veil.value > 0f) {
                    val veilScheme = themeScheme(themeId, darkOverride)
                    Box(
                        Modifier.fillMaxSize()
                            .graphicsLayer { alpha = veil.value }
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(veilScheme.primary, veilScheme.secondary)
                                )
                            )
                    )
                }
            }
        }
        handleLaunchIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(ZadNotifications.EXTRA_OPEN_ADD_EXPENSE, false) == true) {
            quickAddRequested.value = true
        }
    }

    companion object {
        val quickAddRequested: androidx.compose.runtime.MutableState<Boolean> =
            androidx.compose.runtime.mutableStateOf(false)
    }
}

/** Screen position of the light/dark switch, set from Settings so the reveal starts there. */
object ThemeReveal {
    var origin: Offset = Offset.Unspecified
}

@Composable
fun ZadAppContent(vm: MainViewModel) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        val name by vm.userName.collectAsState()
        val unlocked by vm.appUnlocked.collectAsState()
        val lockEnabled by vm.appLockEnabled.collectAsState()
        val activity = androidx.compose.ui.platform.LocalContext.current as? MainActivity
        when {
            name.isEmpty() -> OnboardingScreen(vm)
            lockEnabled && !unlocked -> LockedScreen(onUnlock = {
                activity?.let {
                    AppLock.authenticate(
                        it,
                        onSuccess = { vm.markAppUnlocked() },
                        onFailed = { },
                    )
                }
            })
            else -> MainScaffold(vm)
        }
        val needsPassword by vm.needsPasswordPrompt.collectAsState()
        if (needsPassword) PasswordPromptDialog(vm)
    }
}

@Composable
fun MainScaffold(vm: MainViewModel) {
    val tab by vm.tab.collectAsState()
    val overlay by vm.overlay.collectAsState()

    androidx.activity.compose.BackHandler(
        enabled = overlay != Overlay.None || tab != ZadTab.HOME,
    ) {
        if (overlay != Overlay.None) vm.closeOverlay() else vm.setTab(ZadTab.HOME)
    }

    if (MainActivity.quickAddRequested.value) {
        MainActivity.quickAddRequested.value = false
        vm.openOverlay(Overlay.AddTransaction("EXPENSE"))
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == ZadTab.HOME,
                    onClick = { vm.closeOverlay(); vm.setTab(ZadTab.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("الرئيسية", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                )
                NavigationBarItem(
                    selected = tab == ZadTab.ACCOUNTS,
                    onClick = { vm.closeOverlay(); vm.setTab(ZadTab.ACCOUNTS) },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    label = { Text("الحسابات", fontSize = 11.sp) },
                )
                NavigationBarItem(
                    selected = tab == ZadTab.DEBTS,
                    onClick = { vm.closeOverlay(); vm.setTab(ZadTab.DEBTS) },
                    icon = { Icon(Icons.Default.Handshake, contentDescription = null) },
                    label = { Text("الديون", fontSize = 11.sp) },
                )
                NavigationBarItem(
                    selected = tab == ZadTab.OBLIGATIONS,
                    onClick = { vm.closeOverlay(); vm.setTab(ZadTab.OBLIGATIONS) },
                    icon = { Icon(Icons.Default.EventRepeat, contentDescription = null) },
                    label = { Text("الالتزامات", fontSize = 11.sp) },
                )
                NavigationBarItem(
                    selected = tab == ZadTab.PLANS,
                    onClick = { vm.closeOverlay(); vm.setTab(ZadTab.PLANS) },
                    icon = { Icon(Icons.Default.Savings, contentDescription = null) },
                    label = { Text("الخطط", fontSize = 11.sp) },
                )
                NavigationBarItem(
                    selected = tab == ZadTab.SETTINGS,
                    onClick = { vm.closeOverlay(); vm.setTab(ZadTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("الإعدادات", fontSize = 11.sp) },
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
        when (val current = overlay) {
            is Overlay.AddTransaction -> AddTransactionScreen(vm, current.type)
            is Overlay.CardPurchase -> CardPurchaseScreen(vm, current.accountId)
            is Overlay.CardPayment -> CardPaymentScreen(vm, current.accountId)
            Overlay.AddAccount -> AddAccountScreen(vm)
            Overlay.AddDebt -> AddDebtScreen(vm)
            is Overlay.PayDebt -> PayDebtScreen(vm, current.debtId)
            Overlay.AddObligation -> AddObligationScreen(vm)
            is Overlay.ObligationDetail -> ObligationsTab(vm)
            is Overlay.PayOccurrence -> PayOccurrenceScreen(vm, current.occurrenceId)
            Overlay.AddPlan -> AddPlanScreen(vm)
            is Overlay.PlanDetail -> PlanDetailScreen(vm, current.planId)
            Overlay.About -> AboutScreen(vm)
            Overlay.Charts -> ChartsScreen(vm)
            Overlay.Calendar -> CalendarScreen(vm)
            Overlay.Trash -> TrashScreen(vm)
            Overlay.Notes -> NotesScreen(vm)
            Overlay.None -> AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
                label = "tabContent",
            ) { t ->
                when (t) {
                    ZadTab.HOME -> HomeTab(vm)
                    ZadTab.ACCOUNTS -> AccountsTab(vm)
                    ZadTab.DEBTS -> DebtsTab(vm)
                    ZadTab.OBLIGATIONS -> ObligationsTab(vm)
                    ZadTab.PLANS -> PlansTab(vm)
                    ZadTab.SETTINGS -> SettingsTab(vm)
                }
            }
        }
        }
    }
}
