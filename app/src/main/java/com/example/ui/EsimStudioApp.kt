package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.EsimRepository
import com.example.model.EsimStatus
import com.example.ui.components.EsimDetailSheet
import com.example.ui.screens.CreateEsimScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.viewmodel.EsimViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

const val ROUTE_WALLET = "wallet"
const val ROUTE_CREATE = "create"
const val ROUTE_DEVICE = "device"

private enum class TopTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    WALLET(ROUTE_WALLET, R.string.nav_wallet, Icons.Rounded.SimCard, Icons.Outlined.SimCard),
    CREATE(ROUTE_CREATE, R.string.nav_create, Icons.Rounded.AddCircle, Icons.Outlined.AddCircleOutline),
    DEVICE(ROUTE_DEVICE, R.string.nav_device, Icons.Rounded.Memory, Icons.Outlined.Memory)
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener updates currentUser reactively */ })
    } else {
        key(user.uid) {
            AuthenticatedEsimShell(user = user)
        }
    }
}

@Composable
private fun AuthenticatedEsimShell(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: EsimViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                EsimViewModel(EsimRepository(db), user.uid)
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_WALLET

    val esimsState by viewModel.esimsState.collectAsStateWithLifecycle()
    val filteredEsims by viewModel.filteredEsims.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedEsim by viewModel.selectedEsim.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val createForm by viewModel.createForm.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        TopTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(tab.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                    ) {
                        TopTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(tab.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_WALLET,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp)
                    ) {
                        composable(ROUTE_WALLET) {
                            WalletScreen(
                                esimsState = esimsState,
                                filteredEsims = filteredEsims,
                                searchQuery = searchQuery,
                                selectedFilter = selectedFilter,
                                isBusy = isBusy,
                                onSearchQueryChange = viewModel::setSearchQuery,
                                onFilterSelected = viewModel::setFilter,
                                onOpenDetail = viewModel::openDetail,
                                onSimulateActiveUsage = { esim -> viewModel.simulateDataUsage(esim) },
                                onLoadStarters = viewModel::loadStarterProfiles,
                                onNavigateToCreate = { navigateToTab(ROUTE_CREATE) }
                            )
                        }

                        composable(ROUTE_CREATE) {
                            BackHandler { navigateToTab(ROUTE_WALLET) }
                            CreateEsimScreen(
                                form = createForm,
                                onApplyPreset = viewModel::applyPreset,
                                onUpdateForm = viewModel::updateCreateForm,
                                onRegenerateIccid = viewModel::regenerateIccid,
                                onRegenerateCode = viewModel::regenerateActivationCode,
                                onParseLpaImport = viewModel::parseLpaImport,
                                onCreateSubmit = {
                                    viewModel.createVirtualEsim {
                                        navigateToTab(ROUTE_WALLET)
                                    }
                                }
                            )
                        }

                        composable(ROUTE_DEVICE) {
                            BackHandler { navigateToTab(ROUTE_WALLET) }
                            val list = (esimsState as? UiState.Success)?.data ?: emptyList()
                            DiagnosticsScreen(
                                userDisplayName = user.displayName,
                                userEmail = user.email,
                                totalProfiles = list.size,
                                activeProfiles = list.count { it.status == EsimStatus.ACTIVE.name },
                                onSignOut = { signOut(credentialManager, scope) },
                                onNotify = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedEsim?.let { esim ->
        EsimDetailSheet(
            esim = esim,
            onDismiss = viewModel::closeDetail,
            onActivateSlot = { viewModel.activateSlot(esim.id) },
            onSetStandby = { viewModel.setStatus(esim.id, EsimStatus.STANDBY) },
            onSimulateUsage = { viewModel.simulateDataUsage(esim) },
            onTopUp = { viewModel.topUpData(esim) },
            onToggleRoaming = { viewModel.toggleRoaming(esim) },
            onToggleFiveG = { viewModel.toggleFiveG(esim) },
            onSaveEdit = { label, carrier, apn, notes, accent ->
                viewModel.updateProfileDetails(esim, label, carrier, apn, notes, accent)
            },
            onDelete = { viewModel.deleteEsim(esim.id) },
            onNotify = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
        )
    }
}
