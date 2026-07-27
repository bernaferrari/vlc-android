package org.videolan.vlc

import android.Manifest
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.vlc.util.Permissions

@RunWith(RobolectricTestRunner::class)
@Config(application = VLCTestApplication::class, manifest = Config.NONE, sdk = [36])
abstract class BaseTest {
    val context: Context = ApplicationProvider.getApplicationContext()
    val application = (RuntimeEnvironment.application as VLCTestApplication)
    val medialibrary: Medialibrary

    //To prevent Method getMainLooper in android.os.Looper not mocked error when setting value for MutableLiveData
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    init {
        MockKAnnotations.init(this)
        medialibrary = MLServiceLocator.getAbstractMedialibrary().apply { init(context) }
    }

    @Before
    open fun beforeTest() {
        // Providers deliberately honour Android 13+ scoped-media permissions.
        // Give native-adapter tests the same granted state as a user who accepted
        // the library permission; permission-denied behavior is covered separately.
        Shadows.shadowOf(application).grantPermissions(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_IMAGES,
        )
        Permissions.emptyCache()
        println("beforeTest")
    }

    @After
    open fun afterTest() {
        println("afterTest")
        clearAllMocks()
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupTestClass() {
            Dispatchers.setMain(Dispatchers.Unconfined)
        }

        @AfterClass
        @JvmStatic
        fun cleanupTestClass() {
            unmockkAll()
        }
    }
}
