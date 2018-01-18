package com.sourceforgery.tachikoma.mta

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class MTAEmailQueueServiceGrpcImpl
@Inject
private constructor(
        private val mtaEmailQueueService: MTAEmailQueueService,
        private val grpcExceptionMap: GrpcExceptionMap
) : MTAEmailQueueGrpc.MTAEmailQueueImplBase() {
    override fun getEmails(responseObserver: StreamObserver<EmailMessage>): StreamObserver<MTAQueuedNotification>? {
        return try {
            mtaEmailQueueService.getEmails(responseObserver)
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvert(e))
            null
        }
    }

    override fun incomingEmail(request: IncomingEmailMessage, responseObserver: StreamObserver<Empty>) {
        return try {
            mtaEmailQueueService.incomingEmail(request)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvert(e))
        }
    }
}