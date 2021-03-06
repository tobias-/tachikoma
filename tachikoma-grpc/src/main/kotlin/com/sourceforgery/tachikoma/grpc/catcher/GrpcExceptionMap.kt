package com.sourceforgery.tachikoma.grpc.catcher

import com.sourceforgery.tachikoma.config.DebugConfig
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.glassfish.hk2.api.IterableProvider
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class GrpcExceptionMap
@Inject
private constructor(
        private val catchers: IterableProvider<GrpcExceptionCatcher<Throwable>>,
        private val debugConfig: DebugConfig
) {
    private val map = ConcurrentHashMap<Class<Throwable>, GrpcExceptionCatcher<Throwable>>()

    private val defaultCatcher = object : GrpcExceptionCatcher<Throwable>(debugConfig, Throwable::class.java) {
        override fun logError(t: Throwable) {
            logger.warn("Exception in gRPC", t)
        }

        override fun status(t: Throwable) = Status.fromThrowable(t).withDescription(stackToString(t).substring(0, 6000))
    }

    fun findCatcher(key: Throwable): GrpcExceptionCatcher<Throwable> {
        @Suppress("UNCHECKED_CAST")
        val clazz = key::class.java as Class<Throwable>
        return map.computeIfAbsent(clazz, { findClass(clazz) })
    }

    private fun getGenerics(catcher: GrpcExceptionCatcher<*>): Type {
        val genericSuperclass = catcher.javaClass.genericSuperclass!!
        return (genericSuperclass as ParameterizedType).actualTypeArguments[0]
    }

    private fun findClass(key: Class<Throwable>): GrpcExceptionCatcher<Throwable> {
        var clazz: Class<*> = key
        while (clazz != Object::class.java) {
            catchers.firstOrNull { getGenerics(it) == clazz }
                    ?.let {
                        return it
                    }
            clazz = clazz.superclass
        }
        return defaultCatcher
    }

    fun findAndConvert(t: Throwable): StatusRuntimeException =
            findCatcher(t).toException(t)
}
