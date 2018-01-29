package com.sourceforgery.tachikoma.mq

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import javax.annotation.PreDestroy
import javax.inject.Inject

class MQSequenceFactoryMock
@Inject
private constructor(
) : MQSequenceFactory {
    val deliveryNotifications: BlockingQueue<DeliveryNotificationMessage?> = LinkedBlockingQueue()
    val jobs: BlockingQueue<JobMessage?> = LinkedBlockingQueue()
    val outgoingEmails: BlockingQueue<OutgoingEmailMessage?> = LinkedBlockingQueue()
    val incomingEmails: BlockingQueue<IncomingEmailNotificationMessage?> = LinkedBlockingQueue()

    private val executorService: ExecutorService = Executors.newCachedThreadPool()
    override fun listenForDeliveryNotifications(authenticationId: AuthenticationId, callback: (DeliveryNotificationMessage) -> Unit): ListenableFuture<Void> {
        return listenOnQueue(deliveryNotifications, callback)
    }

    private fun <X : Any> listenOnQueue(queue: BlockingQueue<X?>, callback: (X) -> Unit): SettableFuture<Void> {
        val future = SettableFuture.create<Void>()
        executorService.execute {
            generateSequence({
                queue.take()
            }).forEach {
                callback(it)
            }
            future.set(null)
        }
        return future
    }

    override fun listenForJobs(callback: (JobMessage) -> Unit): ListenableFuture<Void> {
        return listenOnQueue(jobs, callback)
    }

    override fun <T> listenOnQueue(messageQueue: MessageQueue<T>, callback: (T) -> Unit): ListenableFuture<Void> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun listenForOutgoingEmails(mailDomain: MailDomain, callback: (OutgoingEmailMessage) -> Unit): ListenableFuture<Void> {
        return listenOnQueue(outgoingEmails, callback)
    }

    override fun listenForIncomingEmails(authenticationId: AuthenticationId, callback: (IncomingEmailNotificationMessage) -> Unit): ListenableFuture<Void> {
        return listenOnQueue(incomingEmails, callback)
    }

    @PreDestroy
    fun shutdown() {
        executorService.shutdownNow()
    }
}