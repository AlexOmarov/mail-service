package ru.somarov.mail.infrastructure.util

import com.google.protobuf.ByteString
import com.google.protobuf.MessageOrBuilder

object GrpcLoggingUtil {
    fun formLogFromMessage(message: Any): String {
        return if (message is MessageOrBuilder) {
            val logs = message.allFields.entries.map {
                " ${it.key.name}: ${
                    ByteString.copyFromUtf8(it.value.toString()).toStringUtf8()
                }"
            }.joinToString { it }
            return "${message.javaClass.name}($logs)"
        } else {
            message.toString()
        }
    }
}
