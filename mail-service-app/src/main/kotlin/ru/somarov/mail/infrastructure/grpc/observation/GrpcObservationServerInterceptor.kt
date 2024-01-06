package ru.somarov.mail.infrastructure.grpc.observation

import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.micrometer.core.instrument.binder.grpc.DefaultGrpcServerObservationConvention
import io.micrometer.core.instrument.binder.grpc.GrpcObservationDocumentation
import io.micrometer.core.instrument.binder.grpc.GrpcServerObservationContext
import io.micrometer.core.instrument.binder.grpc.GrpcServerObservationConvention
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier

class GrpcObservationServerInterceptor(private val registry: ObservationRegistry) : ServerInterceptor {

    private val defaultConvention: GrpcServerObservationConvention = DefaultGrpcServerObservationConvention()

    private val keyCache: ConcurrentHashMap<String, Metadata.Key<String>> = ConcurrentHashMap()

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val observation =
            GrpcObservationDocumentation.SERVER.observation(
                /* customConvention = */ null,
                /* defaultConvention = */ defaultConvention,
                /* contextSupplier = */ createContextSupplier(call, headers),
                /* registry = */ this.registry
            ).start()

        if (observation.isNoop) {
            // do not instrument anymore
            return next.startCall(call, headers)
        }

        return startObservedCall(observation, call, headers, next)
    }

    private fun <ReqT : Any?, RespT : Any?> createContextSupplier(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
    ): Supplier<GrpcServerObservationContext> {
        return Supplier {
            val context = createGrpcServerObservationContext()
            context.carrier = headers
            val methodDescriptor = call.methodDescriptor
            val serviceName = methodDescriptor.serviceName
            val methodName = methodDescriptor.bareMethodName
            val fullMethodName = methodDescriptor.fullMethodName
            val methodType = methodDescriptor.type
            if (serviceName != null) {
                context.serviceName = serviceName
            }
            if (methodName != null) {
                context.methodName = methodName
            }
            context.fullMethodName = fullMethodName
            context.methodType = methodType
            context.authority = call.authority
            context
        }
    }

    private fun createGrpcServerObservationContext(): GrpcServerObservationContext {
        return GrpcServerObservationContext { carrier, keyName ->
            val key = keyCache.computeIfAbsent(keyName) { Metadata.Key.of(keyName, Metadata.ASCII_STRING_MARSHALLER) }
            carrier[key]
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun <ReqT : Any?, RespT : Any?> startObservedCall(
        observation: Observation,
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ObservationGrpcServerCallListener<ReqT> {
        val serverCall = ObservationGrpcServerCall(call, observation)
        return try {
            observation.scoped(
                Supplier {
                    val result = next.startCall(serverCall, headers)
                    ObservationGrpcServerCallListener(result, observation)
                }
            )
        } catch (ex: Exception) {
            observation.error(ex).stop()
            throw ex
        }
    }
}
