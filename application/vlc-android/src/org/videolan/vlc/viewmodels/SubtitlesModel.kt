package org.videolan.vlc.viewmodels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.text.Spanned
import android.util.Log
import androidx.core.text.HtmlCompat
import androidx.core.text.toSpanned
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import org.videolan.resources.opensubtitles.OpenSubtitlesLimit
import org.videolan.resources.opensubtitles.OpenSubtitlesUser
import org.videolan.resources.opensubtitles.OpenSubtitlesUtils
import org.videolan.resources.AppContextProvider
import org.videolan.resources.opensubtitles.Data
import org.videolan.resources.opensubtitles.OpenSubV1
import org.videolan.resources.opensubtitles.OpenSubtitleRepository
import org.videolan.resources.util.NoConnectivityException
import org.videolan.tools.CoroutineContextProvider
import org.videolan.tools.FileUtils
import org.videolan.tools.Settings
import org.videolan.tools.getContextWithLocale
import org.videolan.tools.putSingle
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.R
import org.videolan.vlc.gui.dialogs.State
import org.videolan.vlc.gui.dialogs.SubtitleItem
import org.videolan.vlc.repository.ExternalSubRepository
import org.videolan.vlc.util.TextUtils
import java.io.File
import java.util.Locale
import java.util.MissingResourceException

private const val LAST_USED_LANGUAGES = "last_used_subtitles"

class SubtitlesModel(context: Context, private val mediaUri: Uri, private val name:String, val coroutineContextProvider: CoroutineContextProvider = CoroutineContextProvider()) : AndroidViewModel(context.applicationContext as Application) {
    val searchName = MutableStateFlow("")
    val searchEpisode = MutableStateFlow("")
    val searchSeason = MutableStateFlow("")
    val searchLanguage = MutableStateFlow<List<String>>(emptyList())
    val searchHearingImpaired = MutableStateFlow(false)
    val inEditMode = MutableStateFlow(false)
    val user = MutableStateFlow(OpenSubtitlesUser())
    val limit = MutableStateFlow(OpenSubtitlesLimit())
    private var previousSearchLanguage: List<String>? = null
    val manualSearchEnabled = MutableStateFlow(false)

    val isApiLoading: MediatorLiveData<Boolean> = MediatorLiveData()
    val message = MutableStateFlow("")
    val error = MutableStateFlow(false)
    val resultDescription = MutableStateFlow<Spanned?>(null)
    val resultDescriptionTalkback = MutableStateFlow("")

    private var lastUsername: String = ""
    private var lastPassword: String = ""

    val oldLanguagesMigration by lazy {
        val newLangCodes =  context.resources.getStringArray(R.array.language_values)
        val oldLangCodes =  context.resources.getStringArray(R.array.old_language_values)
        val newLangEntries =  context.resources.getStringArray(R.array.language_entries)
        val oldLangEntries =  context.resources.getStringArray(R.array.old_language_entries)
        val mapping = HashMap<String, String>()
        for (i in oldLangCodes.indices) {
            for (j in newLangCodes.indices) {
                if (newLangEntries[j] == oldLangEntries[i]) {
                    mapping[oldLangCodes[i]] = newLangCodes[j]
                    break
                }
            }
        }
        mapping
    }

    private fun getContext() =
        getApplication<Application>().getContextWithLocale(AppContextProvider.locale)

    private val apiResultLiveData: MutableLiveData<List<Data>> = MutableLiveData()
    private val downloadedLiveData = ExternalSubRepository.getInstance(context).getDownloadedSubtitles(mediaUri).map { list ->
        list.map { SubtitleItem(it.idSubtitle, -1, mediaUri, it.subLanguageID, it.movieReleaseName, State.Downloaded, "", it.hearingImpaired, 0F, 0) }
    }

    private val downloadingLiveData = ExternalSubRepository.getInstance(context).downloadingSubtitles

    val result: MediatorLiveData<List<SubtitleItem>> = MediatorLiveData()
    val history: MediatorLiveData<List<SubtitleItem>> = MediatorLiveData()

    private var searchJob: Job? = null
    init {
        viewModelScope.launch {
            searchLanguage.collect { languages ->
                if (languages != previousSearchLanguage) {
                    previousSearchLanguage = languages
                    saveLastUsedLanguage(languages)
                    search(!manualSearchEnabled.value)
                }
            }
        }

        history.apply {
            addSource(downloadedLiveData) {
                viewModelScope.launch {
                    value = merge(it, downloadingLiveData.value?.values?.filter { it.mediaUri == mediaUri })
                }
            }

            addSource(downloadingLiveData) {
                viewModelScope.launch {
                    value = merge(downloadedLiveData.value, it?.values?.filter { it.mediaUri == mediaUri })
                }
            }
        }

        result.apply {
            addSource(apiResultLiveData) {
                viewModelScope.launch {
                    value = updateListState(it, history.value)
                }

            }

            addSource(history) {
                viewModelScope.launch {
                    value = updateListState(apiResultLiveData.value, it)
                }
            }
        }
    }

