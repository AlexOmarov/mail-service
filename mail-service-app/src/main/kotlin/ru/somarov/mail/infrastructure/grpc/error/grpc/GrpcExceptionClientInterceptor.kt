package com.denumhub.error.grpc

import com.denumhub.error.exception.technical.TechnicalException
import com.denumhub.error.exception.technical.TechnicalExceptionRegistryMember.BASE_TECHNICAL_EXCEPTION
import com.denumhub.error.grpc.GrpcExceptionClientInterceptor.Companion.EXCEPTION_INTERCEPTOR_ORDER
import com.denumhub.error.grpc.extend.toTechnicalException
import com.fasterxml.jackson.databind.ObjectMapper
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientCall.Listener
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor
import net.devh.boot.grpc.common.util.InterceptorOrder
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.info.BuildProperties
import org.springframework.core.annotation.Order
import java.time.OffsetDateTime

@ConditionalOnClass(GrpcGlobalClientInterceptor::class)
@GrpcGlobalClientInterceptor
@Order(InterceptorOrder.ORDER_TRACING_METRICS + EXCEPTION_INTERCEPTOR_ORDER)
class GrpcExceptionClientInterceptor(private val mapper: ObjectMapper, private val buildProps: BuildProperties) :
    ClientInterceptor {

    private val log = LoggerFactory.getLogger(this.javaClass)

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions?,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        return object : SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                super.start(getModifiedListener(responseListener), headers)
            }
        }
    }

    private fun <RespT> getModifiedListener(listener: Listener<RespT>): SimpleForwardingClientCallListener<RespT> {
        return object : SimpleForwardingClientCallListener<RespT>(listener) {
            override fun onClose(status: Status, trailers: Metadata) {
                if (status.isOk) {
                    super.onClose(status, trailers)
                } else {
                    val ex = extractException(status, trailers)
                    log.error(
                        "Incoming grpc response -> Got not successful response from grpc service, " +
                            "status=$status, metadata: $trailers, exception: $ex"
                    )
                    super.onClose(status.withCause(ex), trailers)
                }
            }
        }
    }

    private fun extractException(status: Status, trailers: Metadata): TechnicalException {
        val statusException = status.asException(trailers)
        val extractedException = statusException.toTechnicalException(mapper, log)
        return (extractedException ?: TechnicalException(
            systemMessage = BASE_TECHNICAL_EXCEPTION.message,
            message = status.asException().message + ". Cause: ${status.cause?.message}",
            code = BASE_TECHNICAL_EXCEPTION.code,
            datetime = OffsetDateTime.now(),
            serviceName = buildProps.name, // TODO: instead of own name set name of side service
            cause = null,
            uniqueTrace = TechnicalException.generateTrace()
        )).also { it.stackTrace = arrayOf() }
    }

    companion object {
        const val EXCEPTION_INTERCEPTOR_ORDER = 100
    }
}
