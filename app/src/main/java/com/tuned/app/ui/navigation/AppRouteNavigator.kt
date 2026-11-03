package com.tuned.app.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton navigation coordinator allowing external components (notifications,
 * system intents, shortcuts) to trigger navigation to app destinations.
 */
@Singleton
class AppRouteNavigator @Inject constructor() {
    private val _pendingRoute = MutableStateFlow<String?>(null)
    val pendingRoute: StateFlow<String?> = _pendingRoute.asStateFlow()

    fun navigateTo(route: String) {
        // External input (notifications, intents, link resolvers) must never be
        // able to inject an arbitrary route: only destinations declared in
        // [Screen] may be queued, anything else is dropped instead of
        // crashing NavController.navigate or opening an unintended page.
        if (route.isBlank() || route.length > MAX_ROUTE_LENGTH) return
        val base = route.substringBefore('/').substringBefore('?')
        if (base !in ALLOWED_BASE_ROUTES) return
        _pendingRoute.value = route
    }

    fun consumeRoute(): String? {
        val route = _pendingRoute.value
        _pendingRoute.value = null
        return route
    }

    private companion object {
        const val MAX_ROUTE_LENGTH = 512

        /** Base path segment of every declared destination, e.g. "album_detail". */
        val ALLOWED_BASE_ROUTES: Set<String> = Screen::class.sealedSubclasses
            .mapNotNull { it.objectInstance?.route }
            .map { it.substringBefore('/').substringBefore('?') }
            .toSet()
    }
}
