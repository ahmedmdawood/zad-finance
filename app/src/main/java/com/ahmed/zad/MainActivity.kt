package com.ahmed.zad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BalanceWidgetProvider.refresh(this)
        setContent {
            val vm: MainViewModel = viewModel()
            val themeId by vm.themeId.collectAsState()
            ZadAppTheme(themeId) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val name by vm.userName.collectAsState()
                    if (name.isEmpty()) {
                        OnboardingScreen(vm)
                    } else {
                        MainScaffold(vm)
                    }
                    val needsPassword by vm.needsPasswordPrompt.collectAsState()
                    if (needsPassword) PasswordPromptDialog(vm)
                }
            }
        }
    }
}

@Composable
fun MainScaffold(vm: MainViewModel) {
    val tab by vm.tab.collectAsState()
    val overlay by vm.overlay.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == ZadTab.HOME,
                    onClick = { vm.setTab(ZadTab.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("الرئيسية", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                )
                NavigationBarItem(
                    selected = tab == ZadTab.ACCOUNTS,
                    onClick = { vm.setTab(ZadTab.ACCOUNTS) },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    label = { Text("الحسابات", fontSize = 11.sp) },
                )
                NavigationBarItem(
                    selected = tab == ZadTab.DEBTS,
                    onClick = { vm.setTab(ZadTab.DEBTS) },
                    icon = { Icon(Icons.Default.Handshake, contentDescription = null) },
                    label = { Text("الديون", fontSize = 11.sp) },
                )
                NavigationBarItem(
                    selected = tab == ZadTab.OBLIGATIONS,
                    onClick = { vm.setTab(ZadTab.OBLIGATIONS) },
                    icon = { Icon(Icons.Default.EventRepeat, contentDescription = null) },
                    label = { Text("الالتزامات", fontSize = 11.sp) },
                )
                NavigationBarItem(
                    selected = tab == ZadTab.PLANS,
                    onClick = { vm.setTab(ZadTab.PLANS) },
                    icon = { Icon(Icons.Default.Savings, contentDescription = null) },
                    label = { Text("الخطط", fontSize = 11.sp) },
                )
                NavigationBarItem(
                    selected = tab == ZadTab.SETTINGS,
                    onClick = { vm.setTab(ZadTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("الإعدادات", fontSize = 11.sp) },
                )
            }
        }
    ) { padding ->
        val modifier = Modifier.padding(padding)
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
            Overlay.None -> when (tab) {
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
