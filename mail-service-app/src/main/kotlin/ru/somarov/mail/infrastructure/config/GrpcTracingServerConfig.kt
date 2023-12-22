package ru.somarov.mail.infrastructure.config

import io.micrometer.observation.ObservationRegistry
import net.devh.boot.grpc.common.util.InterceptorOrder
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import ru.somarov.mail.infrastructure.grpc.GrpcObservationServerInterceptor
import ru.somarov.mail.infrastructure.grpc.GrpcPropagationServerInterceptor

@Configuration
@ConditionalOnClass(value = [Configuration::class, GrpcGlobalServerInterceptor::class])
class GrpcTracingServerConfig {
    @GrpcGlobalServerInterceptor
    @Order(InterceptorOrder.ORDER_TRACING_METRICS + PROPAGATION_INTERCEPTOR_ORDER)
    fun serverPropagationInterceptor(observationRegistry: ObservationRegistry): GrpcPropagationServerInterceptor {
        return GrpcPropagationServerInterceptor(observationRegistry)
    }

    @GrpcGlobalServerInterceptor
    @Order(InterceptorOrder.ORDER_TRACING_METRICS + TRACING_INTERCEPTOR_ORDER)
    fun serverTracingInterceptor(observationRegistry: ObservationRegistry): GrpcObservationServerInterceptor {
        return GrpcObservationServerInterceptor(observationRegistry)
    }

    companion object {
        private const val PROPAGATION_INTERCEPTOR_ORDER = 3
        private const val TRACING_INTERCEPTOR_ORDER = 2
    }
}
