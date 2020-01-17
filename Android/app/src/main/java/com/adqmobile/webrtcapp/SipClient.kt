package com.adqmobile.webrtcapp

import android.content.Context
import android.net.sip.SipManager
import android.net.sip.SipProfile
import android.net.sip.SipRegistrationListener

class SipClient(val context: Context, val userName: String, val password: String):
    SipRegistrationListener {

    val domain = "10.127.66.17"
    val sipManager: SipManager by lazy(LazyThreadSafetyMode.NONE) {
        return@lazy SipManager.newInstance(this.context)
    }
    val sipProfile: SipProfile by lazy {
        val builder = SipProfile.Builder(this.userName, this.domain).setPassword(this.password)
        builder.build()
    }

    fun start() {
        if (SipManager.isVoipSupported(this.context)) {
            print("VOIP: Supported!")
            if (SipManager.isApiSupported(this.context)) {
                print("API: Supported!")
                sipManager.open(sipProfile)
                sipManager.register(sipProfile, 60, this)
            } else {
                print("API: NOT Supported!")
            }
        } else {
            print("VOIP: NOT Supported!")
        }
    }

    fun close() {
        try {
            sipManager.close(sipProfile.uriString)
        } catch (e: Exception) {
            print("WalkieTalkieActivity/onDestroy - Failed to close local profile: ${e.message}")
        }
    }

    override fun onRegistering(localProfileUri: String?) {
        print("registering on url: $localProfileUri")
    }

    override fun onRegistrationDone(localProfileUri: String?, expiryTime: Long) {
        print("YAY")
    }

    override fun onRegistrationFailed(
        localProfileUri: String?,
        errorCode: Int,
        errorMessage: String?
    ) {
        print(errorMessage)
    }
}