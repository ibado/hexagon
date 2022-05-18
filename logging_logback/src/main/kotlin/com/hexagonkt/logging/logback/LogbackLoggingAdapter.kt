package com.hexagonkt.logging.logback

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.hexagonkt.core.logging.LoggerPort
import com.hexagonkt.core.logging.LoggingLevel
import com.hexagonkt.core.logging.LoggingLevel.DEBUG
import com.hexagonkt.core.logging.LoggingLevel.ERROR
import com.hexagonkt.core.logging.LoggingLevel.INFO
import com.hexagonkt.core.logging.LoggingLevel.TRACE
import com.hexagonkt.core.logging.LoggingLevel.WARN
import com.hexagonkt.core.logging.LoggingLevel.OFF
import com.hexagonkt.core.logging.LoggingPort
import com.hexagonkt.core.logging.jul.JulLoggingAdapter
import org.slf4j.LoggerFactory

class LogbackLoggingAdapter : LoggingPort {

    private val julLoggingAdapter = JulLoggingAdapter()

    override fun createLogger(name: String): LoggerPort =
        julLoggingAdapter.createLogger(name)

    override fun setLoggerLevel(name: String, level: LoggingLevel) {
        val loggerName = name.ifEmpty { Logger.ROOT_LOGGER_NAME }
        (LoggerFactory.getLogger(loggerName) as Logger).level = mapLevel(level)
    }

    override fun isLoggerLevelEnabled(name: String, level: LoggingLevel): Boolean {
        val loggerName = name.ifEmpty { Logger.ROOT_LOGGER_NAME }
        return (LoggerFactory.getLogger(loggerName) as Logger).isEnabledFor(mapLevel(level))
    }

    private fun mapLevel(level: LoggingLevel): Level = when (level) {
        TRACE -> Level.TRACE
        DEBUG -> Level.DEBUG
        INFO -> Level.INFO
        WARN -> Level.WARN
        ERROR -> Level.ERROR
        OFF -> Level.OFF
    }
}
