package io.github.hanxmoneai.sunmibridge

import io.github.hanxmoneai.sunmibridge.config.UrlNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlNormalizerTest {

    @Test
    fun addsHttpsWhenSchemeMissing() {
        assertEquals("https://shop.example.com/pos", UrlNormalizer.normalize("  shop.example.com/pos "))
    }

    @Test
    fun keepsExplicitHttpForLanServers() {
        assertEquals("http://192.168.1.20:8080/#/home", UrlNormalizer.normalize("http://192.168.1.20:8080/#/home"))
        assertEquals("HTTPS://Example.com", UrlNormalizer.normalize("HTTPS://Example.com"))
    }

    @Test
    fun allowsBuiltInDemoPage() {
        assertEquals(UrlNormalizer.DEMO_URL, UrlNormalizer.normalize(UrlNormalizer.DEMO_URL))
    }

    @Test
    fun rejectsUnusableInput() {
        assertNull(UrlNormalizer.normalize(null))
        assertNull(UrlNormalizer.normalize("   "))
        assertNull(UrlNormalizer.normalize("https://"))
        assertNull(UrlNormalizer.normalize("ftp://example.com"))
        assertNull(UrlNormalizer.normalize("file:///sdcard/x.html"))
        assertNull(UrlNormalizer.normalize("javascript:alert(1)"))
        assertNull(UrlNormalizer.normalize("my shop.com"))
    }
}
