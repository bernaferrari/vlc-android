package org.videolan.vlc.gui.dialogs

import android.content.SharedPreferences
import android.net.Uri
import android.text.Spanned
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.databinding.Observable
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import main.java.org.videolan.resources.opensubtitles.OpenSubtitlesLimit
import main.java.org.videolan.resources.opensubtitles.OpenSubtitlesUser
import main.java.org.videolan.resources.opensubtitles.OpenSubtitlesUtils
import org.videolan.resources.AndroidDevices
import org.videolan.resources.opensubtitles.OpenSubtitleClient
import org.videolan.resources.opensubtitles.OpenSubtitleRepository
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.helpers.UiTools.deleteSubtitleDialog
import org.videolan.vlc.util.VLCDownloadManager
import org.videolan.vlc.util.isTalkbackIsEnabled
import org.videolan.vlc.util.openLinkIfPossible
import org.videolan.vlc.viewmodels.SubtitlesModel
import java.util.Locale

fun ComponentActivity.showSubtitleDownloaderComposeDialog(mediaUri: Uri, mediaTitle: String) {
    SubtitleDownloaderComposeDialog(
        activity = this,
        mediaUri = mediaUri,
        mediaTitle = mediaTitle
    ).show()
}

private class SubtitleDownloaderComposeDialog(
    private val activity: ComponentActivity,
    private val mediaUri: Uri,
    private val mediaTitle: String
) {
    private val settings: SharedPreferences = Settings.getInstance(activity)
    private val viewModel = ViewModelProvider(
        activity,
        SubtitlesModel.Factory(activity, mediaUri, mediaTitle)
    )[mediaUri.path!!, SubtitlesModel::class.java]
    private val dialog = if (Settings.showTvUi) {
        ComposeMaterialBottomSheetHost(activity)
    } else {
        ComposeMaterialBottomSheetHost(activity)
    }
    private val modeState = mutableStateOf(SubDownloadDialogState.Download)
    private val resultsState = mutableStateOf<List<SubtitleItem>>(emptyList())
    private val historyState = mutableStateOf<List<SubtitleItem>>(emptyList())
    private val isLoadingState = mutableStateOf(false)
    private val messageState = mutableStateOf("")
    private val errorState = mutableStateOf(false)
    private val resultDescriptionState = mutableStateOf("")
    private val resultDescriptionTalkbackState = mutableStateOf("")
    private val inEditModeState = mutableStateOf(false)
    private val searchNameState = mutableStateOf("")
    private val searchSeasonState = mutableStateOf("")
    private val searchEpisodeState = mutableStateOf("")
    private val hearingImpairedState = mutableStateOf(false)
    private val selectedLanguagesState = mutableStateOf<List<String>>(emptyList())
    private val userState = mutableStateOf(OpenSubtitlesUser())
    private val limitState = mutableStateOf(OpenSubtitlesLimit())
    private val usernameState = mutableStateOf("")
    private val passwordState = mutableStateOf("")
    private val observableCallbacks = mutableListOf<Pair<Observable, Observable.OnPropertyChangedCallback>>()
    private val resultObserver = Observer<List<SubtitleItem>> { resultsState.value = it.orEmpty() }
    private val historyObserver = Observer<List<SubtitleItem>> { historyState.value = it.orEmpty() }
    private val loadingObserver = Observer<Boolean> { isLoadingState.value = it == true }
    private var rootView: ComposeView? = null
    private var toast: Toast? = null

    fun show() {
        initializeModel()
        registerObservers()
        setupContent()
        dialog.show()
        configureBottomSheet()
        if (viewModel.isApiLoading.value == false) viewModel.onRefresh()
    }

    private fun initializeModel() {
        val user = OpenSubtitlesUtils.getUser(settings)
        val token = user.account?.token
        if (!token.isNullOrEmpty()) OpenSubtitleClient.authorizationToken = token
        OpenSubtitleClient.userDomain = user.account?.baseUrl
        viewModel.observableInEditMode.set(false)
        viewModel.observableSearchHearingImpaired.set(false)
        viewModel.observableUser.set(user)
        viewModel.observableLimit.set(OpenSubtitlesUtils.getLimit(settings))
        val languages = viewModel.getLastUsedLanguage()
        selectedLanguagesState.value = languages
        viewModel.observableSearchLanguage.set(languages)
        if (!token.isNullOrEmpty()) viewModel.checkUserInfos(settings)
    }

    private fun registerObservers() {
        viewModel.result.observe(activity, resultObserver)
        viewModel.history.observe(activity, historyObserver)
        viewModel.isApiLoading.observe(activity, loadingObserver)
        observe(viewModel.observableMessage) { messageState.value = viewModel.observableMessage.get().orEmpty() }
        observe(viewModel.observableError) { errorState.value = viewModel.observableError.get() == true }
        observe(viewModel.observableResultDescription) { resultDescriptionState.value = viewModel.observableResultDescription.get().plainText() }
        observe(viewModel.observableResultDescriptionTalkback) { resultDescriptionTalkbackState.value = viewModel.observableResultDescriptionTalkback.get().orEmpty() }
        observe(viewModel.observableInEditMode) { inEditModeState.value = viewModel.observableInEditMode.get() == true }
        observe(viewModel.observableSearchName) { searchNameState.value = viewModel.observableSearchName.get().orEmpty() }
        observe(viewModel.observableSearchSeason) { searchSeasonState.value = viewModel.observableSearchSeason.get().orEmpty() }
        observe(viewModel.observableSearchEpisode) { searchEpisodeState.value = viewModel.observableSearchEpisode.get().orEmpty() }
        observe(viewModel.observableSearchHearingImpaired) { hearingImpairedState.value = viewModel.observableSearchHearingImpaired.get() == true }
        observe(viewModel.observableSearchLanguage) { selectedLanguagesState.value = viewModel.observableSearchLanguage.get().orEmpty() }
        observe(viewModel.observableUser) { userState.value = viewModel.observableUser.get() ?: OpenSubtitlesUser() }
        observe(viewModel.observableLimit) { limitState.value = viewModel.observableLimit.get() ?: OpenSubtitlesLimit() }
    }

    private fun observe(observable: Observable, update: () -> Unit) {
        val callback = object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) = update()
        }
        observable.addOnPropertyChangedCallback(callback)
        observableCallbacks += observable to callback
        update()
    }

    private fun setupContent() {
        rootView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme {
                    SubtitleDownloaderContent(
                        mode = modeState.value,
                        results = resultsState.value,
                        history = historyState.value,
                        isLoading = isLoadingState.value,
                        message = messageState.value,
                        inError = errorState.value,
                        resultDescription = resultDescriptionState.value,
                        resultDescriptionTalkback = resultDescriptionTalkbackState.value,
                        inEditMode = inEditModeState.value,
                        searchName = searchNameState.value,
                        searchSeason = searchSeasonState.value,
                        searchEpisode = searchEpisodeState.value,
                        hearingImpaired = hearingImpairedState.value,
                        languageEntries = activity.resources.getStringArray(R.array.language_entries).toList(),
                        languageValues = activity.resources.getStringArray(R.array.language_values).toList(),
                        selectedLanguages = selectedLanguagesState.value,
                        user = userState.value,
                        limit = limitState.value,
                        username = usernameState.value,
                        password = passwordState.value,
                        onModeSelected = ::setMode,
                        onEditToggle = ::toggleEditMode,
                        onSearchNameChanged = ::setSearchName,
                        onSearchSeasonChanged = ::setSearchSeason,
                        onSearchEpisodeChanged = ::setSearchEpisode,
                        onHearingImpairedChanged = ::setHearingImpaired,
                        onLanguagesChanged = ::setSelectedLanguages,
                        onSearch = ::searchByName,
                        onReset = ::resetSearch,
                        onCancelEdit = { setEditMode(false) },
                        onRetry = viewModel::onRefresh,
                        onSubtitleClicked = ::onSubtitleClicked,
                        onSubtitleLongClicked = ::onSubtitleLongClicked,
                        onUsernameChanged = { usernameState.value = it },
                        onPasswordChanged = { passwordState.value = it },
                        onLoginClicked = ::loginOrLogout,
                        onRegisterClicked = {
                            activity.openLinkIfPossible("https://www.opensubtitles.com/en/users/sign_up", 512)
                        }
                    )
                }
            }
        }
        dialog.setContentView(rootView!!)
        dialog.setOnDismissListener { cleanup() }
    }

    private fun setMode(mode: SubDownloadDialogState) {
        modeState.value = if (modeState.value == mode && mode != SubDownloadDialogState.Download) {
            SubDownloadDialogState.Download
        } else {
            mode
        }
    }

    private fun setSearchName(value: String) {
        searchNameState.value = value
        viewModel.observableSearchName.set(value)
    }

    private fun setSearchSeason(value: String) {
        searchSeasonState.value = value
        viewModel.observableSearchSeason.set(value)
    }

    private fun setSearchEpisode(value: String) {
        searchEpisodeState.value = value
        viewModel.observableSearchEpisode.set(value)
    }

    private fun setHearingImpaired(value: Boolean) {
        hearingImpairedState.value = value
        viewModel.observableSearchHearingImpaired.set(value)
    }

    private fun setSelectedLanguages(values: List<String>) {
        selectedLanguagesState.value = values
        viewModel.observableSearchLanguage.set(values)
    }

    private fun toggleEditMode() {
        if (!inEditModeState.value) {
            val name = viewModel.observableSearchName.get().takeUnless { it.isNullOrBlank() } ?: mediaTitle
            setSearchName(name)
            setSearchSeason(viewModel.observableSearchSeason.get().orEmpty())
            setSearchEpisode(viewModel.observableSearchEpisode.get().orEmpty())
            setHearingImpaired(viewModel.observableSearchHearingImpaired.get() == true)
        }
        setEditMode(!inEditModeState.value)
    }

    private fun setEditMode(editing: Boolean) {
        viewModel.observableInEditMode.set(editing)
        inEditModeState.value = editing
    }

    private fun searchByName() {
        viewModel.search(false)
        setMode(SubDownloadDialogState.Download)
        setEditMode(false)
    }

    private fun resetSearch() {
        setEditMode(false)
        viewModel.search(true)
    }

    private fun loginOrLogout() {
        if (userState.value.logged) {
            viewModel.logout(settings)
            viewModel.clearCredentials()
            usernameState.value = ""
            passwordState.value = ""
        } else {
            viewModel.login(settings, usernameState.value, passwordState.value)
        }
    }

    private fun onSubtitleClicked(item: SubtitleItem) {
        when (item.state) {
            State.NotDownloaded -> downloadSubtitle(item)
            State.Downloaded -> deleteSubtitleDialog(activity) {
                item.mediaUri.path?.let { viewModel.deleteSubtitle(it, item.idSubtitle) }
            }
            State.Downloading -> Unit
        }
    }

    private fun downloadSubtitle(item: SubtitleItem) {
        activity.lifecycleScope.launch {
            var downloadAvailable = true
            withContext(Dispatchers.IO) {
                Log.i("SubtitleDownload", "Launching download for ${item.idSubtitle}")
                val downloadLink = OpenSubtitleRepository.getInstance().getDownloadLink(item.fileId)
                val openSubtitlesLimit = OpenSubtitlesLimit(
                    downloadLink.requests,
                    downloadLink.requests + downloadLink.remaining,
                    downloadLink.resetTimeUtc
                )
                Log.i("SubtitleDownload", "Subtitle download retrieved: ${downloadLink.link} - ${downloadLink.fileName}")
                try {
                    OpenSubtitlesUtils.saveLimit(settings, openSubtitlesLimit)
                    viewModel.observableLimit.set(openSubtitlesLimit)
                    item.zipDownloadLink = downloadLink.link
                    item.fileName = downloadLink.fileName
                    item.downloadError = false
                } catch (e: Exception) {
                    Log.w("SubtitleDownload", e.message, e)
                    downloadAvailable = false
                    item.downloadError = true
                    withContext(Dispatchers.Main) { refreshSubtitleRows() }
                }
            }
            if (downloadAvailable) VLCDownloadManager.download(activity, item, true)
        }
    }

    private fun refreshSubtitleRows() {
        resultsState.value = resultsState.value.toList()
        historyState.value = historyState.value.toList()
    }

    private fun onSubtitleLongClicked(item: SubtitleItem) {
        @StringRes val message = when (item.state) {
            State.NotDownloaded -> R.string.download_the_selected
            State.Downloaded -> R.string.delete_the_selected
            State.Downloading -> return
        }
        toast?.cancel()
        toast = Toast.makeText(activity, message, Toast.LENGTH_SHORT).apply {
            setGravity(Gravity.TOP, 0, 100)
            show()
        }
    }

    private fun configureBottomSheet() {
        dialog.window?.setLayout(
            activity.resources.getDimensionPixelSize(R.dimen.default_context_width),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.findViewById<View>(R.id.touch_outside)?.isFocusable = false
        dialog.findViewById<View>(R.id.touch_outside)?.isFocusableInTouchMode = false
        rootView?.let { view ->
            if (AndroidDevices.isTv) {
                val overscan = activity.resources.getDimensionPixelSize(org.videolan.resources.R.dimen.tv_overscan_vertical)
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom + overscan)
            }
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()
            if (activity.isTalkbackIsEnabled()) view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }
    }

    private fun cleanup() {
        viewModel.result.removeObserver(resultObserver)
        viewModel.history.removeObserver(historyObserver)
        viewModel.isApiLoading.removeObserver(loadingObserver)
        observableCallbacks.forEach { (observable, callback) ->
            observable.removeOnPropertyChangedCallback(callback)
        }
        observableCallbacks.clear()
        toast?.cancel()
        toast = null
        rootView = null
    }
}

