package ru.somarov.mail.infrastructure.hessian

import com.caucho.hessian.io.HessianSerializerInput
import com.caucho.hessian.io.HessianSerializerOutput
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.util.MimeType
import java.io.ByteArrayInputStream
import java.io.IOException

open class HessianCodecSupport {

    fun <T> decode(clazz: Class<T>, dataBuffer: DataBuffer): T {
        val inpStr = dataBuffer.asInputStream()
        val message = readMessage(clazz, HessianSerializerInput(inpStr))
        inpStr.close()
        DataBufferUtils.release(dataBuffer)
        return message
    }

    fun <T> decode(clazz: Class<T>, inputMessage: HttpInputMessage): T {
        val inpStr = inputMessage.body
        val message = readMessage(clazz, HessianSerializerInput(inpStr))
        inpStr.close()
        return message
    }

    fun encode(obj: Any, bufferFactory: DataBufferFactory): DataBuffer {
        val dataBuffer = bufferFactory.allocateBuffer(CAPACITY)
        val outStr = dataBuffer.asOutputStream()
        HessianSerializerOutput(outStr).also {
            it.startMessage()
            it.writeObject(obj)
            it.completeMessage()
            it.close()
        }
        outStr.close()
        return dataBuffer
    }

    fun encode(obj: Any, outputMessage: HttpOutputMessage) {
        HessianSerializerOutput(outputMessage.body).also {
            it.startMessage()
            it.writeObject(obj)
            it.completeMessage()
            it.close()
        }
    }

    // Thread-safe
    fun readObject(inp: HessianSerializerInput): Any {
        inp.startMessage()
        val message = inp.readObject()
        inp.completeMessage()
        inp.close()
        return message
    }

    // Thread-safe
    fun isHessian(byteArray: ByteArray): Boolean {
        return try {
            val byteArrayInputStream = ByteArrayInputStream(byteArray)
            val hessianInput = HessianSerializerInput(byteArrayInputStream)
            readObject(hessianInput)
            true
        } catch (e: IOException) {
            false
        }
    }

    private fun <T> readMessage(clazz: Class<T>, inp: HessianSerializerInput): T {
        val message = readObject(inp)
        return clazz.cast(message)
    }

    companion object {
        val HESSIAN_MIME_TYPE = MimeType("application", "x-hessian")
        val HESSIAN_MEDIA_TYPES = mutableListOf(MediaType.asMediaType(HESSIAN_MIME_TYPE))
        val HESSIAN_MIME_TYPES = mutableListOf(HESSIAN_MIME_TYPE)
        const val CAPACITY = 2048
    }
}
