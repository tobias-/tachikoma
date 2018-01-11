package com.sourceforgery.tachikoma.unsubscribe

import com.google.protobuf.ByteString
import com.sourceforgery.tachikoma.common.HmacUtil
import com.sourceforgery.tachikoma.common.randomDelay
import com.sourceforgery.tachikoma.grpc.frontend.tracking.UrlSignedMessage
import com.sourceforgery.tachikoma.grpc.frontend.unsubscribe.UnsubscribeData
import com.sourceforgery.tachikoma.tracking.TrackingConfig
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.inject.Inject

internal class UnsubscribeDecoderImpl
@Inject
private constructor(
        // TODO Rename to EncryptionConfig ?
        val trackingConfig: TrackingConfig
) : UnsubscribeDecoder {

    private val encryptionKey = trackingConfig.trackingEncryptionKey.toByteArray(StandardCharsets.UTF_8)

    override fun decodeUnsubscribeData(unsubscribeData: String): UnsubscribeData {
        val decoded = Base64.getUrlDecoder().decode(unsubscribeData)!!
        val signedMessage = UrlSignedMessage.parseFrom(decoded)!!
        val sig = signedMessage.signature!!
        if (!sig.toByteArray().contentEquals(HmacUtil.hmacSha1(signedMessage.message.toByteArray(), encryptionKey))) {
            randomDelay(LongRange(100, 250)) {
                throw RuntimeException("Not correct signature")
            }
        }
        return UnsubscribeData.parseFrom(signedMessage.message)!!
    }

    override fun createUrl(unsubscribeData: UnsubscribeData): String {
        val parcelled = unsubscribeData.toByteArray()!!
        val signature = HmacUtil.hmacSha1(parcelled, encryptionKey)
        val signedMessage = UrlSignedMessage.newBuilder()
                .setMessage(ByteString.copyFrom(parcelled))
                .setSignature(ByteString.copyFrom(signature))
                .build()
        return Base64.getUrlEncoder().encodeToString(signedMessage.toByteArray())!!
    }
}
