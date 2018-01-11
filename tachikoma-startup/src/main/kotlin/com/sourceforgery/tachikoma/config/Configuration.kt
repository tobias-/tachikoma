package com.sourceforgery.tachikoma.config

import com.sourceforgery.tachikoma.mq.MqConfig
import com.sourceforgery.tachikoma.tracking.TrackingConfig
import java.net.URI
import java.nio.charset.StandardCharsets

internal class Configuration : DatabaseConfig, TrackingConfig, MqConfig, WebServerConfig, DebugConfig {
    override val databaseEncryptionKey: String = readConfig("DATABASE_ENCRYPTION_KEY", "aigac4eeth4uChosea2ohvazoop3Ootal6Vaethei2ohhibooK")
    override val mqUrl: URI = readConfig("MQ_URL", "amqp://guest:guest@localhost/tachikoma", URI::class.java)
    override val sendDebugData: Boolean = readConfig("SEND_DEBUG_DATA", true)
    override val sqlUrl = readConfig("SQL_URL", "postgres://username:password@localhost:5432/tachikoma", URI::class.java)
    override val timeDatabaseQueries: Boolean = readConfig("TIME_DATABASE_QUERIES", true)
    override val trackingEncryptionKey: String = readConfig("TRACKING_ENCRYPTION_KEY", "peilieK6RoomaiPhainocool6ebezai0ox7qui4p")
    override val webtokenSignKey: ByteArray = readConfig("WEBTOKEN_SIGN_KEY", "really, really poor key").toByteArray(StandardCharsets.UTF_8)
    override val baseUrl: URI = readConfig("BASE_URL", "http://localhost:8070/", URI::class.java)
    // TODO set WIPE_AND_CREATE_DATABASE to false
    override val wipeAndCreateDatabase: Boolean = readConfig("WIPE_AND_CREATE_DATABASE", true)
}