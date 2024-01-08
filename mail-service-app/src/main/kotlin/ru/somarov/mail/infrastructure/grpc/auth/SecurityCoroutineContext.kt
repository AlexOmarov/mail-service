package ru.somarov.mail.infrastructure.grpc.auth

import io.micrometer.context.ContextRegistry
import io.micrometer.context.ContextSnapshot
import io.micrometer.context.ContextSnapshotFactory
import kotlinx.coroutines.ThreadContextElement
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.coroutines.CoroutineContext

class SecurityCoroutineContext : ThreadContextElement<ContextSnapshot.Scope> {
    private val contextSnapshot: ContextSnapshot
    private val registry = ContextRegistry.getInstance()
    private val securityContext = SecurityContextHolder.getContext()
    override val key: CoroutineContext.Key<SecurityCoroutineContext> =
        object : CoroutineContext.Key<SecurityCoroutineContext> {}

    init {
        this.contextSnapshot = ContextSnapshotFactory.builder()
            .captureKeyPredicate { KEY == it }
            .contextRegistry(registry)
            .build()
            .captureAll()
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: ContextSnapshot.Scope) {
        SecurityContextHolder.setContext(securityContext)
    }

    override fun updateThreadContext(context: CoroutineContext): ContextSnapshot.Scope {
        return this.contextSnapshot.setThreadLocals { KEY == it }
    }

    companion object {
        const val KEY = "security.context"
    }
}
