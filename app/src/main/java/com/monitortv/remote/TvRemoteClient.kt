package com.monitortv.remote

import com.monitortv.model.TvDevice

interface TvRemoteClient {
    suspend fun pair(device: TvDevice, pairingCode: String): PairingResult
    suspend fun connect(device: TvDevice): ConnectionResult
    suspend fun adjustVolume(direction: VolumeDirection): CommandResult
    suspend fun setMuted(muted: Boolean): CommandResult
    suspend fun disconnect()
}
enum class VolumeDirection { UP, DOWN }
sealed interface PairingResult {
    data object Success : PairingResult
    data class Failure(val reason: String) : PairingResult
}
sealed interface ConnectionResult {
    data object Connected : ConnectionResult
    data class Failure(val reason: String) : ConnectionResult
}
sealed interface CommandResult {
    data object Sent : CommandResult
    data class Failure(val reason: String) : CommandResult
}
