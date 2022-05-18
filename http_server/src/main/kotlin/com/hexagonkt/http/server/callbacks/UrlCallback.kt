package com.hexagonkt.http.server.callbacks

import com.hexagonkt.core.ResourceNotFoundException
import com.hexagonkt.core.logging.Logger
import com.hexagonkt.core.media.mediaTypeOfOrNull
import com.hexagonkt.core.require
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.http.server.handlers.HttpServerContext
import java.net.URL

class UrlCallback(private val url: URL) : (HttpServerContext) -> HttpServerContext {
    private val logger: Logger = Logger(UrlCallback::class)

    override fun invoke(context: HttpServerContext): HttpServerContext {
        val requestPath = when (context.pathParameters.size) {
            0 -> ""
            1 -> context.pathParameters.require("0")
            else -> error("URL loading require a single path parameter or none")
        }

        check(!requestPath.contains("..")) { "Requested path cannot contain '..': $requestPath" }
        logger.debug { "Resolving resource: $requestPath" }

        return try {
            if (requestPath.endsWith("/"))
                throw ResourceNotFoundException("$requestPath not found (folder)")

            val url = when {
                requestPath.isEmpty() -> url.toString()
                url.toString() == "classpath:" -> "$url$requestPath"
                else -> "$url/$requestPath"
            }

            val resource = URL(url)
            val bytes = resource.readBytes()
            val mediaType = mediaTypeOfOrNull(resource)
            context.ok(bytes, contentType = mediaType?.let { ContentType(it) })
        }
        catch (e: ResourceNotFoundException) {
            context.notFound(e.message ?: "")
        }
    }
}
