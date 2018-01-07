package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.NamedEmail
import com.sourceforgery.tachikoma.identifiers.EmailId
import io.ebean.common.BeanList
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "e_email")
// Represents a single email to one recipient
class EmailDBO
constructor(
        @Column
        val recipient: Email,
        @Column
        val recipientName: String,
        @ManyToOne(cascade = [CascadeType.ALL])
        val transaction: EmailSendTransactionDBO,
        @ManyToOne(cascade = [CascadeType.ALL])
        val sentMailMessageBody: SentMailMessageBodyDBO,
        @Column
        var mtaQueueId: String? = null
) : GenericDBO() {
    @OneToMany
    val emailStatusEvents: List<EmailStatusEventDBO> = BeanList()

    constructor(
            recipient: NamedEmail,
            transaction: EmailSendTransactionDBO,
            sentMailMessageBody: SentMailMessageBodyDBO,
            mtaQueueId: String? = null
    ) : this(
            recipient = recipient.address,
            recipientName = recipient.name,
            transaction = transaction,
            sentMailMessageBody = sentMailMessageBody,
            mtaQueueId = mtaQueueId
    )
}

val EmailDBO.id: EmailId
    get() = EmailId(dbId!!)