@Composable
private fun SubtitleDownloaderContent(
    mode: SubDownloadDialogState,
    results: List<SubtitleItem>,
    history: List<SubtitleItem>,
    isLoading: Boolean,
    message: String,
    inError: Boolean,
    resultDescription: String,
    resultDescriptionTalkback: String,
    inEditMode: Boolean,
    searchName: String,
    searchSeason: String,
    searchEpisode: String,
    hearingImpaired: Boolean,
    languageEntries: List<String>,
    languageValues: List<String>,
    selectedLanguages: List<String>,
    user: OpenSubtitlesUser,
    limit: OpenSubtitlesLimit,
    username: String,
    password: String,
    onModeSelected: (SubDownloadDialogState) -> Unit,
    onEditToggle: () -> Unit,
    onSearchNameChanged: (String) -> Unit,
    onSearchSeasonChanged: (String) -> Unit,
    onSearchEpisodeChanged: (String) -> Unit,
    onHearingImpairedChanged: (Boolean) -> Unit,
    onLanguagesChanged: (List<String>) -> Unit,
    onSearch: () -> Unit,
    onReset: () -> Unit,
    onCancelEdit: () -> Unit,
    onRetry: () -> Unit,
    onSubtitleClicked: (SubtitleItem) -> Unit,
    onSubtitleLongClicked: (SubtitleItem) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClicked: () -> Unit,
    onRegisterClicked: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Surface(
        color = colors.backgroundDefault,
        contentColor = colors.fontDefault,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(min = 300.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            SubtitleHeader(
                mode = mode,
                onModeSelected = onModeSelected
            )
            Text(
                text = stringResource(R.string.powered_opensubtitles),
                color = colors.fontLight,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp)
            )
            when (mode) {
                SubDownloadDialogState.Download -> DownloadModeContent(
                    results = results,
                    isLoading = isLoading,
                    message = message,
                    inError = inError,
                    resultDescription = resultDescription,
                    resultDescriptionTalkback = resultDescriptionTalkback,
                    inEditMode = inEditMode,
                    searchName = searchName,
                    searchSeason = searchSeason,
                    searchEpisode = searchEpisode,
                    hearingImpaired = hearingImpaired,
                    languageEntries = languageEntries,
                    languageValues = languageValues,
                    selectedLanguages = selectedLanguages,
                    onEditToggle = onEditToggle,
                    onSearchNameChanged = onSearchNameChanged,
                    onSearchSeasonChanged = onSearchSeasonChanged,
                    onSearchEpisodeChanged = onSearchEpisodeChanged,
                    onHearingImpairedChanged = onHearingImpairedChanged,
                    onLanguagesChanged = onLanguagesChanged,
                    onSearch = onSearch,
                    onReset = onReset,
                    onCancelEdit = onCancelEdit,
                    onRetry = onRetry,
                    onSubtitleClicked = onSubtitleClicked,
                    onSubtitleLongClicked = onSubtitleLongClicked
                )
                SubDownloadDialogState.History -> HistoryModeContent(
                    history = history,
                    onSubtitleClicked = onSubtitleClicked,
                    onSubtitleLongClicked = onSubtitleLongClicked
                )
                SubDownloadDialogState.Login -> LoginModeContent(
                    user = user,
                    limit = limit,
                    username = username,
                    password = password,
                    onUsernameChanged = onUsernameChanged,
                    onPasswordChanged = onPasswordChanged,
                    onLoginClicked = onLoginClicked,
                    onRegisterClicked = onRegisterClicked
                )
            }
        }
    }
}

