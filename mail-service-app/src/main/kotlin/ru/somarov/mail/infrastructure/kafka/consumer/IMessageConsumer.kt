package ru.somarov.mail.infrastructure.kafka.consumer

import reactor.kafka.receiver.KafkaReceiver

interface IMessageConsumer<T> {
    suspend fun handle(message: T, metadata: MessageMetadata): MessageConsumptionResult
    fun getReceiver(): KafkaReceiver<String, T?>
    fun enabled(): Boolean
    fun getName(): String
    fun getDelaySeconds(): Long?
    fun getExecutionStrategy(): ExecutionStrategy = ExecutionStrategy.PARALLEL

    enum class ExecutionStrategy {
        PARALLEL, SEQUENTIAL
    }
}
