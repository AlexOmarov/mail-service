package ru.somarov.mail.infrastructure.config

import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor
import net.devh.boot.grpc.common.util.InterceptorOrder
import net.devh.boot.grpc.server.event.GrpcServerStartedEvent
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import ru.somarov.mail.infrastructure.grpc.logging.GrpcLoggingClientInterceptor
import ru.somarov.mail.infrastructure.grpc.logging.GrpcLoggingServerInterceptor

@Configuration
private class GrpcClientServerLoggingConfig {

    private val log = LoggerFactory.getLogger(GrpcClientServerLoggingConfig::class.java)

    @EventListener
    fun onServerStarted(event: GrpcServerStartedEvent) {
        log.info("gRPC Server started, services: ${event.server.services[0].methods}")
    }

    @GrpcGlobalServerInterceptor
    @Order(InterceptorOrder.ORDER_TRACING_METRICS + LOGGING_INTERCEPTOR_ORDER)
    fun grpcLoggingServerInterceptor(): GrpcLoggingServerInterceptor {
        return GrpcLoggingServerInterceptor()
    }

    @GrpcGlobalClientInterceptor
    fun grpcLoggingClientInterceptor(): GrpcLoggingClientInterceptor {
        return GrpcLoggingClientInterceptor()
    }

    companion object {
        private const val LOGGING_INTERCEPTOR_ORDER = 4
    }
}