@Composable
private fun SubtitleHeader(
    mode: SubDownloadDialogState,
    onModeSelected: (SubDownloadDialogState) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.download_subtitles),
            color = colors.fontDefault,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { onModeSelected(SubDownloadDialogState.History) }) {
            Icon(
                painter = painterResource(R.drawable.ic_history),
                contentDescription = stringResource(R.string.talkback_subtitle_history),
                tint = if (mode == SubDownloadDialogState.History) colors.primary else colors.fontDefault
            )
        }
        IconButton(onClick = { onModeSelected(SubDownloadDialogState.Login) }) {
            Icon(
                painter = painterResource(R.drawable.ic_account),
                contentDescription = stringResource(R.string.login),
                tint = if (mode == SubDownloadDialogState.Login) colors.primary else colors.fontDefault
            )
        }
    }
}

@Composable
private fun DownloadModeContent(
    results: List<SubtitleItem>,
    isLoading: Boolean,
    message: String,
    inError: Boolean,
    resultDescription: String,
    resultDescriptionTalkback: String,
    inEditMode: Boolean,
    searchName: String,
    searchSeason: String,
    searchEpisode: String,
    hearingImpaired: Boolean,
    languageEntries: List<String>,
    languageValues: List<String>,
    selectedLanguages: List<String>,
    onEditToggle: () -> Unit,
    onSearchNameChanged: (String) -> Unit,
    onSearchSeasonChanged: (String) -> Unit,
    onSearchEpisodeChanged: (String) -> Unit,
    onHearingImpairedChanged: (Boolean) -> Unit,
    onLanguagesChanged: (List<String>) -> Unit,
    onSearch: () -> Unit,
    onReset: () -> Unit,
    onCancelEdit: () -> Unit,
    onRetry: () -> Unit,
    onSubtitleClicked: (SubtitleItem) -> Unit,
    onSubtitleLongClicked: (SubtitleItem) -> Unit
) {
    SearchDescriptionCard(
        resultDescription = resultDescription,
        resultDescriptionTalkback = resultDescriptionTalkback,
        inEditMode = inEditMode,
        searchName = searchName,
        searchSeason = searchSeason,
        searchEpisode = searchEpisode,
        hearingImpaired = hearingImpaired,
        languageEntries = languageEntries,
        languageValues = languageValues,
        selectedLanguages = selectedLanguages,
        onEditToggle = onEditToggle,
        onSearchNameChanged = onSearchNameChanged,
        onSearchSeasonChanged = onSearchSeasonChanged,
        onSearchEpisodeChanged = onSearchEpisodeChanged,
        onHearingImpairedChanged = onHearingImpairedChanged,
        onLanguagesChanged = onLanguagesChanged,
        onSearch = onSearch,
        onReset = onReset,
        onCancelEdit = onCancelEdit
    )
    SubtitleList(
        subtitles = results,
        emptyText = message,
        loading = isLoading,
        inError = inError,
        onRetry = onRetry,
        onSubtitleClicked = onSubtitleClicked,
        onSubtitleLongClicked = onSubtitleLongClicked,
        modifier = Modifier.padding(top = 16.dp)
    )
}

