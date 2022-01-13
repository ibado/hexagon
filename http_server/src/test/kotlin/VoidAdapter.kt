package com.hexagonkt.http.server

import com.hexagonkt.http.model.HttpProtocol
import com.hexagonkt.http.model.HttpProtocol.H2C
import com.hexagonkt.http.server.HttpServerFeature.ASYNC
import com.hexagonkt.http.server.HttpServerFeature.ZIP

internal object VoidAdapter : HttpServerPort {
    private var started = false

    override fun runtimePort() = 12345
    override fun started() = started
    override fun startUp(server: HttpServer) { started = true }
    override fun shutDown() { started = false }
    override fun supportedProtocols(): Set<HttpProtocol> = HttpProtocol.values().toSet() - H2C
    override fun supportedFeatures(): Set<HttpServerFeature> = setOf(ZIP, ASYNC)
    override fun supportedOptions(): Set<String> = setOf("option1", "option2")
}
