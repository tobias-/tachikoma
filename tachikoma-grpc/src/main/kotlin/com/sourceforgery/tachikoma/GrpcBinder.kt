package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.hk2.HK2RequestContext
import com.sourceforgery.tachikoma.hk2.RequestScoped
import com.sourceforgery.tachikoma.mta.MTADeliveryService
import com.sourceforgery.tachikoma.mta.MTAEmailQueueService
import com.sourceforgery.tachikoma.tracking.DeliveryNotificationService
import com.sourceforgery.tachikoma.tracking.TrackingDecoder
import com.sourceforgery.tachikoma.tracking.TrackingDecoderImpl
import io.grpc.BindableService
import org.glassfish.hk2.api.Context
import org.glassfish.hk2.api.TypeLiteral
import org.glassfish.hk2.utilities.binding.AbstractBinder
import javax.inject.Singleton

class GrpcBinder : AbstractBinder() {
    override fun configure() {
        bindAsContract(MTADeliveryService::class.java)
                .to(BindableService::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(MTAEmailQueueService::class.java)
                .to(BindableService::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(DeliveryNotificationService::class.java)
                .to(BindableService::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(TrackingDecoderImpl::class.java)
                .to(TrackingDecoder::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(HK2RequestContext::class.java)
                .to(RequestScoped::class.java)
                .to(object : TypeLiteral<Context<RequestScoped>>() {}.type)
                .`in`(Singleton::class.java)
    }
}