package ru.somarov.mail.infrastructure.grpc.observation

import io.grpc.ForwardingServerCallListener
import io.grpc.ServerCall
import io.micrometer.core.instrument.binder.grpc.GrpcObservationDocumentation.GrpcServerEvents
import io.micrometer.observation.Observation

internal class ObservationGrpcServerCallListener<RespT>(
    delegate: ServerCall.Listener<RespT>?,
    private val observation: Observation
) : ForwardingServerCallListener.SimpleForwardingServerCallListener<RespT>(delegate) {
    override fun onMessage(message: RespT) {
        observation.event(GrpcServerEvents.MESSAGE_RECEIVED)
        observation.openScope().use { super.onMessage(message) }
    }

    override fun onHalfClose() {
        observation.openScope().use { super.onHalfClose() }
    }

    override fun onCancel() {
        try {
            observation.openScope().use { super.onCancel() }
        } finally {
            observation.stop()
        }
    }

    override fun onComplete() {
        try {
            observation.openScope().use { super.onComplete() }
        } finally {
            observation.stop()
        }
    }

    override fun onReady() {
        observation.openScope().use { super.onReady() }
    }
}
