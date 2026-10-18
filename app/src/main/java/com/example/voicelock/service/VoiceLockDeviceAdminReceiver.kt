package com.example.voicelock.service

import android.app.admin.DeviceAdminReceiver

/**
 * F1: device admin with the single `force-lock` policy (res/xml/device_admin.xml), only used to call
 * DevicePolicyManager.lockNow() after the owner's lock command is verified. It never wipes data or
 * touches the screen-lock password. The user must turn it off in Android settings before uninstalling.
 */
class VoiceLockDeviceAdminReceiver : DeviceAdminReceiver()
