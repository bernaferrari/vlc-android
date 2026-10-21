package org.videolan.vlc.compose.app

import platform.UIKit.UITabBarController
import platform.UIKit.UIView

/**
 * Installs or removes the tab bar's bottom accessory. `UITabAccessory` only exists on iOS 26+, and
 * Kotlin/Native would link that class strongly (the app would not even launch on iOS 18), so the
 * Swift host implements this with `#available` and Swift's automatic weak linking.
 */
interface IosTabAccessoryHandler {
    /** Returns false when the running iOS has no tab accessory; [content] null removes it. */
    fun setAccessory(tabs: UITabBarController, content: UIView?, animated: Boolean): Boolean
}

object IosTabAccessoryBridge {
    var handler: IosTabAccessoryHandler? = null
        private set

    fun setHandler(handler: IosTabAccessoryHandler?) {
        this.handler = handler
    }
}
