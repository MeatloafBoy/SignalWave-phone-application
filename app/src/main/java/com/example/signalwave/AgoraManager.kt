package com.example.signalwave

import android.content.Context
import android.util.Log
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig

object AgoraManager {
    private const val AppId = "b41ac320dbed42809a295de63f2a8a40"

    const val Token =
        "007eJxTYIjNOTlP3/lD637ZzKMKFyZuzbefP/Gxotah7YuEJRO13yQpMCSZGCYmGxsZpCSlppgYWRhYJhpZmqakmhmnGSVaJJoYKB+Xz2gIZGTQ+1TJwsgAgSA+F0NIanGJQlh+ZnIqAwMAC7ohGg=="
    const val ChannelName = "Test Voice"

    var rtcEngine: RtcEngine? = null

    val rtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.d("AgoraEventHandler", "Joined channel $channel with uid $uid")
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d("AgoraEventHandler", "User $uid joined the channel")
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d("AgoraEventHandler", "User $uid left the channel, reason: $reason")
        }

        override fun onError(err: Int) {
            Log.e("AgoraEventHandler", "Agora Error: $err")
        }
    }

    fun initialize(context: Context) {
        if (rtcEngine == null) {

            try {
                val config = RtcEngineConfig()
                config.mAppId = AppId
                config.mEventHandler = rtcEngineEventHandler
                rtcEngine = RtcEngine.create(config)
                Log.d("AgoraManager", "Agora RTC Engine initialized successfully")

                rtcEngine?.enableAudio()

            } catch (e: Exception) {
                Log.e("AgoraManager", "Error initializing Agora RTC Engine", e)
                rtcEngine = null
            }
        } else
        {
            Log.d("AgoraManager", "Agora RTC Engine already initialized")
        }
    }

    fun destroy() {
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
        Log.d("AgoraManager", "Agora RTC Engine destroyed")
    }
}