    private suspend fun merge(downloadedResult: List<SubtitleItem>?, downloadingResult: List<SubtitleItem>?): List<SubtitleItem> = withContext(coroutineContextProvider.Default) {
        downloadedResult.orEmpty() + downloadingResult?.toList().orEmpty()
    }

    private suspend fun updateListState(apiResultLiveData: List<Data>?, history: List<SubtitleItem>?): MutableList<SubtitleItem> = withContext(coroutineContextProvider.Default) {
        val list = mutableListOf<SubtitleItem>()
        apiResultLiveData?.forEach { openSubtitle ->
            val exist = history?.find { it.idSubtitle == openSubtitle.attributes.subtitleId }
            val state = exist?.state ?: State.NotDownloaded
            if (openSubtitle.attributes.files.isNotEmpty()) {
                list.add(
                    SubtitleItem(
                        openSubtitle.attributes.subtitleId,
                        openSubtitle.attributes.files.first().fileId,
                        mediaUri,
                        openSubtitle.attributes.language,
                        openSubtitle.attributes.featureDetails.movieName,
                        state,
                        "",
                        openSubtitle.attributes.hearingImpaired,
                        openSubtitle.attributes.ratings,
                        openSubtitle.attributes.downloadCount
                    )
                )
            }
        }
        list
    }

