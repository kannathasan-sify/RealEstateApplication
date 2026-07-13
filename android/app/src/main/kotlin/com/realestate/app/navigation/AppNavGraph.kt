package com.realestate.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.realestate.app.data.api.AuthEvent
import com.realestate.app.data.api.AuthEventBus
import com.realestate.app.ui.admin.AdminDashboardScreen
import com.realestate.app.ui.admin.AdminPropertyReviewScreen
import com.realestate.app.ui.auth.LoginScreen
import com.realestate.app.ui.auth.RegisterScreen
import com.realestate.app.ui.auth.RoleSelectionScreen
import com.realestate.app.ui.booking.BookingScreen
import com.realestate.app.ui.booking.MyBookingsScreen
import com.realestate.app.ui.myads.MyAdsScreen
import com.realestate.app.ui.mysearches.MySearchesScreen
import com.realestate.app.ui.settings.AccountSettingsScreen
import com.realestate.app.ui.settings.NotificationSettingsScreen
import com.realestate.app.ui.chat.ChatListScreen
import com.realestate.app.ui.components.BottomNavBar
import com.realestate.app.ui.home.HomeScreen
import com.realestate.app.ui.menu.MenuScreen
import com.realestate.app.ui.onboarding.OnboardingScreen
import com.realestate.app.ui.post_ad.PostAdCategoryScreen
import com.realestate.app.ui.post_ad.PostAdDetailsScreen
import com.realestate.app.ui.post_ad.PostAdMapPickerScreen
import com.realestate.app.ui.post_ad.PostAdRoleScreen
import com.realestate.app.ui.post_ad.PostAdSubCategoryScreen
import com.realestate.app.ui.post_ad.PostAdTitleScreen
import com.realestate.app.ui.post_ad.PostAdViewModel
import com.realestate.app.ui.post_ad.resolveRolePair
import com.realestate.app.ui.property.AmenitiesScreen
import com.realestate.app.ui.property.DistrictListScreen
import com.realestate.app.ui.property.PropertyDetailScreen
import com.realestate.app.ui.property.PropertyFilterScreen
import com.realestate.app.ui.property.PropertyListScreen
import com.realestate.app.ui.profile.ProfileScreen
import com.realestate.app.ui.saved.SavedScreen
import com.realestate.app.ui.splash.SplashScreen
import com.realestate.app.ui.service_request.PostServiceRequestScreen
import com.realestate.app.ui.service_request.ServiceRequestFeedScreen
import com.realestate.app.ui.service_request.ServiceRequestDetailScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ── Global 401 / session-expired handler ────────────────────────────────
    // Any API call that returns 401 (expired / invalid token) triggers this.
    // We clear the back-stack and send the user back to Login.
    LaunchedEffect(Unit) {
        AuthEventBus.events.collect { event ->
            if (event is AuthEvent.SessionExpired) {
                scope.launch {
                    snackbarHostState.showSnackbar("Session expired. Please log in again.")
                }
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    // Only Home and Menu are bottom-nav tabs now (Saved/Chat removed from nav bar)
    val bottomNavRoutes = setOf(
        Screen.Home.route,
        Screen.Menu.route,
    )
    val showBottomNav = currentRoute in bottomNavRoutes

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomNav) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onPostAdClick = { navController.navigate(Screen.PostAdGraph.route) }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(padding),
        ) {
            // ── Auth flow ────────────────────────────────────────────────────
            composable(Screen.Splash.route) {
                SplashScreen(onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(onDone = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = hiltViewModel(),
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateRegister = { navController.navigate(Screen.Register.route) },
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    viewModel = hiltViewModel(),
                    onSuccess = {
                        navController.navigate(Screen.RoleSelection.route) {
                            popUpTo(Screen.Login.route)
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.RoleSelection.route) {
                RoleSelectionScreen(
                    viewModel = hiltViewModel(),
                    onRoleSelected = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }

            // ── Main tabs ────────────────────────────────────────────────────
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = hiltViewModel(),
                    onPropertyClick = { id ->
                        navController.navigate(Screen.PropertyDetail.createRoute(id))
                    },
                    onCategoryClick = { listingType, district, workCategory ->
                        // Pass the selected district and optional work-category filter
                        navController.navigate(
                            Screen.PropertyList.createRoute(district, listingType, workCategory)
                        )
                    },
                    onFavouritesClick = { navController.navigate(Screen.Saved.route) },
                    onSearchClick = {
                        navController.navigate(
                            Screen.PropertyList.createRoute(
                                "All TN",
                                "all"
                            )
                        )
                    },
                    onPostAdClick = { navController.navigate(Screen.PostAdGraph.route) },
                )
            }
            composable(Screen.Saved.route) {
                SavedScreen(
                    viewModel = hiltViewModel(),
                    onPropertyClick = { navController.navigate(Screen.PropertyDetail.createRoute(it)) },
                )
            }
            composable(Screen.Chat.route) {
                ChatListScreen()
            }
            composable(Screen.Menu.route) {
                MenuScreen(
                    viewModel = hiltViewModel(),
                    onNavigateProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateAdminPanel = { navController.navigate(Screen.AdminDashboard.route) },
                    onNavigateMyAds = { navController.navigate(Screen.MyAds.route) },
                    onNavigateMySearches = { navController.navigate(Screen.MySearches.route) },
                    onNavigateAccountSettings = { navController.navigate(Screen.AccountSettings.route) },
                    onNavigatePostServiceRequest = { navController.navigate(Screen.PostServiceRequest.route) },
                    onNavigateServiceRequestFeed = { navController.navigate(Screen.ServiceRequestList.route) },
                    onNavigateSubscriptionPlans = { navController.navigate(Screen.SubscriptionPlans.route) },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }

            // ── District + Property browsing ─────────────────────────────────
            composable(
                route = Screen.DistrictList.route,
                arguments = listOf(navArgument("listingType") { defaultValue = "rent" })
            ) { entry ->
                val listingType = entry.arguments?.getString("listingType") ?: "rent"
                DistrictListScreen(
                    listingType = listingType,
                    onDistrictClick = { district ->
                        navController.navigate(
                            Screen.PropertyList.createRoute(
                                district,
                                listingType
                            )
                        )
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Screen.PropertyList.route,
                arguments = listOf(
                    navArgument("district") { defaultValue = "All TN" },
                    navArgument("listingType") { defaultValue = "rent" },
                    navArgument("workCategory") {
                        nullable = true
                        defaultValue = null
                    },
                )
            ) { entry ->
                PropertyListScreen(
                    viewModel = hiltViewModel(),
                    district = entry.arguments?.getString("district") ?: "All TN",
                    listingType = entry.arguments?.getString("listingType"),
                    workCategory = entry.arguments?.getString("workCategory"),
                    onPropertyClick = { id ->
                        navController.navigate(
                            Screen.PropertyDetail.createRoute(
                                id
                            )
                        )
                    },
                    onViewAllAmenities = { navController.navigate(Screen.Amenities.route) },
                    onPostAdClick = {
                        val lt = entry.arguments?.getString("listingType")
                        val wc = entry.arguments?.getString("workCategory")
                        navController.navigate(Screen.PostAdGraph.createRoute(lt, wc))
                    },
                    onBack = { navController.popBackStack() },
                )
                // Note: Filter is shown as an overlay inside PropertyListScreen.
                // The separate PropertyFilter route remains for backward compatibility.
            }
            composable(
                route = Screen.PropertyDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                PropertyDetailScreen(
                    viewModel = hiltViewModel(),
                    propertyId = entry.arguments?.getString("id") ?: "",
                    onBack = { navController.popBackStack() },
                    onBookVisit = { id -> navController.navigate(Screen.Booking.createRoute(id)) },
                    onPropertyClick = { id ->
                        navController.navigate(
                            Screen.PropertyDetail.createRoute(
                                id
                            )
                        )
                    },
                )
            }
            composable(Screen.PropertyFilter.route) {
                // Standalone route kept for direct deep-link use.
                // From PropertyListScreen the filter is shown as an overlay instead.
                PropertyFilterScreen(
                    onApply = { _ -> navController.popBackStack() },
                    onClose = { navController.popBackStack() },
                    onViewAllAmenities = { navController.navigate(Screen.Amenities.route) },
                )
            }
            composable(Screen.Amenities.route) {
                AmenitiesScreen(onBack = { navController.popBackStack() })
            }

            // ── Post Ad wizard (nested graph — ONE shared PostAdViewModel) ────
            // All five screens resolve hiltViewModel from the graph entry so they
            // share the same instance; title/category/subCategory etc. persist
            // across the entire wizard flow.
            navigation(
                route = Screen.PostAdGraph.route,
                startDestination = Screen.PostAdCategory.route,
                arguments = listOf(
                    navArgument("listingType") { nullable = true; defaultValue = null },
                    navArgument("workCategory") { nullable = true; defaultValue = null }
                )
            ) {
                composable(
                    route = Screen.PostAdCategory.route,
                    arguments = listOf(
                        navArgument("listingType") { nullable = true; defaultValue = null },
                        navArgument("workCategory") { nullable = true; defaultValue = null }
                    )
                ) { entry ->
                    val graphEntry = remember(entry) {
                        navController.getBackStackEntry(Screen.PostAdGraph.route)
                    }
                    val sharedVm: PostAdViewModel = hiltViewModel(graphEntry)
                    
                    val argListingType = entry.arguments?.getString("listingType")
                    val argWorkCategory = entry.arguments?.getString("workCategory")

                    // Refresh subscription limits when screen displays
                    LaunchedEffect(Unit) {
                        sharedVm.checkSubscriptionLimits()
                    }

                    // Pre-select category based on navigation arguments
                    LaunchedEffect(argListingType, argWorkCategory) {
                        if (!argListingType.isNullOrBlank() && sharedVm.selectedCategory.value.isBlank()) {
                            val mappedCat = when {
                                argListingType == "rent" -> "Property for Rent"
                                argListingType == "sale" -> "Property for Sale"
                                argListingType == "contractor" && argWorkCategory == "construction" -> "Construction Services"
                                argListingType == "contractor" && argWorkCategory == "maintenance" -> "Maintenance Services"
                                else -> null
                            }
                            if (mappedCat != null) {
                                sharedVm.selectedCategory.value = mappedCat
                                sharedVm.subCategory.value = ""
                                val pair = resolveRolePair(mappedCat)
                                sharedVm.postedBy.value = pair.leftKey
                            }
                        }
                    }

                    PostAdDetailsScreen(
                        viewModel = sharedVm,
                        onSubmit = {
                            // Pop the entire wizard graph (inclusive) and go Home
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.PostAdGraph.route) { inclusive = true }
                            }
                        },
                        onPickOnMap = {
                            navController.navigate(Screen.PostAdMapPicker.route)
                        },
                        onBack = { navController.popBackStack() },
                        onUpgradeClick = {
                            navController.navigate(Screen.SubscriptionPlans.route)
                        }
                    )
                }

                // ── Map picker — shares the same PostAdViewModel as the wizard ─
                composable(Screen.PostAdMapPicker.route) { entry ->
                    val graphEntry = remember(entry) {
                        navController.getBackStackEntry(Screen.PostAdGraph.route)
                    }
                    PostAdMapPickerScreen(
                        viewModel = hiltViewModel(graphEntry),
                        onLocationConfirmed = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                    )
                }
            }

                // ── Booking ──────────────────────────────────────────────────────
                composable(
                    route = Screen.Booking.route,
                    arguments = listOf(navArgument("propertyId") { type = NavType.StringType })
                ) { entry ->
                    BookingScreen(
                        viewModel = hiltViewModel(),
                        propertyId = entry.arguments?.getString("propertyId") ?: "",
                        onBack = { navController.popBackStack() },
                        onSuccess = { navController.popBackStack() },
                    )
                }

                // ── Admin screens — nested graph, shared AdminViewModel ───────────
                // Both Dashboard and Review resolve hiltViewModel() from the graph
                // entry so they share ONE AdminViewModel instance. This means:
                //  • loadAllProperties() is called only once (in Dashboard init)
                //  • Review immediately sees the already-loaded list — no flash of
                //    "Property not found" while a second API call completes.
                navigation(
                    route = Screen.AdminGraph.route,
                    startDestination = Screen.AdminDashboard.route,
                ) {
                    composable(Screen.AdminDashboard.route) { entry ->
                        val graphEntry = remember(entry) {
                            navController.getBackStackEntry(Screen.AdminGraph.route)
                        }
                        AdminDashboardScreen(
                            viewModel = hiltViewModel(graphEntry),
                            onPropertyClick = { id ->
                                navController.navigate(
                                    Screen.AdminPropertyReview.createRoute(
                                        id
                                    )
                                )
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(
                        route = Screen.AdminPropertyReview.route,
                        arguments = listOf(navArgument("id") { type = NavType.StringType }),
                    ) { entry ->
                        val graphEntry = remember(entry) {
                            navController.getBackStackEntry(Screen.AdminGraph.route)
                        }
                        AdminPropertyReviewScreen(
                            viewModel = hiltViewModel(graphEntry),
                            propertyId = entry.arguments?.getString("id") ?: "",
                            onBack = { navController.popBackStack() },
                        )
                    }
                }

                // ── My Bookings ──────────────────────────────────────────────────
                composable(Screen.MyBookings.route) {
                    MyBookingsScreen(
                        viewModel = hiltViewModel(),
                        onBack = { navController.popBackStack() },
                        onPropertyClick = { id ->
                            navController.navigate(
                                Screen.PropertyDetail.createRoute(
                                    id
                                )
                            )
                        },
                    )
                }

                // ── Profile ──────────────────────────────────────────────────────
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        viewModel = hiltViewModel(),
                        onBack = { navController.popBackStack() },
                    )
                }

                // ── My Ads ────────────────────────────────────────────────────────
                composable(Screen.MyAds.route) {
                    MyAdsScreen(
                        viewModel = hiltViewModel(),
                        onBack = { navController.popBackStack() },
                        onPropertyClick = { id ->
                            navController.navigate(
                                Screen.PropertyDetail.createRoute(
                                    id
                                )
                            )
                        },
                        onPostNew = { navController.navigate(Screen.PostAdGraph.route) },
                    )
                }

                // ── Approved Ads (My Ads starting on the APPROVED tab) ───────────
                composable(Screen.ApprovedAds.route) {
                    val vm = hiltViewModel<com.realestate.app.ui.myads.MyAdsViewModel>()
                    // Jump straight to the APPROVED tab when this route is opened
                    LaunchedEffect(Unit) {
                        vm.selectTab(com.realestate.app.ui.myads.MyAdsTab.APPROVED)
                    }
                    MyAdsScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onPropertyClick = { id ->
                            navController.navigate(
                                Screen.PropertyDetail.createRoute(
                                    id
                                )
                            )
                        },
                        onPostNew = { navController.navigate(Screen.PostAdGraph.route) },
                    )
                }

                // ── My Searches ───────────────────────────────────────────────────
                composable(Screen.MySearches.route) {
                    MySearchesScreen(
                        viewModel = hiltViewModel(),
                        onBack = { navController.popBackStack() },
                        onSearchClick = { listingType, district ->
                            val dist = district ?: "All TN"
                            navController.navigate(
                                Screen.PropertyList.createRoute(
                                    dist,
                                    listingType
                                )
                            )
                        },
                    )
                }

                // ── Account Settings ──────────────────────────────────────────────
                composable(Screen.AccountSettings.route) {
                    AccountSettingsScreen(
                        viewModel = hiltViewModel(),
                        onBack = { navController.popBackStack() },
                    )
                }

                // ── Notification Settings ─────────────────────────────────────────
                composable(Screen.NotificationSettings.route) {
                    NotificationSettingsScreen(
                        viewModel = hiltViewModel(),
                        onBack = { navController.popBackStack() },
                    )
                }

                // ── Service Requests ──────────────────────────────────────────────
                composable(Screen.PostServiceRequest.route) {
                    PostServiceRequestScreen(
                        viewModel = hiltViewModel(),
                        onSuccess = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.ServiceRequestList.route) {
                    ServiceRequestFeedScreen(
                        viewModel = hiltViewModel(),
                        onNavigateToDetail = { id ->
                            navController.navigate(Screen.ServiceRequestDetail.createRoute(id))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Screen.ServiceRequestDetail.route,
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { entry ->
                    ServiceRequestDetailScreen(
                        viewModel = hiltViewModel(),
                        requestId = entry.arguments?.getString("id") ?: "",
                        currentUserId = com.realestate.app.data.mock.MockData.currentUser.id,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.SubscriptionPlans.route) {
                    com.realestate.app.ui.subscription.SubscriptionPlansScreen(
                        viewModel = hiltViewModel(),
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

