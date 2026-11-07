package com.monitortv.model

import java.net.InetAddress

data class TvDevice(
    val serviceName: String,
    val host: InetAddress?,
    val port: Int,
    val serviceType: String
) {
    val displayName: String get() = serviceName.ifBlank { host?.hostAddress ?: "Android TV" }
    val endpoint: String get() = "${host?.hostAddress ?: "?"}:$port"
}
