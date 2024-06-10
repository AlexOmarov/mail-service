package ru.somarov.mail.infrastructure.logging

import ch.qos.logback.classic.pattern.CallerDataConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.AppenderBase
import org.komamitsu.fluency.EventTime
import org.komamitsu.fluency.Fluency
import org.komamitsu.fluency.fluentd.FluencyBuilderForFluentd

class FluentdAppender<E : ILoggingEvent> : AppenderBase<E>() {

    private var logger: Fluency? = null

    private var host: String? = null
    private var port: Int = 0
    private var tag: String? = null
    private val callerDataConverter = CallerDataConverter()

    override fun start() {
        super.start()
        val builder = FluencyBuilderForFluentd()
        logger = builder.build(host, port)
    }

    @Suppress("TooGenericExceptionCaught") // We had to catch anything to flush buffer
    override fun append(eventObject: E) {
        val data = mutableMapOf<String, Any?>()

        if (eventObject.level.levelStr == "ERROR") {
            data["throwable"] = ThrowableProxyUtil.asString(eventObject.throwableProxy)
        }

        eventObject.mdcPropertyMap["traceId"]?.let { data["X-ParentMsg-Id"] = it }
        eventObject.mdcPropertyMap["traceId"]?.let { data["X-Process-Id"] = it }
        eventObject.mdcPropertyMap["spanId"]?.let { data["X-Call-Id"] = it }

        data["message"] = eventObject.formattedMessage
        data["level"] = eventObject.level.toString()
        data["logLevel.levelStr"] = eventObject.level.toString()
        data["loggerName"] = eventObject.loggerName
        data["threadName"] = eventObject.threadName
        data["timestamp"] = eventObject.timeStamp
        data["caller"] = callerDataConverter.convert(eventObject)

        try {
            logger?.emit(tag ?: "default", EventTime.fromEpochMilli(eventObject.timeStamp), data)
            logger?.flush()
        } catch (e: Throwable) {
            addWarn("Fluency throws the error and the message has been omitted: $data", e)
        } finally {
            logger?.flush()
        }
    }

    override fun stop() {
        super.stop()
        logger?.close()
    }

    // Setters for Fluentd parameters for logback
    fun setHost(host: String) {
        this.host = host
    }

    fun setPort(port: Int) {
        this.port = port
    }

    fun setTag(tag: String) {
        this.tag = tag
    }
}