@Composable
private fun SearchDescriptionCard(
    resultDescription: String,
    resultDescriptionTalkback: String,
    inEditMode: Boolean,
    searchName: String,
    searchSeason: String,
    searchEpisode: String,
    hearingImpaired: Boolean,
    languageEntries: List<String>,
    languageValues: List<String>,
    selectedLanguages: List<String>,
    onEditToggle: () -> Unit,
    onSearchNameChanged: (String) -> Unit,
    onSearchSeasonChanged: (String) -> Unit,
    onSearchEpisodeChanged: (String) -> Unit,
    onHearingImpairedChanged: (Boolean) -> Unit,
    onLanguagesChanged: (List<String>) -> Unit,
    onSearch: () -> Unit,
    onReset: () -> Unit,
    onCancelEdit: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Surface(
        color = colors.backgroundDefaultDarker,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 24.dp, end = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = resultDescription,
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = if (inEditMode) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEditToggle) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = resultDescriptionTalkback.ifEmpty { stringResource(R.string.subtitle_query_edit) },
                        tint = if (inEditMode) colors.primary else colors.fontDefault
                    )
                }
            }
            if (inEditMode) {
                SearchEditFields(
                    searchName = searchName,
                    searchSeason = searchSeason,
                    searchEpisode = searchEpisode,
                    hearingImpaired = hearingImpaired,
                    languageEntries = languageEntries,
                    languageValues = languageValues,
                    selectedLanguages = selectedLanguages,
                    onSearchNameChanged = onSearchNameChanged,
                    onSearchSeasonChanged = onSearchSeasonChanged,
                    onSearchEpisodeChanged = onSearchEpisodeChanged,
                    onHearingImpairedChanged = onHearingImpairedChanged,
                    onLanguagesChanged = onLanguagesChanged,
                    onSearch = onSearch,
                    onReset = onReset,
                    onCancelEdit = onCancelEdit
                )
            }
        }
    }
}

