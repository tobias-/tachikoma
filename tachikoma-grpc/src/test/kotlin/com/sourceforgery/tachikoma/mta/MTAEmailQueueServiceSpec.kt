package com.sourceforgery.tachikoma.mta

import com.sourceforgery.tachikoma.DatabaseBinder
import com.sourceforgery.tachikoma.Hk2TestBinder
import com.sourceforgery.tachikoma.auth.AuthenticationMock
import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.exceptions.InvalidOrInsufficientCredentialsException
import com.sourceforgery.tachikoma.grpc.NullStreamObserver
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.mq.MQSequenceFactoryMock
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.hk2.utilities.ServiceLocatorUtilities
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith

@RunWith(JUnitPlatform::class)
class MTAEmailQueueServiceSpec : Spek({
    lateinit var serviceLocator: ServiceLocator
    lateinit var mtaEmailQueueService: MTAEmailQueueService
    lateinit var mqSequenceFactoryMock: MQSequenceFactoryMock
    lateinit var authentication: AuthenticationMock
    beforeEachTest {
        serviceLocator = ServiceLocatorUtilities.bind(Hk2TestBinder(), DatabaseBinder())!!
        mtaEmailQueueService = serviceLocator.get()
        mqSequenceFactoryMock = serviceLocator.get()
        authentication = serviceLocator.get()
    }
    afterEachTest { serviceLocator.shutdown() }

    describe("MTA queue auth", {
        it("with invalid auth", {
            authentication.invalidAuth()
            assertFailsWith(InvalidOrInsufficientCredentialsException::class, {
                mtaEmailQueueService.getEmails(NullStreamObserver())
            })
        })

        it("with frontend auth", {
            authentication.from(AuthenticationRole.FRONTEND, MailDomain("example.com"))
            assertFailsWith(InvalidOrInsufficientCredentialsException::class, {
                mtaEmailQueueService.getEmails(NullStreamObserver())
            })
        })

        it("with frontendadmin auth", {
            authentication.from(AuthenticationRole.FRONTEND_ADMIN, MailDomain("example.com"))
            assertFailsWith(InvalidOrInsufficientCredentialsException::class, {
                mtaEmailQueueService.getEmails(NullStreamObserver())
            })
        })
    })
})

//fun ServiceLocator.bindHack(bound: Any) {
//    ServiceLocatorUtilities.bind(this, object : AbstractBinder() {
//        override fun configure() {
//            bind(bound)
//        }
//    })
//}

inline fun <reified T : Any> ServiceLocator.get(): T = this.getService(T::class.java)

