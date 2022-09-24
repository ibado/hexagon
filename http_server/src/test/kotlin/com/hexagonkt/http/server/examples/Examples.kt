package com.hexagonkt.http.server.examples

import com.hexagonkt.core.encodeToBase64
import com.hexagonkt.http.model.HttpAuthorization
import com.hexagonkt.http.model.HttpMethod
import com.hexagonkt.http.model.HttpStatus
import com.hexagonkt.http.model.SuccessStatus.OK
import com.hexagonkt.http.parseQueryString
import com.hexagonkt.http.server.handlers.PathHandler
import com.hexagonkt.http.server.model.HttpServerRequest
import com.hexagonkt.http.server.model.HttpServerResponse
import kotlin.test.assertEquals

internal fun assertResponseContains(
    response: HttpServerResponse?, status: HttpStatus, vararg content: String) {

    assertEquals(status, response?.status)
    val payload = response?.body?.let { b -> b as String }
    content.forEach { assert(payload?.contains(it) ?: false) }
}

internal fun assertResponseContains(response: HttpServerResponse?, vararg content: String) {
    assertResponseContains(response, OK, *content)
}

internal fun PathHandler.send(
    method: HttpMethod,
    requestPath: String,
    query: String = "",
    user: String? = null,
    password: String? = null,
): HttpServerResponse =
    process(
        HttpServerRequest(
            method = method,
            path = requestPath,
            queryParameters = parseQueryString(query)
        )
        .auth(user, password)
    )

internal fun HttpServerRequest.auth(
    user: String? = null, password: String? = null): HttpServerRequest {

    val authorization =
        if (user != null || password != null)
            "$user:$password".encodeToBase64()
        else
            null

    return if (authorization != null)
        copy(authorization = HttpAuthorization("Basic", authorization))
    else
        this
}

internal fun assertResponseEquals(
    response: HttpServerResponse?, content: String, status: HttpStatus = OK) {

    assertEquals(status, response?.status)
    assertEquals(content, response?.body?.let { it as String })
}