@Composable
private fun SearchEditFields(
    searchName: String,
    searchSeason: String,
    searchEpisode: String,
    hearingImpaired: Boolean,
    languageEntries: List<String>,
    languageValues: List<String>,
    selectedLanguages: List<String>,
    onSearchNameChanged: (String) -> Unit,
    onSearchSeasonChanged: (String) -> Unit,
    onSearchEpisodeChanged: (String) -> Unit,
    onHearingImpairedChanged: (Boolean) -> Unit,
    onLanguagesChanged: (List<String>) -> Unit,
    onSearch: () -> Unit,
    onReset: () -> Unit,
    onCancelEdit: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 16.dp)
    ) {
        OutlinedTextField(
            value = searchName,
            onValueChange = onSearchNameChanged,
            label = { Text(stringResource(R.string.subtitle_search_name_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = searchSeason,
                onValueChange = onSearchSeasonChanged,
                label = { Text(stringResource(R.string.subtitle_search_season_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = searchEpisode,
                onValueChange = onSearchEpisodeChanged,
                label = { Text(stringResource(R.string.subtitle_search_episode_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onSearch()
                }),
                modifier = Modifier.weight(1f)
            )
        }
        LanguageSelectorDropdown(
            languageEntries = languageEntries,
            languageValues = languageValues,
            selectedLanguages = selectedLanguages,
            onLanguagesChanged = onLanguagesChanged
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = hearingImpaired,
                onCheckedChange = onHearingImpairedChanged
            )
            Text(
                text = stringResource(R.string.sub_result_by_name_hearing_impaired),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onHearingImpairedChanged(!hearingImpaired) }
                    .padding(start = 4.dp)
            )
        }
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.reset))
            }
            TextButton(onClick = onCancelEdit) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    onSearch()
                },
                enabled = searchName.trim().isNotEmpty()
            ) {
                Text(stringResource(android.R.string.search_go))
            }
        }
    }
}

