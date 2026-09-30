package com.fastiptv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BuildSanityTest {

    @Test
    fun testAppNamespaceAndVersion() {
        assertEquals("com.fastiptv", BuildConfig.APPLICATION_ID)
        assertEquals("1.0.8", BuildConfig.VERSION_NAME)
        assertEquals(9, BuildConfig.VERSION_CODE)
    }

    @Test
    fun testFastIptvAppClassExists() {
        val appClass = FastIptvApp::class.java
        assertNotNull(appClass)
    }
}
