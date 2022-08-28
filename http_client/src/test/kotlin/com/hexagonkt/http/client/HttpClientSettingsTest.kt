package com.hexagonkt.http.client

import com.hexagonkt.http.model.HttpFields
import org.junit.jupiter.api.Test
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class HttpClientSettingsTest {

    @Test fun `Default HTTP client settings contains the proper values`() {
        HttpClientSettings().let {
            assertEquals(URL("http://localhost:2010"), it.baseUrl)
            assertNull(it.contentType)
            assertTrue(it.useCookies)
            assertEquals(HttpFields(), it.headers)
            assertFalse(it.insecure)
            assertNull(it.sslSettings)
        }
    }
}
