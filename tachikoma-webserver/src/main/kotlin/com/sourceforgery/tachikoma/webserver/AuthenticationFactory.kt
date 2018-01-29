package com.sourceforgery.tachikoma.webserver

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.linecorp.armeria.common.HttpHeaders
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.common.HmacUtil.hmacSha1
import com.sourceforgery.tachikoma.config.WebServerConfig
import com.sourceforgery.tachikoma.database.dao.AccountDAO
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.exceptions.InvalidOrInsufficientCredentialsException
import com.sourceforgery.tachikoma.exceptions.NoAuthorizationCredentialsException
import com.sourceforgery.tachikoma.grpc.frontend.auth.AuthRole
import com.sourceforgery.tachikoma.grpc.frontend.auth.WebTokenAuthData
import com.sourceforgery.tachikoma.grpc.frontend.toAccountId
import com.sourceforgery.tachikoma.grpc.frontend.toAuthenticationId
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.logging.logger
import io.netty.util.AsciiString
import org.glassfish.hk2.api.Factory
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AuthenticationFactory
@Inject
private constructor(
        private val httpHeaders: HttpHeaders,
        private val webServerConfig: WebServerConfig,
        private val authenticationDAO: AuthenticationDAO,
        private val accountDAO: AccountDAO
) : Factory<Authentication> {

    private val apiKeyCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(CacheLoader.from<String, AuthenticationImpl?>({ parseApiTokenHeader(it) }))

    override fun provide() =
            parseWebTokenHeader()
                    ?: parseApiTokenHeader()
                    ?: NO_AUTHENTICATION

    private fun parseApiTokenHeader(): AuthenticationImpl? =
            apiKeyCache.getUnchecked(httpHeaders[APITOKEN_HEADER])

    private fun parseApiTokenHeader(header: String?): AuthenticationImpl? =
            header
                    ?.let {
                        MailDomain(it.substringBefore(':')) to it.substringAfter(':')
                    }
                    ?.let { splitAuthString ->
                        // TODO Needs to be handled better. This is slow and hits the database
                        // for every request
                        authenticationDAO.validateApiToken(splitAuthString.second)
                                ?.let { auth ->
                                    if (auth.account.mailDomain == splitAuthString.first) {
                                        auth
                                    } else {
                                        LOGGER.warn { "Incorrect domain(${splitAuthString.first}) for account ${auth.account.id})" }
                                        throw InvalidOrInsufficientCredentialsException("Incorrect domain(${splitAuthString.first}")
                                    }
                                }
                    }
                    ?.let {
                        AuthenticationImpl(
                                // No webtoken should allow backend
                                role = it.role,
                                authenticationId = it.id,
                                accountId = it.account.id,
                                accountDAO = accountDAO
                        )
                    }

    private fun parseWebTokenHeader(): Authentication? {
        val webtokenHeader = httpHeaders[WEBTOKEN_HEADER]
                ?: return null
        val splitToken = webtokenHeader.split(
                delimiters = *charArrayOf('.'),
                limit = 2
        )
        if (splitToken.size != 2) {
            return null
        }
        val payloadSignature = BASE64_DECODER.decode(splitToken[0])!!
        val payload = BASE64_DECODER.decode(splitToken[1])!!
        if (hmacSha1(webServerConfig.webtokenSignKey, payload).contentEquals(payloadSignature)) {
            return null
        }
        val tokenAuthData = WebTokenAuthData.parseFrom(payload)
        val authenticationId = tokenAuthData.toAuthenticationId()
                ?: throw InvalidOrInsufficientCredentialsException()

        val accountId = tokenAuthData.toAccountId()
                ?: throw InvalidOrInsufficientCredentialsException()
        return AuthenticationImpl(
                // No webtoken should allow backend
                role = tokenAuthData.authenticationRole.toAuthenticationRole(),
                authenticationId = authenticationId,
                accountId = accountId,
                accountDAO = accountDAO
        )
    }

    override fun dispose(instance: Authentication) {
    }

    companion object {
        val LOGGER = logger()
        val BASE64_DECODER = Base64.getDecoder()!!
        val NO_AUTHENTICATION = object : Authentication {
            override fun requireFrontend(): AccountId {
                throw NoAuthorizationCredentialsException()
            }

            override fun requireBackend(): AccountId {
                throw NoAuthorizationCredentialsException()
            }

            override fun requireFrontendAdmin(): AccountId {
                throw NoAuthorizationCredentialsException()
            }

            override val mailDomain: MailDomain
                get() = throw NoAuthorizationCredentialsException()

            override val authenticationId: AuthenticationId
                get() = throw NoAuthorizationCredentialsException()

            override val accountId: AccountId
                get() = throw NoAuthorizationCredentialsException()

            override val valid: Boolean = false
        }

        val WEBTOKEN_HEADER = AsciiString("x-webtoken")
        val APITOKEN_HEADER = AsciiString("x-apitoken")
    }
}

internal class AuthenticationImpl(
        override var authenticationId: AuthenticationId,
        override var accountId: AccountId,
        private val accountDAO: AccountDAO,
        private val role: AuthenticationRole
) : Authentication {

    override val mailDomain: MailDomain by lazy {
        accountDAO.getById(accountId).mailDomain
    }

    override fun requireFrontend(): AccountId {
        requireValid()
        if (role != AuthenticationRole.FRONTEND_ADMIN && role != AuthenticationRole.FRONTEND) {
            throw InvalidOrInsufficientCredentialsException()
        }
        return accountId
    }

    override fun requireFrontendAdmin(): AccountId {
        requireValid()
        if (role != AuthenticationRole.FRONTEND_ADMIN) {
            throw InvalidOrInsufficientCredentialsException()
        }
        return accountId
    }

    override fun requireBackend(): AccountId {
        requireValid()
        if (role != AuthenticationRole.BACKEND) {
            throw InvalidOrInsufficientCredentialsException()
        }
        return accountId
    }

    private fun requireValid() {
        if (!valid) {
            throw InvalidOrInsufficientCredentialsException()
        }
    }

    override val valid: Boolean = true
}

fun AuthRole.toAuthenticationRole() =
        when (this) {
            AuthRole.FRONTEND -> AuthenticationRole.FRONTEND
            AuthRole.BACKEND -> AuthenticationRole.BACKEND
            AuthRole.FRONTEND_ADMIN -> AuthenticationRole.FRONTEND_ADMIN
            AuthRole.UNRECOGNIZED -> throw InvalidOrInsufficientCredentialsException("Webtoken is invalid")
        }