@Composable
private fun LanguageSelectorDropdown(
    languageEntries: List<String>,
    languageValues: List<String>,
    selectedLanguages: List<String>,
    onLanguagesChanged: (List<String>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabels = selectedLanguages.mapNotNull { value ->
        languageEntries.getOrNull(languageValues.indexOf(value))
    }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = if (selectedLabels.isEmpty()) {
                    stringResource(R.string.no_track_preference)
                } else {
                    selectedLabels.joinToString(", ")
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languageEntries.forEachIndexed { index, entry ->
                val value = languageValues.getOrNull(index) ?: return@forEachIndexed
                val selected = selectedLanguages.contains(value)
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = selected, onCheckedChange = null)
                            Text(text = entry, modifier = Modifier.padding(start = 8.dp))
                        }
                    },
                    onClick = {
                        val next = if (selected) {
                            selectedLanguages.filterNot { it == value }
                        } else {
                            selectedLanguages + value
                        }
                        val normalized = next.distinct().let { values ->
                            if (values.size == languageValues.size) emptyList() else values
                        }
                        onLanguagesChanged(normalized)
                    }
                )
            }
        }
    }
}

@Composable
private fun HistoryModeContent(
    history: List<SubtitleItem>,
    onSubtitleClicked: (SubtitleItem) -> Unit,
    onSubtitleLongClicked: (SubtitleItem) -> Unit
) {
    SubtitleList(
        subtitles = history,
        emptyText = stringResource(R.string.no_sub_history),
        loading = false,
        inError = false,
        onRetry = {},
        onSubtitleClicked = onSubtitleClicked,
        onSubtitleLongClicked = onSubtitleLongClicked,
        modifier = Modifier.padding(top = 16.dp)
    )
}

