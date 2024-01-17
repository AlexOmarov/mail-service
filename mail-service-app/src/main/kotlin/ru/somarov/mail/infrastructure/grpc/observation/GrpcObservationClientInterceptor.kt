package ru.somarov.mail.infrastructure.grpc.observation

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.ForwardingClientCallListener
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import org.slf4j.LoggerFactory

class GrpcObservationClientInterceptor(private val registry: ObservationRegistry) : ClientInterceptor {
    private val log = LoggerFactory.getLogger(this.javaClass)
    override fun <ReqT : Any, RespT : Any> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions?,
        next: Channel,
    ): ClientCall<ReqT, RespT> {
        return object :
            ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                val observation =
                    registry.currentObservation ?: Observation.createNotStarted("GrpcClientObservation", registry)
                val listenerWithLogging =
                    object : ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                        override fun onMessage(message: RespT) {
                            observation.scoped { super.onMessage(message) }
                        }

                        override fun onClose(status: Status?, trailers: Metadata?) {
                            observation.scoped {
                                super.onClose(status, trailers)
                            }
                        }
                    }

                super.start(listenerWithLogging, headers)
            }
        }
    }
}
