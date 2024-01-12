package ru.somarov.mail.infrastructure.config.grpc

import io.grpc.ClientInterceptor
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcClientInterceptor
import io.micrometer.observation.ObservationRegistry
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor
import net.devh.boot.grpc.common.util.InterceptorOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import ru.somarov.mail.infrastructure.grpc.observation.GrpcObservationClientInterceptor

@Configuration
@ConditionalOnClass(value = [Configuration::class, GrpcGlobalClientInterceptor::class])
private class GrpcTracingClientConfig {
    @GrpcGlobalClientInterceptor
    @Order(InterceptorOrder.ORDER_LAST)
    fun tracingClientInterceptor(registry: ObservationRegistry): ClientInterceptor {
        return GrpcObservationClientInterceptor(registry)
    }

    @GrpcGlobalClientInterceptor
    fun tracingPropagationClientInterceptor(registry: ObservationRegistry): ClientInterceptor {
        return ObservationGrpcClientInterceptor(registry)
    }
}
