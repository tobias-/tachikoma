package com.sourceforgery.tachikoma.grpc.frontend

import com.sourceforgery.tachikoma.grpc.frontend.auth.WebTokenAuthData
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailRecipient
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId

fun com.sourceforgery.tachikoma.common.Email.toGrpcInternal() =
        Email.newBuilder().setEmail(address).build()

fun Email.toEmail() =
        com.sourceforgery.tachikoma.common.Email(email)

fun grpcEmailInternal(emailAddress: String) =
        Email.newBuilder().setEmail(emailAddress).build()!!

fun com.sourceforgery.tachikoma.common.NamedEmail.toGrpcInternal() =
        NamedEmail.newBuilder()
                .setEmail(address.address)
                .setName(name)
                .build()!!

fun NamedEmail.toNamedEmail() =
        com.sourceforgery.tachikoma.common.NamedEmail(com.sourceforgery.tachikoma.common.Email(email), name)

fun EmailId.toGrpcInternal() =
        com.sourceforgery.tachikoma.grpc.frontend.EmailId.newBuilder().setId(emailId).build()!!

fun com.sourceforgery.tachikoma.identifiers.EmailTransactionId.toGrpcInternal() =
        EmailTransactionId.newBuilder().setId(emailTransactionId).build()!!

fun WebTokenAuthData.toAccountId(): AccountId? {
    return if (accountId == 0L) {
        null
    } else {
        AccountId(accountId)
    }
}

fun WebTokenAuthData.toAuthenticationId(): AuthenticationId? {
    return if (userId == 0L) {
        null
    } else {
        AuthenticationId(userId)
    }
}

fun EmailRecipient.toNamedEmail() =
        com.sourceforgery.tachikoma.common.NamedEmail(
                address = namedEmail.email!!,
                name = namedEmail.name!!
        )

fun String.emptyToNull() =
        if (this.isEmpty()) {
            null
        } else {
            this
        }