@Composable
private fun SubtitleList(
    subtitles: List<SubtitleItem>,
    emptyText: String,
    loading: Boolean,
    inError: Boolean,
    onRetry: () -> Unit,
    onSubtitleClicked: (SubtitleItem) -> Unit,
    onSubtitleLongClicked: (SubtitleItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp, max = 520.dp)
    ) {
        if (subtitles.isEmpty() && !loading && emptyText.isNotEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Text(
                    text = emptyText,
                    color = colors.fontLight,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (inError) {
                    TextButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(subtitles, key = { "${it.idSubtitle}-${it.state}-${it.downloadError}" }) { item ->
                    SubtitleRow(
                        item = item,
                        onClick = { onSubtitleClicked(item) },
                        onLongClick = { onSubtitleLongClicked(item) }
                    )
                }
            }
        }
        if (loading) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubtitleRow(
    item: SubtitleItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(start = 16.dp, top = 10.dp, end = 8.dp, bottom = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.movieReleaseName.trim(),
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (item.hearingImpaired) {
                    Icon(
                        painter = painterResource(R.drawable.ic_hearing_impaired),
                        contentDescription = stringResource(R.string.sub_result_by_name_hearing_impaired),
                        tint = colors.fontDefault,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(18.dp)
                    )
                }
                Text(
                    text = item.subLanguageID.trim().uppercase(Locale.getDefault()),
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                if (item.downloadError) {
                    Text(
                        text = stringResource(R.string.open_subs_download_link_error),
                        color = colors.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    if (item.downloadNumber > 0) {
                        Icon(
                            painter = painterResource(R.drawable.ic_download),
                            contentDescription = null,
                            tint = colors.fontLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = item.getReadableDownloadNumber(),
                            color = colors.fontLight,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    if (item.rating > 0) {
                        RatingStars(
                            rating = item.rating,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        when (item.state) {
            State.Downloading -> CircularProgressIndicator(modifier = Modifier.size(28.dp))
            State.Downloaded -> RowActionIcon(icon = R.drawable.ic_done, contentDescription = R.string.downloaded)
            State.NotDownloaded -> RowActionIcon(icon = R.drawable.ic_download_subtitles, contentDescription = R.string.not_downloaded)
        }
    }
    HorizontalDivider()
}

@Composable
private fun RowActionIcon(
    @DrawableRes icon: Int,
    @StringRes contentDescription: Int
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = stringResource(contentDescription),
        tint = VLCThemeDefaults.colors.fontDefault,
        modifier = Modifier.size(32.dp)
    )
}

@Composable
private fun RatingStars(
    rating: Float,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        for (index in 1..5) {
            Icon(
                painter = painterResource(starIconFor(rating, index)),
                contentDescription = null,
                tint = VLCThemeDefaults.colors.fontLight,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@DrawableRes
private fun starIconFor(rating: Float, index: Int): Int {
    val threshold = index * 2 - 1
    return when {
        rating < threshold -> R.drawable.ic_star_border
        rating == threshold.toFloat() -> R.drawable.ic_star_half
        else -> R.drawable.ic_star
    }
}

@Composable
private fun LoginModeContent(
    user: OpenSubtitlesUser,
    limit: OpenSubtitlesLimit,
    username: String,
    password: String,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClicked: () -> Unit,
    onRegisterClicked: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 24.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (user.logged) {
                stringResource(R.string.open_subtitles_logged_in, user.username)
            } else {
                stringResource(R.string.open_subtitles_not_logged_in)
            },
            color = colors.primary,
            style = MaterialTheme.typography.titleMedium
        )
        if (user.logged) {
            Text(
                text = stringResource(if (user.isVip()) R.string.open_subtitles_is_vip else R.string.open_subtitles_is_not_vip),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.open_subtitles_limits,
                    limit.getRemaining(),
                    limit.max,
                    limit.getResetTime(LocalContext.current)
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChanged,
                label = { Text(stringResource(R.string.open_subtitles_username)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChanged,
                label = { Text(stringResource(R.string.open_subtitles_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        }
        val errorMessage = user.errorMessage
        if (!errorMessage.isNullOrEmpty()) {
            Surface(color = colors.backgroundDefaultDarker) {
                Text(
                    text = errorMessage,
                    color = colors.error,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!user.logged) {
                TextButton(onClick = onRegisterClicked) {
                    Text(stringResource(R.string.open_subtitles_register))
                }
            }
            TextButton(onClick = onLoginClicked) {
                Text(stringResource(if (user.logged) R.string.open_subtitles_log_out else R.string.login))
            }
        }
    }
}

private fun Spanned?.plainText(): String = this?.toString().orEmpty()

enum class SubDownloadDialogState {
    Download,
    History,
    Login
}
