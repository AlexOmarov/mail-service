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

/**
 * This Interface is experimental and may lead to bugs or memory leaks. For now it is in progress
 * */
interface RsocketPayloadDeserializer {

    fun getDeserializedPayload(payload: Payload): Pair<Any, List<String>> {
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

    companion object {
        val codec = HessianCodecSupport()
        val encoding: Charset = Charset.forName("UTF8")
    }
}
