package com.hexagonkt.http.server.handlers

import com.hexagonkt.core.handlers.Callback
import com.hexagonkt.http.model.*
import com.hexagonkt.http.model.HttpMethod.*
import com.hexagonkt.http.model.HttpProtocol.HTTP
import com.hexagonkt.http.server.model.HttpServerCall
import com.hexagonkt.http.server.model.HttpServerRequest
import java.security.cert.X509Certificate
import kotlin.reflect.KClass
import kotlin.reflect.cast

typealias HttpCallback = HttpServerContext.() -> HttpServerContext
typealias HttpExceptionCallback<T> = HttpServerContext.(T) -> HttpServerContext

internal fun toCallback(handler: HttpCallback): Callback<HttpServerCall> =
    { context -> HttpServerContext(context).handler().context }

fun HttpCallback.process(
    request: HttpServerRequest,
    attributes: Map<*, *> = emptyMap<Any, Any>()
): HttpServerContext =
    this(HttpServerContext(request = request, attributes = attributes))

fun HttpCallback.process(
    method: HttpMethod = GET,
    protocol: HttpProtocol = HTTP,
    host: String = "localhost",
    port: Int = 80,
    path: String = "",
    queryParameters: HttpFields<QueryParameter> = HttpFields(),
    headers: HttpFields<Header> = HttpFields(),
    body: Any = "",
    parts: List<HttpPart> = emptyList(),
    formParameters: HttpFields<FormParameter> = HttpFields(),
    cookies: List<HttpCookie> = emptyList(),
    contentType: ContentType? = null,
    certificateChain: List<X509Certificate> = emptyList(),
    accept: List<ContentType> = emptyList(),
    contentLength: Long = -1L,
    attributes: Map<*, *> = emptyMap<Any, Any>(),
): HttpServerContext =
    this.process(
        HttpServerRequest(
            method,
            protocol,
            host,
            port,
            path,
            queryParameters,
            headers,
            body,
            parts,
            formParameters,
            cookies,
            contentType,
            certificateChain,
            accept,
            contentLength,
        ),
        attributes,
    )

// TODO Create PathBuilder to leave outside WS. ServerBuilder would use PathBuilder and WsBuilder
fun path(pattern: String = "", block: ServerBuilder.() -> Unit): PathHandler {
    val builder = ServerBuilder()
    builder.block()
    return path(pattern, builder.handlers)
}

// TODO Add first filter with error handling and 'bodyToBytes' checks
fun path(contextPath: String = "", handlers: List<ServerHandler>): PathHandler =
    handlers
        .filterIsInstance<HttpHandler>()
        .let {
            if (it.size == 1 && it[0] is PathHandler)
                (it[0] as PathHandler).addPrefix(contextPath) as PathHandler
            else
                PathHandler(contextPath, it)
        }

fun on(
    predicate: HttpServerPredicate = HttpServerPredicate(),
    callback: HttpCallback
): OnHandler =
    OnHandler(predicate, callback)

fun on(
    methods: Set<HttpMethod> = emptySet(),
    pattern: String = "",
    exception: KClass<out Exception>? = null,
    status: HttpStatus? = null,
    callback: HttpCallback,
): OnHandler =
    OnHandler(methods, pattern, exception, status, callback)

fun on(method: HttpMethod, pattern: String = "", callback: HttpCallback): OnHandler =
    OnHandler(method, pattern, callback)

fun on(pattern: String, callback: HttpCallback): OnHandler =
    OnHandler(pattern, callback)

fun filter(
    predicate: HttpServerPredicate = HttpServerPredicate(),
    callback: HttpCallback
): FilterHandler =
    FilterHandler(predicate, callback)

fun filter(
    methods: Set<HttpMethod> = emptySet(),
    pattern: String = "",
    exception: KClass<out Exception>? = null,
    status: HttpStatus? = null,
    callback: HttpCallback,
): FilterHandler =
    FilterHandler(methods, pattern, exception, status, callback)

fun filter(method: HttpMethod, pattern: String = "", callback: HttpCallback): FilterHandler =
    FilterHandler(method, pattern, callback)

fun filter(pattern: String, callback: HttpCallback): FilterHandler =
    FilterHandler(pattern, callback)

fun after(
    predicate: HttpServerPredicate = HttpServerPredicate(),
    callback: HttpCallback
): AfterHandler =
    AfterHandler(predicate, callback)

fun after(
    methods: Set<HttpMethod> = emptySet(),
    pattern: String = "",
    exception: KClass<out Exception>? = null,
    status: HttpStatus? = null,
    callback: HttpCallback,
): AfterHandler =
    AfterHandler(methods, pattern, exception, status, callback)

fun after(method: HttpMethod, pattern: String = "", callback: HttpCallback): AfterHandler =
    AfterHandler(method, pattern, callback)

fun after(pattern: String, callback: HttpCallback): AfterHandler =
    AfterHandler(pattern, callback)

fun <T : Exception> exception(
    exception: KClass<T>? = null,
    status: HttpStatus? = null,
    callback: HttpExceptionCallback<T>,
): AfterHandler =
    after(emptySet(), "*", exception, status) {
        callback(this.exception.castException(exception))
    }

inline fun <reified T : Exception> exception(
    status: HttpStatus? = null,
    noinline callback: HttpExceptionCallback<T>,
): AfterHandler =
    exception(T::class, status, callback)

internal fun <T : Exception> Exception?.castException(exception: KClass<T>?) =
    this?.let { exception?.cast(this) } ?: error("Exception 'null' or incorrect type")

fun get(pattern: String = "", callback: HttpCallback): OnHandler =
    on(GET, pattern, callback)

fun head(pattern: String = "", callback: HttpCallback): OnHandler =
    on(HEAD, pattern, callback)

fun post(pattern: String = "", callback: HttpCallback): OnHandler =
    on(POST, pattern, callback)

fun put(pattern: String = "", callback: HttpCallback): OnHandler =
    on(PUT, pattern, callback)

fun delete(pattern: String = "", callback: HttpCallback): OnHandler =
    on(DELETE, pattern, callback)

fun trace(pattern: String = "", callback: HttpCallback): OnHandler =
    on(TRACE, pattern, callback)

fun options(pattern: String = "", callback: HttpCallback): OnHandler =
    on(OPTIONS, pattern, callback)

fun patch(pattern: String = "", callback: HttpCallback): OnHandler =
    on(PATCH, pattern, callback)
