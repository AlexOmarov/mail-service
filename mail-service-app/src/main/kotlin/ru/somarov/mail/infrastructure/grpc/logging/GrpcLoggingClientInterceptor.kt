package ru.somarov.mail.infrastructure.grpc.logging

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.ForwardingClientCallListener
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import org.slf4j.LoggerFactory
import ru.somarov.mail.infrastructure.grpc.logging.GrpcLoggingUtil.formLogFromMessage

class GrpcLoggingClientInterceptor : ClientInterceptor {
    private val log = LoggerFactory.getLogger(GrpcLoggingClientInterceptor::class.java)
    override fun <ReqT : Any, RespT : Any> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions?,
        next: Channel,
    ): ClientCall<ReqT, RespT> {
        return object :
            ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            private lateinit var capturedRequestHeaders: Metadata
            override fun sendMessage(message: ReqT) {
                log.info(
                    "Outgoing grpc request <- ${method.fullMethodName}: " +
                        "headers: $capturedRequestHeaders, message=${formLogFromMessage(message)}"
                )
                super.sendMessage(message)
            }

            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                capturedRequestHeaders = headers
                var capturedResponseHeaders = headers
                val listenerWithLogging =
                    object : ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                        override fun onHeaders(headers: Metadata) {
                            capturedResponseHeaders = headers
                            super.onHeaders(headers)
                        }

                        override fun onMessage(message: RespT) {
                            log.info(
                                "Incoming grpc response -> ${method.fullMethodName}: " +
                                    "headers: $capturedResponseHeaders, message=${formLogFromMessage(message)}"
                            )
                            super.onMessage(message)
                        }
                    }

                super.start(listenerWithLogging, headers)
            }
        }
    }
}
