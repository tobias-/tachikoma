package com.sourceforgery.tachikoma.webserver.hk2

import com.linecorp.armeria.common.RequestContext
import com.sourceforgery.tachikoma.hk2.SettableReference
import org.glassfish.hk2.api.Factory
import javax.inject.Inject

class ServiceRequestContextFactory
@Inject
private constructor(
        private val serviceRequestContext: SettableReference<RequestContext>
) : Factory<RequestContext> {

    override fun provide(): RequestContext = serviceRequestContext.value!!

    override fun dispose(instance: RequestContext?) {
    }
}