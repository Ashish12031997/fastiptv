package com.fastiptv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BuildSanityTest {

    @Test
    fun testAppNamespaceAndVersion() {
        assertEquals("com.fastiptv", BuildConfig.APPLICATION_ID)
        assert(BuildConfig.VERSION_NAME.isNotBlank())
        assert(BuildConfig.VERSION_CODE >= 1)
    }

    @Test
    fun testFastIptvAppClassExists() {
        val appClass = FastIptvApp::class.java
        assertNotNull(appClass)
    }
}
