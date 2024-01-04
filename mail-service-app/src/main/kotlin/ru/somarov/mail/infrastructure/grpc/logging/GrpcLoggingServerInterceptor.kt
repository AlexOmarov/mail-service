package ru.somarov.mail.infrastructure.grpc.logging

import io.grpc.ForwardingServerCall.SimpleForwardingServerCall
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.somarov.mail.infrastructure.grpc.logging.GrpcLoggingUtil.formLogFromMessage

class GrpcLoggingServerInterceptor : ServerInterceptor {

    private val log: Logger = LoggerFactory.getLogger(GrpcLoggingServerInterceptor::class.java)

    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val listener: ServerCall<ReqT, RespT> = object : SimpleForwardingServerCall<ReqT, RespT>(call) {
            override fun close(status: Status, trailers: Metadata) {
                if (status.isOk) {
                    return super.close(status, trailers)
                }
                log.info(
                    "Outgoing grpc response <- ${call.methodDescriptor.fullMethodName}: metadata=$trailers, " +
                        "message=$status"
                )
                super.close(status, trailers)
            }

            override fun sendMessage(message: RespT) {
                log.info(
                    "Outgoing grpc response <- ${call.methodDescriptor.fullMethodName}: metadata=$headers, " +
                        "message=${formLogFromMessage(message)}"
                )
                super.sendMessage(message)
            }
        }

        return object : SimpleForwardingServerCallListener<ReqT>(next.startCall(listener, headers)) {
            override fun onMessage(message: ReqT) {
                log.info(
                    "Incoming grpc request -> ${call.methodDescriptor.fullMethodName}: metadata=$headers, " +
                        "message=${formLogFromMessage(message)}"
                )
                super.onMessage(message)
            }
        }
    }
}
