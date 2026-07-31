package org.videolan.vlc.compose.app

/**
 * The Nav3 stack is the shell's only screen-navigation authority. A push cannot duplicate the
 * current route, and a pop can never remove the root. Dialogs and sheets dismiss themselves
 * before the shell sees Back, so every remaining Back action removes exactly one route.
 */
internal fun <T> pushNav3Route(backStack: MutableList<T>, route: T): Boolean {
    if (backStack.lastOrNull() == route) return false
    backStack.add(route)
    return true
}

internal fun <T> popNav3Route(backStack: MutableList<T>): Boolean {
    if (backStack.size <= 1) return false
    backStack.removeAt(backStack.lastIndex)
    return true
}

/** Selection is temporary screen state and must consume Back before navigation or app exit. */
internal fun shouldInterceptShellBack(
    appLocked: Boolean,
    hasActiveSelection: Boolean,
    canNavigateBack: Boolean,
): Boolean = !appLocked && (hasActiveSelection || canNavigateBack)
