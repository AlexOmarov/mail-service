package ru.somarov.mail.infrastructure.grpc.error.grpc

import com.fasterxml.jackson.databind.ObjectMapper
import io.grpc.ForwardingServerCall
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import net.devh.boot.grpc.common.util.InterceptorOrder
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.info.BuildProperties
import org.springframework.core.annotation.Order
import ru.somarov.mail.infrastructure.grpc.error.exception.technical.TechnicalException
import ru.somarov.mail.infrastructure.grpc.error.exception.technical.TechnicalExceptionRegistryMember

private const val INTERCEPTOR_ORDER = 9

@GrpcGlobalServerInterceptor
@Order(InterceptorOrder.ORDER_TRACING_METRICS + INTERCEPTOR_ORDER)
@ConditionalOnClass(value = [GrpcGlobalServerInterceptor::class])
class GrpcExceptionServerInterceptor(
    private val mapper: ObjectMapper,
    private val buildProps: BuildProperties,
    private val handler: GrpcExceptionServerHandler = GrpcExceptionServerHandler(mapper, buildProps)
) : ServerInterceptor {

    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: io.grpc.Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        return next.startCall(ExceptionTranslatingServerCall(handler, call), headers)
    }

    private class ExceptionTranslatingServerCall<ReqT, RespT>(
        private val handler: GrpcExceptionServerHandler,
        delegate: ServerCall<ReqT, RespT>,
    ) : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(delegate) {

        private val log = LoggerFactory.getLogger(GrpcExceptionServerInterceptor::class.java)

        override fun close(status: Status, trailers: io.grpc.Metadata) {
            if (status.isOk) {
                return super.close(status, trailers)
            }
            log.info("Got exception while processing grpc request: ${status.cause}")
            val ex = when (status.cause?.javaClass) {
                TechnicalException::class.java -> handler.createStatusRuntimeException(
                    status.cause as TechnicalException
                )

                null -> handler.createStatusRuntimeException()
                else -> handler.createStatusRuntimeException(
                    status.cause as Exception,
                    TechnicalExceptionRegistryMember.getByException(status.cause!!.javaClass)
                )
            }

            super.close(Status.fromThrowable(ex), ex.trailers)
        }
    }
}
