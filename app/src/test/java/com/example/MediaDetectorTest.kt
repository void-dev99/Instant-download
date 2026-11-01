package com.example

import com.example.data.detector.DirectPlatformAdapter
import com.example.data.detector.MediaDetector
import com.example.data.detector.PinterestPlatformAdapter
import com.example.data.detector.RedditPlatformAdapter
import com.example.data.detector.XPlatformAdapter
import com.example.data.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaDetectorTest {

    @Test
    fun extractUrlsFromText_singleUrl_extractsCorrectly() {
        val text = "Check out this reel https://www.instagram.com/p/C7Xyz123/ so cool!"
        val urls = MediaDetector.extractUrlsFromText(text)
        assertEquals(1, urls.size)
        assertEquals("https://www.instagram.com/p/C7Xyz123/", urls[0])
    }

    @Test
    fun extractUrlsFromText_multipleUrls_extractsAll() {
        val text = "First link: https://example.com/video.mp4 and second: https://reddit.com/r/test"
        val urls = MediaDetector.extractUrlsFromText(text)
        assertEquals(2, urls.size)
        assertEquals("https://example.com/video.mp4", urls[0])
        assertEquals("https://reddit.com/r/test", urls[1])
    }

    @Test
    fun extractUrlsFromText_trailingPunctuation_trimsProperly() {
        val text = "Look at this (https://example.com/img.jpg)."
        val urls = MediaDetector.extractUrlsFromText(text)
        assertEquals(1, urls.size)
        assertEquals("https://example.com/img.jpg", urls[0])
    }

    @Test
    fun mediaType_fromExtension_identifiesTypesCorrectly() {
        assertEquals(MediaType.VIDEO, MediaType.fromMimeOrExtension(null, "mp4"))
        assertEquals(MediaType.VIDEO, MediaType.fromMimeOrExtension(null, "webm"))
        assertEquals(MediaType.IMAGE, MediaType.fromMimeOrExtension(null, "jpg"))
        assertEquals(MediaType.IMAGE, MediaType.fromMimeOrExtension(null, "png"))
        assertEquals(MediaType.GIF, MediaType.fromMimeOrExtension(null, "gif"))
        assertEquals(MediaType.AUDIO, MediaType.fromMimeOrExtension(null, "mp3"))
        assertEquals(MediaType.DOCUMENT, MediaType.fromMimeOrExtension(null, "pdf"))
    }

    @Test
    fun formatBytes_convertsToHumanReadable() {
        assertEquals("500 B", MediaDetector.formatBytes(500))
        assertEquals("50 KB", MediaDetector.formatBytes(51200))
        assertEquals("10.0 MB", MediaDetector.formatBytes(10485760))
        assertEquals("2.00 GB", MediaDetector.formatBytes(2147483648))
    }

    @Test
    fun platformAdapters_canHandleUrlsAppropriately() {
        val direct = DirectPlatformAdapter()
        assertTrue(direct.canHandle("https://sample.com/video.mp4"))
        assertTrue(direct.canHandle("https://sample.com/photo.jpg"))
        assertTrue(direct.canHandle("https://sample.com/anim.gif"))
        assertFalse(direct.canHandle("https://instagram.com/p/xyz"))

        val reddit = RedditPlatformAdapter()
        assertTrue(reddit.canHandle("https://www.reddit.com/r/funny/comments/123/video/"))
        assertTrue(reddit.canHandle("https://v.redd.it/xyz123"))

        val pinterest = PinterestPlatformAdapter()
        assertTrue(pinterest.canHandle("https://www.pinterest.com/pin/12345/"))

        val x = XPlatformAdapter()
        assertTrue(x.canHandle("https://x.com/user/status/123456"))
        assertTrue(x.canHandle("https://twitter.com/user/status/123456"))
    }
}
