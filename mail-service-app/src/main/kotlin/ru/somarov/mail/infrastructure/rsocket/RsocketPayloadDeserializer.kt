package ru.somarov.mail.infrastructure.rsocket

import com.caucho.hessian.io.HessianSerializerInput
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.rsocket.Payload
import io.rsocket.metadata.CompositeMetadata
import ru.somarov.mail.infrastructure.hessian.HessianCodecSupport
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset

interface RsocketPayloadDeserializer {
    private val encoding: Charset
        get() = Charset.forName("UTF8")

    fun getDeserializedPayload(payload: Payload, codec: HessianCodecSupport): Pair<Any, List<String>> {
        val dataByteArray = getByteArray(payload.data)

        val deserializedData = if (codec.isHessian(dataByteArray)) {
            codec.readObject(HessianSerializerInput(ByteArrayInputStream(dataByteArray)))
        } else {
            payload.dataUtf8
        }
        val deserializedMetadata = CompositeMetadata(Unpooled.wrappedBuffer(payload.metadata), false).map {
            val mimeType = it.mimeType
            val content = it.content
            val result = if (ByteBufUtil.isText(content, encoding)) {
                "Header(mime: $mimeType, content: ${content.toString(encoding)})"
            } else {
                "Header(mime: $mimeType, content: Not text)"
            }
            // Release the ByteBuf when done using it
            content.release()
            result
        }

        return Pair(deserializedData, deserializedMetadata)
    }

    @Suppress("kotlin:S6518") // Here byteArray should be filled with get method
    private fun getByteArray(buffer: ByteBuffer): ByteArray {
        val byteArray = ByteArray(buffer.remaining())
        buffer.get(byteArray)
        buffer.rewind()
        return byteArray
    }
}