    private suspend fun getSubtitleByName(name: String, episode: Int?, season: Int?, languageIds: List<String>?, hearingImpaired: Boolean): OpenSubV1 {
        if (BuildConfig.DEBUG) Log.d(this::class.java.simpleName, "Getting subs by name with $name")
        val builder = StringBuilder(getContext().getString(R.string.sub_result_by_name, "<i>$name</i>"))
        season?.let { builder.append(" ${TextUtils.SEPARATOR} ").append(getContext().getString(R.string.sub_result_by_name_season, "<i>$it</i>")) }
        episode?.let { builder.append(" ${TextUtils.SEPARATOR} ").append(getContext().getString(R.string.sub_result_by_name_episode, "<i>$it</i>")) }
        languageIds?.let { languages -> if (languageIds.isNotEmpty()) builder.append(" ${TextUtils.SEPARATOR} ").append("<i>${languages.joinToString(", "){ it.uppercase()} }</i>") }
        if (hearingImpaired) builder.append(" ${TextUtils.SEPARATOR} ").append(getContext().getString(R.string.sub_result_by_name_hearing_impaired))
        resultDescription.value = HtmlCompat.fromHtml(builder.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
        val talkbackBuilder = StringBuilder(getContext().getString(R.string.sub_result_by_name, name))
        season?.let { talkbackBuilder.append(". ").append(getContext().getString(R.string.sub_result_by_name_season, "$it")) }
        episode?.let { talkbackBuilder.append(". ").append(getContext().getString(R.string.sub_result_by_name_episode, "$it")) }
        val langEntries = getContext().resources.getStringArray(R.array.language_entries)
        val langValues = getContext().resources.getStringArray(R.array.language_values)
        languageIds?.let { languages -> if (languageIds.isNotEmpty()) talkbackBuilder.append(". ").append(
            languages.joinToString(", "){
                val index = langValues.indexOf(it)
                if (index != -1) langEntries[index] else it
            }) }
        if (hearingImpaired) talkbackBuilder.append(". ").append(getContext().getString(R.string.sub_result_by_name_hearing_impaired))
        resultDescriptionTalkback.value = talkbackBuilder.toString()
        manualSearchEnabled.value = true
        return OpenSubtitleRepository.getInstance().queryWithName(name, episode, season, languageIds, hearingImpaired)
    }

    private suspend fun getSubtitleByHash(movieHash: String?, languageIds: List<String>?, hearingImpaired: Boolean): OpenSubV1 {
        if (BuildConfig.DEBUG) Log.d(this::class.java.simpleName, "Getting subs by hash with $movieHash")
        manualSearchEnabled.value = false
        resultDescription.value = getContext().getString(R.string.sub_result_by_file).toSpanned()
        return OpenSubtitleRepository.getInstance().queryWithHash(movieHash, languageIds, hearingImpaired)
    }

    fun onRefresh() {
        if (manualSearchEnabled.value && searchName.value.isBlank()) {
            isApiLoading.postValue(false)
            return
        }

        search(!manualSearchEnabled.value)
    }

    fun search(byFile: Boolean) {
        searchJob?.cancel()
        isApiLoading.postValue(true)
        message.value = ""
        error.value = false
        apiResultLiveData.postValue(listOf())

        searchJob = viewModelScope.launch {
            try {
                val subs = if (byFile) {
                    withContext(coroutineContextProvider.IO) {
                        val videoFile = File(mediaUri.path)
                        if (videoFile.exists()) {
                            val hash = FileUtils.computeHash(videoFile)
                            val hashSubs = getSubtitleByHash(hash, searchLanguage.value, searchHearingImpaired.value).data
                            // No result for hash. Falling back to name search
                            if (hashSubs.isEmpty()) getSubtitleByName(videoFile.name, null, null, searchLanguage.value, searchHearingImpaired.value).data else hashSubs
                        } else {
                            getSubtitleByName(name, null, null, searchLanguage.value, searchHearingImpaired.value).data
                        }

                    }
                } else {
                    searchName.value.takeIf { it.isNotBlank() }?.let {
                        val episode = try {
                            searchEpisode.value.toIntOrNull()
                        } catch (e: NumberFormatException) {
                            null
                        }
                        val season = try {
                            searchSeason.value.toIntOrNull()
                        } catch (e: NumberFormatException) {
                            null
                        }
                        getSubtitleByName(it, episode, season, searchLanguage.value, searchHearingImpaired.value).data
                    } ?: listOf()
                }
                if (isActive) apiResultLiveData.postValue(subs)
                if (subs.isEmpty()) {
                    message.value = getContext().getString(R.string.no_result)
                } else {
                    message.value = ""
                }
                error.value = false
            } catch (e: Exception) {
                Log.e("SubtitlesModel", e.message, e)
                error.value = true
                if (e is NoConnectivityException)
                    message.value = getContext().getString(R.string.no_internet_connection)
                else
                    message.value = getContext().getString(R.string.open_subs_download_error)
            } finally {
                isApiLoading.postValue(false)
            }
        }
    }

    fun deleteSubtitle(mediaPath: String, idSubtitle: String) {
        ExternalSubRepository.getInstance(getContext()).deleteSubtitle(mediaPath, idSubtitle)
    }

    fun getLastUsedLanguage(): List<String> {
        val language = try {
            Locale.getDefault().language
        } catch (e: MissingResourceException) {
            "en"
        }
        return Settings.getInstance(getContext()).getStringSet(LAST_USED_LANGUAGES, setOf(language))?.map { if (it.length > 2) migrateFromOld(it) ?: it else it } ?: emptyList()
    }

    fun login(settings: SharedPreferences, username: String, password: String) {
        if (lastPassword == username && lastUsername == password) {
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val call = OpenSubtitleRepository.getInstance().login(username, password)
                    if (call.isSuccessful) {
                        val userResult = call.body()
                        if (userResult != null) {
                            val openSubtitlesUser =
                                OpenSubtitlesUser(true, userResult, username = username)
                            OpenSubtitlesUtils.saveUser(settings, openSubtitlesUser)
                            user.value = openSubtitlesUser
                            checkUserInfos(settings)
                            return@withContext
                        }
                    }
                    val code = call.code()
                    if (code == 401) {
                        lastPassword = password
                        lastUsername = username
                    }
                    user.value =
                        OpenSubtitlesUser(
                            false,
                            null,
                            errorMessage = if (code == 401) getContext().getString(R.string.login_error) else getContext().getString(
                                R.string.unknown_error
                            )
                        )
                } catch (e: NoConnectivityException) {
                    user.value =
                        OpenSubtitlesUser(
                            false,
                            null,
                            errorMessage = getContext().getString(R.string.no_internet_connection)
                        )
                }

            }
        }
    }

    fun checkUserInfos(settings: SharedPreferences) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val callInfo = OpenSubtitleRepository.getInstance().userInfo()
                if (callInfo.isSuccessful) {
                    val userInfo = callInfo.body()
                    if (userInfo != null) {
                        val limit = OpenSubtitlesUtils.getLimit(settings)
                        limit.max = userInfo.data.allowedDownloads
                        limit.requests = userInfo.data.downloadsCount
                        OpenSubtitlesUtils.saveLimit(settings, limit)
                        this@SubtitlesModel.limit.value = limit
                    }
                }
            }
        }
    }


    fun logout(settings: SharedPreferences) {
        val user = OpenSubtitlesUser()
        OpenSubtitlesUtils.saveUser(settings, user)
        this.user.value = user
        val limit = OpenSubtitlesLimit()
        OpenSubtitlesUtils.saveLimit(settings, limit)
        this.limit.value = limit
    }

    private fun migrateFromOld(it: String?): String? {
        return oldLanguagesMigration[it]
    }

    fun saveLastUsedLanguage(lastUsedLanguages: List<String>) = Settings.getInstance(getContext()).putSingle(LAST_USED_LANGUAGES, lastUsedLanguages)
    fun clearCredentials() {
        lastPassword = ""
        lastUsername = ""
    }

    class Factory(private val context: Context, private val mediaUri: Uri, private val name: String) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SubtitlesModel(context.applicationContext, mediaUri, name) as T
        }
    }

}
