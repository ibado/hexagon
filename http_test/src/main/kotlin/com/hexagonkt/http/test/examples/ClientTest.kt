package com.hexagonkt.http.test.examples

import com.hexagonkt.core.multiMapOf
import com.hexagonkt.core.multiMapOfLists
import com.hexagonkt.core.require
import com.hexagonkt.core.media.ApplicationMedia.JSON
import com.hexagonkt.core.media.ApplicationMedia.YAML
import com.hexagonkt.http.SslSettings
import com.hexagonkt.http.client.HttpClient
import com.hexagonkt.http.client.HttpClientPort
import com.hexagonkt.http.client.HttpClientSettings
import com.hexagonkt.http.client.model.HttpClientRequest
import com.hexagonkt.http.client.model.HttpClientResponse
import com.hexagonkt.http.formatQueryString
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.http.model.HttpCookie
import com.hexagonkt.http.model.HttpMethod.GET
import com.hexagonkt.http.model.HttpProtocol.HTTPS
import com.hexagonkt.http.model.ServerErrorStatus.INTERNAL_SERVER_ERROR
import com.hexagonkt.http.model.SuccessStatus.OK
import com.hexagonkt.http.server.*
import com.hexagonkt.http.server.handlers.HttpCallback
import com.hexagonkt.http.server.handlers.ServerHandler
import com.hexagonkt.http.server.handlers.path
import com.hexagonkt.http.test.BaseTest
import com.hexagonkt.serialization.SerializationFormat
import com.hexagonkt.serialization.SerializationManager
import com.hexagonkt.serialization.serialize
import org.junit.jupiter.api.*

