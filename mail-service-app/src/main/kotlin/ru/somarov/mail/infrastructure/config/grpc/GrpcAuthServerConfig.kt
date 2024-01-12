package ru.somarov.mail.infrastructure.config.grpc

import net.devh.boot.grpc.common.util.InterceptorOrder
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import ru.somarov.mail.infrastructure.grpc.auth.GrpcAuthPropagationServerInterceptor

@Configuration
@ConditionalOnClass(value = [Configuration::class, GrpcGlobalServerInterceptor::class])
private class GrpcAuthServerConfig {
    @GrpcGlobalServerInterceptor
    @Order(InterceptorOrder.ORDER_SECURITY_AUTHENTICATION + PROPAGATION_INTERCEPTOR_ORDER)
    fun serverAuthPropagationInterceptor(): GrpcAuthPropagationServerInterceptor {
        return GrpcAuthPropagationServerInterceptor()
    }

    companion object {
        private const val PROPAGATION_INTERCEPTOR_ORDER = 5
    }
}
