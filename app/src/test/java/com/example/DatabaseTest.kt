package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DownloadEntity
import com.example.data.local.entity.HistoryEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun downloadDao_insertAndRetrieve() = runBlocking {
        val entity = DownloadEntity(
            url = "https://sample.com/video.mp4",
            title = "Sample Video",
            platform = "Direct",
            mediaType = MediaType.VIDEO.name,
            fileLocation = "/data/sample.mp4",
            fileName = "sample.mp4",
            fileSizeBytes = 1024L,
            downloadedBytes = 1024L,
            status = DownloadStatus.COMPLETED.name
        )

        val id = db.downloadDao().insert(entity)
        val retrieved = db.downloadDao().getDownloadById(id)

        assertNotNull(retrieved)
        assertEquals("Sample Video", retrieved?.title)
        assertEquals(DownloadStatus.COMPLETED.name, retrieved?.status)

        val all = db.downloadDao().getAllDownloads().first()
        assertEquals(1, all.size)
    }

    @Test
    fun historyDao_insertAndClear() = runBlocking {
        val hist = HistoryEntity(
            url = "https://instagram.com/reel/123",
            title = "Funny Reel",
            platform = "Instagram",
            mediaCount = 1
        )

        db.historyDao().insert(hist)
        val list = db.historyDao().getAllHistory().first()
        assertEquals(1, list.size)

        db.historyDao().clearAll()
        val emptyList = db.historyDao().getAllHistory().first()
        assertEquals(0, emptyList.size)
    }

    @Test
    fun appSettingsDao_setAndGet() = runBlocking {
        db.appSettingsDao().setSetting(AppSettingsEntity("pref_theme_mode", "dark"))
        val value = db.appSettingsDao().getSettingValue("pref_theme_mode")
        assertEquals("dark", value)
    }
}