import java.math.BigInteger
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("FunctionName") // This class's functions are intended to be used only in tests
abstract class ClientTest(
    final override val clientAdapter: () -> HttpClientPort,
    final override val serverAdapter: () -> HttpServerPort,
    private val serializationFormats: List<SerializationFormat>,
    final override val serverSettings: HttpServerSettings = HttpServerSettings(),
) : BaseTest() {

    private var callback: HttpCallback = { this }

    override val handler: ServerHandler = path {
        post("/*") { callback() }
        get("/*") { callback() }
        head("/*") { callback() }
        put("/*") { callback() }
        delete("/*") { callback() }
        trace("/*") { callback() }
        options("/*") { callback() }
        patch("/*") { callback() }
    }

    @BeforeAll fun setUpSerializationFormats() {
        SerializationManager.formats = serializationFormats.toSet()
        SerializationManager.defaultFormat = serializationFormats.firstOrNull()
    }

    @BeforeEach fun resetHandler() {
        callback = {
            val contentType = ContentType(JSON, charset = Charsets.UTF_8)
            val bodyString = request.bodyString()
            val bodyHeader = if (bodyString.endsWith("\n") || bodyString.contains("{")) "json" else bodyString
            ok(
                body = bodyString,
                headers = response.headers
                    + ("body" to bodyHeader)
                    + ("ct" to (request.contentType?.text ?: ""))
                    + ("query-parameters" to formatQueryString(queryParameters)),
                contentType = contentType,
            )
        }
    }

    @Test fun `Exceptions are returned as internal server errors`() {
        callback = { error("failure") }

        val response = client.send(HttpClientRequest())

        assertEquals(INTERNAL_SERVER_ERROR, response.status)
        assertTrue(response.bodyString().contains("failure"))
    }

    @Test fun `Form parameters are sent correctly`() {
        callback = { ok(headers = formParameters) }

        val response = client.send(
            HttpClientRequest(
                formParameters = multiMapOf(
                    "p1" to "v11",
                    "p2" to "v21",
                    "p2" to "v22",
                )
            )
        )

        val expectedHeaders = multiMapOfLists("p1" to listOf("v11"), "p2" to listOf("v21", "v22"))
        val actualHeaders = response.headers - "transfer-encoding" - "content-length" - "connection"
        assertEquals(expectedHeaders, actualHeaders)
    }

    @Test fun `Cookies are sent correctly`() {
        callback = {
            val cookiesMap = request.cookiesMap()
            assertEquals(HttpCookie("c1", "v1"), cookiesMap["c1"])
            assertEquals(HttpCookie("c2", "v2", -1), cookiesMap["c2"])
            assertNull(cookiesMap["c3"]) // Secure headers only sent through HTTPS
            ok(cookies = listOf(
                HttpCookie("c4", "v4", 60),
                HttpCookie("c5", "v5", secure = true),
            ))
        }

        client.cookies = emptyList()
        val response = client.send(
            HttpClientRequest(
                cookies = listOf(
                    HttpCookie("c1", "v1"),
                    HttpCookie("c2", "v2", 1),
                    HttpCookie("c3", "v3", secure = true),
                )
            )
        )

        val responseC4 = response.cookiesMap().require("c4")
        assertEquals("v4", responseC4.value)
        assertTrue(responseC4.maxAge in 59..60)
        assertEquals(HttpCookie("c5", "v5", secure = true), response.cookiesMap()["c5"])

        val clientC4 = client.cookiesMap().require("c4")
        assertEquals("v4", clientC4.value)
        assertTrue(clientC4.maxAge in 59..60)
        assertEquals(HttpCookie("c5", "v5", secure = true), client.cookiesMap()["c5"])
    }

    @Test fun `Create HTTP clients`() {
        val adapter = clientAdapter()

        // clientCreation
        HttpClient(adapter)
        HttpClient(adapter, URL("http://host:1234/base"))
        // clientCreation

        // clientSettingsCreation
        // All client settings parameters are optionals and provide default values
        HttpClient(adapter, HttpClientSettings(
            baseUrl = URL("http://host:1234/base"),
            contentType = ContentType(JSON),
            useCookies = true,
            headers = multiMapOf("x-api-Key" to "cafebabe"), // Headers used in all requests
            insecure = false,               // If true, the client doesn't check server certificates
            sslSettings = SslSettings()     // Key stores settings (check TLS section for details)
        ))
        // clientSettingsCreation
    }

    @Test fun `JSON requests works as expected`() {
        val expectedBody = "{  \"foo\" : \"fighters\",  \"es\" : \"áéíóúÁÉÍÓÚñÑ\"}"
        val requestBody = mapOf("foo" to "fighters", "es" to "áéíóúÁÉÍÓÚñÑ").serialize(JSON)

        val response = client.post("/", requestBody, contentType = ContentType(JSON))
        assertEquals(expectedBody, response.body.toString().trim().replace("[\r\n]".toRegex(), ""))
        assertEquals(ContentType(JSON).text, response.headers["ct"])

        val body2 = client.post("/", requestBody).body
        assertEquals(expectedBody, body2.toString().trim().replace("[\r\n]".toRegex(), ""))
    }

    @Test fun `HTTP generic requests work ok`() {

        // genericRequest
        val request = HttpClientRequest(
            method = GET,
            path = "/",
            body = mapOf("body" to "payload").serialize(),
            headers = multiMapOf("x-header" to "value"),
            queryParameters = multiMapOf("qp" to "qpValue"),
            contentType = ContentType(JSON)
        )

        val response = client.send(request)
        // genericRequest

        val getResponse = client.get("/queryParameters?qp=qpValue")
        assertEquals("qp=qpValue", getResponse.headers["query-parameters"])
        assertEquals("qp=qpValue", response.headers["query-parameters"])
        checkResponse(response, mapOf("body" to "payload"))
    }

    @Test fun `HTTP methods without body work ok`() {

        // withoutBodyRequests
        val responseGet = client.get("/")
        val responseHead = client.head("/")
        val responsePost = client.post("/")
        val responsePut = client.put("/")
        val responseDelete = client.delete("/")
        val responseTrace = client.trace("/")
        val responseOptions = client.options("/")
        val responsePatch = client.patch("/")
        // withoutBodyRequests

        checkResponse(responseGet, null)
        checkResponse(responseHead, null)
        checkResponse(responsePost, null)
        checkResponse(responsePut, null)
        checkResponse(responseDelete, null)
        checkResponse(responseTrace, null)
        checkResponse(responseOptions, null)
        checkResponse(responsePatch, null)
    }

    @Test fun `HTTP methods with body work ok`() {

        // bodyRequests
        val body = mapOf("key" to "value")
        val serializedBody = body.serialize()

        val responseGet = client.get("/", body = serializedBody)
        val responsePost = client.post("/", serializedBody)
        val responsePut = client.put("/", serializedBody)
        val responseDelete = client.delete("/", serializedBody)
        val responseTrace = client.trace("/", serializedBody)
        val responseOptions = client.options("/", serializedBody)
        val responsePatch = client.patch("/", serializedBody)
        // bodyRequests

        checkResponse(responseGet, body)
        checkResponse(responsePost, body)
        checkResponse(responsePut, body)
        checkResponse(responseDelete, body)
        checkResponse(responseTrace, body)
        checkResponse(responseOptions, body)
        checkResponse(responsePatch, body)
    }

    @Test fun `HTTP methods with body and content type work ok`() {

        // bodyAndContentTypeRequests
        val body = mapOf("key" to "value")
        val serializedBody = body.serialize(YAML)

        val responseGet = client.get("/", body = serializedBody, contentType = ContentType(YAML))
        val responsePost = client.post("/", serializedBody, contentType = ContentType(YAML))
        val responsePut = client.put("/", serializedBody, contentType = ContentType(YAML))
        val responseDelete = client.delete("/", serializedBody, contentType = ContentType(YAML))
        val responseTrace = client.trace("/", serializedBody, contentType = ContentType(YAML))
        val responseOptions = client.options("/", serializedBody, contentType = ContentType(YAML))
        val responsePatch = client.patch("/", serializedBody, contentType = ContentType(YAML))
        // bodyAndContentTypeRequests

        checkResponse(responseGet, body, ContentType(YAML))
        checkResponse(responsePost, body, ContentType(YAML))
        checkResponse(responsePut, body, ContentType(YAML))
        checkResponse(responseDelete, body, ContentType(YAML))
        checkResponse(responseTrace, body, ContentType(YAML))
        checkResponse(responseOptions, body, ContentType(YAML))
        checkResponse(responsePatch, body, ContentType(YAML))
    }

    @Test fun `Parameters are set properly` () {
        val endpoint = URL("http://localhost:${server.runtimePort}")
        val h = multiMapOfLists("header1" to listOf("val1", "val2"))
        val settings = HttpClientSettings(
            contentType = ContentType(JSON),
            useCookies = false,
            headers = h,
            insecure = true
        )
        val c = HttpClient(clientAdapter(), endpoint, settings)

        assertEquals(c.settings.contentType, ContentType(JSON))
        assert(!c.settings.useCookies)
        assertEquals(c.settings.headers, h)

        callback = {
            val headers = multiMapOfLists("head1" to request.headers.allValues.require("header1"))
            ok(headers = response.headers + headers)
        }

        c.use {
            it.start()
            it.get("/auth").apply {
                assertEquals(listOf("val1", "val2"), headers.allValues["head1"])
                assertEquals(status, OK)
            }
        }
    }

    @Test fun `Integers are sent properly` () {
        var run: Boolean

        callback = {
            val contentType = ContentType(JSON, charset = Charsets.UTF_8)
            val number = BigInteger(request.body as ByteArray).toLong()
            ok(
                body = number,
                headers = response.headers + ("body" to number.toString()),
                contentType = contentType
            )
        }

        client.post("/string", 42).apply {
            assertEquals("42", headers.require("body"))
            assertEquals(status, OK)
            run = true
        }

        assert(run)
    }

    @Test fun `Strings are sent properly` () {
        var run: Boolean

        client.post("/string", "text").apply {
            assert(headers["body"]?.isNotEmpty() ?: false)
            assertEquals(status, OK)
            run = true
        }

        assert(run)
    }

    @Test fun `Request HTTPS example`() {

        val serverAdapter = serverAdapter()

        // Key store files
        val identity = "hexagonkt.p12"
        val trust = "trust.p12"

        // Default passwords are file name reversed
        val keyStorePassword = identity.reversed()
        val trustStorePassword = trust.reversed()

        // Key stores can be set as URIs to classpath resources (the triple slash is needed)
        val keyStore = URL("classpath:ssl/$identity")
        val trustStore = URL("classpath:ssl/$trust")

        val sslSettings = SslSettings(
            keyStore = keyStore,
            keyStorePassword = keyStorePassword,
            trustStore = trustStore,
            trustStorePassword = trustStorePassword,
            clientAuth = true // Requires a valid certificate from the client (mutual TLS)
        )

        val serverSettings = serverSettings.copy(
            bindPort = 0,
            protocol = HTTPS, // You can also use HTTP2
            sslSettings = sslSettings
        )

        val server = serve(serverAdapter, serverSettings) {
            get("/hello") {
                // We can access the certificate used by the client from the request
                val subjectDn = request.certificate()?.subjectX500Principal?.name ?: ""
                ok("Hello World!", headers = response.headers + ("cert" to subjectDn) )
            }
        }

        // We'll use the same certificate for the client (in a real scenario it would be different)
        val clientSettings = HttpClientSettings(
            baseUrl = URL("https://localhost:${server.runtimePort}"),
            sslSettings = sslSettings
        )

        // Create an HTTP client and make an HTTPS request
        val client = HttpClient(clientAdapter(), clientSettings)
        client.start()
        client.get("/hello").apply {
            // Assure the certificate received (and returned) by the server is correct
            assert(headers.require("cert").startsWith("CN=hexagonkt.com"))
            assertEquals(body, "Hello World!")
        }

        client.stop()
        server.stop()
    }

    private fun checkResponse(
        response: HttpClientResponse,
        parameter: Map<String, String>?,
        format: ContentType = ContentType(JSON),
    ) {

        assertEquals(OK, response.status)
        assertEquals(
            parameter?.serialize(format.mediaType)?.trim() ?: "",
            response.bodyString().trim()
        )
    }
}
