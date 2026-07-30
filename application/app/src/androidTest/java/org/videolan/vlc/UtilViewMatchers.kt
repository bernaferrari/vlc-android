package org.videolan.vlc

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.core.content.ContextCompat
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import com.google.android.material.tabs.TabLayout
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

fun withBgColor(@ColorInt color: Int): Matcher<View> {
    return object : BoundedMatcher<View, ViewGroup>(ViewGroup::class.java) {
        public override fun matchesSafely(vg: ViewGroup): Boolean {
            val colorDrawable = vg.background as ColorDrawable
            return color == colorDrawable.color
        }

        override fun describeTo(description: Description) {
            description.appendText("with background color: $color")
        }
    }
}

class TabsMatcher internal constructor(var position: Int) : ViewAction {

    override fun getConstraints(): Matcher<View> = isDisplayed()

    override fun getDescription(): String = "Click on tab"

    override fun perform(uiController: UiController?, view: View) {
        (view as? TabLayout)?.getTabAt(position)?.select()
    }
}

class FirstViewMatcher : BaseMatcher<View>() {
    var matchedBefore = false

    override fun matches(view: Any): Boolean = !matchedBefore.also { matchedBefore = true }

    override fun describeTo(description: Description) {
        description.appendText(" is the first view that comes along ")
    }
}

fun firstView() = FirstViewMatcher()

/*
    Taken from https://gist.github.com/frankiesardo/7490059
 */
fun withBackground(resourceId: Int): Matcher<View> {
    return object : TypeSafeMatcher<View>() {

        public override fun matchesSafely(view: View): Boolean {
            return sameBitmap(view.context, view.background, resourceId)
        }

        override fun describeTo(description: Description) {
            description.appendText("has background resource $resourceId")
        }
    }
}

fun withCompoundDrawable(resourceId: Int): Matcher<View> {
    return object : BoundedMatcher<View, TextView>(TextView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has compound drawable resource $resourceId")
        }

        public override fun matchesSafely(textView: TextView): Boolean {
            for (drawable in textView.compoundDrawables) {
                if (sameBitmap(textView.context, drawable, resourceId)) {
                    return true
                }
            }
            return false
        }
    }
}

fun withImageDrawable(resourceId: Int): Matcher<View> {
    return object : BoundedMatcher<View, ImageView>(ImageView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has image drawable resource $resourceId")
        }

        public override fun matchesSafely(imageView: ImageView): Boolean {
            return sameBitmap(imageView.context, imageView.drawable, resourceId)
        }
    }
}

private fun sameBitmap(context: Context, drawable: Drawable?, @DrawableRes resourceId: Int): Boolean {
    var drawable = drawable
    var otherDrawable = ContextCompat.getDrawable(context, resourceId)
    if (drawable == null || otherDrawable == null) {
        return false
    }
    if (drawable is StateListDrawable && otherDrawable is StateListDrawable) {
        drawable = drawable.current
        otherDrawable = otherDrawable.current
    }
    if (drawable is BitmapDrawable) {
        val bitmap = drawable.bitmap
        val otherBitmap = (otherDrawable as BitmapDrawable).bitmap
        return bitmap.sameAs(otherBitmap)
    }
    return false
}

fun withActionIconDrawable(@DrawableRes resourceId: Int): Matcher<View> {
    return object : BoundedMatcher<View, ActionMenuItemView>(ActionMenuItemView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has image drawable resource $resourceId")
        }

        public override fun matchesSafely(actionMenuItemView: ActionMenuItemView): Boolean {
            return sameBitmap(actionMenuItemView.context, actionMenuItemView.itemData.icon, resourceId)
        }
    }
}

fun withResName(resName: String): Matcher<View> {
    return object: TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("with res-name: $resName")
        }

        override fun matchesSafely(view: View): Boolean {
            val identifier = view.resources.getIdentifier(resName, "id", "android")
            return resName.isNotEmpty() && (view.id == identifier)
        }
    }
}
