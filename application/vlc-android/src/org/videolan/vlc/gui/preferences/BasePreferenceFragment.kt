/*
 * *************************************************************************
 *  BasePreferenceFragment.java
 * **************************************************************************
 *  Copyright © 2015 VLC authors and VideoLAN
 *  Author: Geoffrey Métais
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *  ***************************************************************************
 */

package org.videolan.vlc.gui.preferences

import android.os.Bundle
import android.view.View
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.preference.DialogPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import org.videolan.resources.util.parcelable
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.gui.preferences.search.PreferenceItem
import org.videolan.vlc.gui.view.NumberPickerPreference

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {

    protected abstract fun getXml(): Int
    protected abstract fun getTitleId(): Int

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        addPreferencesFromResource(getXml())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = Settings.getInstance(requireActivity())
        PreferenceVisibilityManager.manageVisibility(settings, preferenceScreen, false)
    }

    override fun onStart() {
        super.onStart()
        (activity as? PreferencesActivity)?.let {
            it.expandBar()
            if (it.supportActionBar != null && getTitleId() != 0)
                it.supportActionBar!!.title = getString(getTitleId())
        }
    }

    protected fun loadFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction().replace(R.id.fragment_placeholder, fragment)
                .addToBackStack("main")
                .commit()
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        when (preference) {
            is MultiSelectListPreference -> showMultiSelectListPreferenceDialog(preference)
            is NumberPickerPreference -> showNumberPickerPreferenceDialog(preference)
            else -> super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun showMultiSelectListPreferenceDialog(preference: MultiSelectListPreference) {
        val entries = preference.entries
        val entryValues = preference.entryValues
        if (entries == null || entryValues == null) {
            throw IllegalStateException("MultiSelectListPreference requires an entries array and an entryValues array.")
        }

        val originalValues = preference.values.toSet()
        val newValues = originalValues.toMutableSet()
        val checkedItems = BooleanArray(entryValues.size) { index ->
            originalValues.contains(entryValues[index].toString())
        }

        AlertDialog.Builder(requireContext())
            .applyPreferenceDialogMetadata(preference)
            .setMultiChoiceItems(entries, checkedItems) { _, which, isChecked ->
                val value = entryValues[which].toString()
                if (isChecked) newValues.add(value) else newValues.remove(value)
            }
            .setPositiveButton(preference.positiveButtonText ?: getText(android.R.string.ok)) { _, _ ->
                if (newValues != originalValues && preference.callChangeListener(newValues)) {
                    preference.values = newValues
                }
            }
            .setNegativeButton(preference.negativeButtonText ?: getText(android.R.string.cancel), null)
            .show()
    }

    private fun showNumberPickerPreferenceDialog(preference: NumberPickerPreference) {
        val container = layoutInflater.inflate(R.layout.pref_number_picker, null)
        val numberPicker = container.findViewById<NumberPicker>(R.id.number_picker).apply {
            minValue = NumberPickerPreference.MIN_VALUE
            maxValue = NumberPickerPreference.MAX_VALUE
            value = preference.getPersistedInt()
        }

        AlertDialog.Builder(requireContext())
            .applyPreferenceDialogMetadata(preference)
            .setView(container)
            .setPositiveButton(preference.positiveButtonText ?: getText(android.R.string.ok)) { _, _ ->
                numberPicker.clearFocus()
                val newValue = numberPicker.value
                if (preference.callChangeListener(newValue)) {
                    preference.doPersistInt(newValue)
                }
            }
            .setNegativeButton(preference.negativeButtonText ?: getText(android.R.string.cancel), null)
            .show()
    }

    private fun AlertDialog.Builder.applyPreferenceDialogMetadata(preference: DialogPreference): AlertDialog.Builder {
        setTitle(preference.dialogTitle ?: preference.title)
        preference.dialogIcon?.let { setIcon(it) }
        return this
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.parcelable<PreferenceItem>(EXTRA_PREF_END_POINT)?.let { endPoint ->
            selectPreference(endPoint.key)
        }
    }

    private fun selectPreference(key: String) {
        scrollToPreference(key)
        findPreference<Preference>(key)?.isSelectable = true
        listView?.postDelayed({
            (listView?.adapter as? PreferenceGroup.PreferencePositionCallback)?.let { adapter ->
                listView?.findViewHolderForAdapterPosition(adapter.getPreferenceAdapterPosition(key))?.itemView?.let { itemView ->
                    listView?.postDelayed({
                        itemView.isPressed = true
                    }, 600)
                }
            }
        }, 200)
    }
}
