package ru.somarov.mail.infrastructure.grpc.auth

import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.kotlin.CoroutineContextServerInterceptor
import kotlin.coroutines.CoroutineContext

class GrpcAuthPropagationServerInterceptor : CoroutineContextServerInterceptor() {
    override fun coroutineContext(call: ServerCall<*, *>, headers: Metadata): CoroutineContext {
        return SecurityCoroutineContext()
    }
}
