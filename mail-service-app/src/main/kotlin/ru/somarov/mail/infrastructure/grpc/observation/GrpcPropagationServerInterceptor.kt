package ru.somarov.mail.infrastructure.grpc.observation

import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.kotlin.CoroutineContextServerInterceptor
import io.micrometer.core.instrument.kotlin.asContextElement
import io.micrometer.observation.ObservationRegistry
import kotlin.coroutines.CoroutineContext

class GrpcPropagationServerInterceptor(private val registry: ObservationRegistry) :
    CoroutineContextServerInterceptor() {
    override fun coroutineContext(call: ServerCall<*, *>, headers: Metadata): CoroutineContext {
        return registry.asContextElement()
    }
}
