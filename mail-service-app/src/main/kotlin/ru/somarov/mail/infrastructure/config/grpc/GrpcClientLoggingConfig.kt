package ru.somarov.mail.infrastructure.config.grpc

import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor
import org.springframework.context.annotation.Configuration
import ru.somarov.mail.infrastructure.grpc.logging.GrpcLoggingClientInterceptor

@Configuration
private class GrpcClientLoggingConfig {
    @GrpcGlobalClientInterceptor
    fun grpcLoggingClientInterceptor(): GrpcLoggingClientInterceptor {
        return GrpcLoggingClientInterceptor()
    }
}
