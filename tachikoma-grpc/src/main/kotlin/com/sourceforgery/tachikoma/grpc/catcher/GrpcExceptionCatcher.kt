package com.sourceforgery.tachikoma.grpc.catcher

import com.sourceforgery.tachikoma.config.DebugConfig
import com.sourceforgery.tachikoma.logging.logger
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Locale

abstract class GrpcExceptionCatcher<in T : Throwable>(
        private val debugConfig: DebugConfig,
        clazz: Class<T>
) {
    protected val logger = logger("grpc.exceptions.${clazz.simpleName.toLowerCase(Locale.US)}")

    abstract fun status(t: T): Status

    open fun logError(t: T) {}

    open fun metadata(t: T) = Metadata()

    open fun toException(t: T): StatusRuntimeException = StatusRuntimeException(status(t), metadata(t))

    fun throwIt(t: T): Nothing {
        logError(t)
        throw toException(t)
    }

    protected fun stackToString(e: Throwable): String {
        if (debugConfig.sendDebugData) {
            StringWriter().use {
                e.printStackTrace(PrintWriter(it))
                return it.toString()
            }
        } else {
            return e.message ?: ""
        }
    }
}