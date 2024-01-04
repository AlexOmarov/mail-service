package ru.somarov.mail.infrastructure.grpc.observation

import io.grpc.ForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.Status
import io.micrometer.core.instrument.binder.grpc.GrpcObservationDocumentation.GrpcServerEvents
import io.micrometer.core.instrument.binder.grpc.GrpcServerObservationContext
import io.micrometer.observation.Observation

internal class ObservationGrpcServerCall<ReqT, RespT>(
    delegate: ServerCall<ReqT, RespT>?,
    private val observation: Observation
) : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(delegate) {
    override fun sendMessage(message: RespT) {
        observation.event(GrpcServerEvents.MESSAGE_SENT)
        super.sendMessage(message)
    }

    override fun close(status: Status, trailers: Metadata) {
        if (status.cause != null) {
            observation.error(status.cause!!)
        }
        val context = observation.context as GrpcServerObservationContext
        context.setStatusCode(status.code)
        super.close(status, trailers)
    }
}
