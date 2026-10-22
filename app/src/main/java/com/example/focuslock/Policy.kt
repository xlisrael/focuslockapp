package com.example.focuslock

import android.content.Context
import android.content.pm.ApplicationInfo
import android.provider.Telephony
import android.telecom.TelecomManager

object Policy {
    private val denylist = setOf(
        "com.facebook.katana",
        "com.facebook.lite",
        "com.facebook.orca",
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.twitter.android",
        "com.xingin.xhs",
        "com.snapchat.android",
        "org.telegram.messenger",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.linkedin.android",
        "com.reddit.frontpage",
        "com.pinterest",
        "com.discord",
        "com.google.android.youtube"
    )

    fun isSocial(i: ApplicationInfo): Boolean {
        val category = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            i.category
        } else {
            ApplicationInfo.CATEGORY_UNDEFINED
        }
        return category == ApplicationInfo.CATEGORY_SOCIAL || i.packageName in denylist
    }

    fun smsPackage(c: Context): String? = Telephony.Sms.getDefaultSmsPackage(c)

    fun dialerPackage(c: Context): String? {
        val telecom = c.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        return telecom?.defaultDialerPackage
    }

    fun allowedPickableApps(c: Context): List<ApplicationInfo> {
        val pm = c.packageManager
        val installed = pm.getInstalledApplications(0)
        return installed.filter { app ->
            if (app.packageName == c.packageName) return@filter false
            if (isSocial(app)) return@filter false
            val category = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                app.category
            } else {
                ApplicationInfo.CATEGORY_UNDEFINED
            }
            if (category == ApplicationInfo.CATEGORY_GAME) return@filter false
            pm.getLaunchIntentForPackage(app.packageName) != null
        }.sortedBy { pm.getApplicationLabel(it).toString() }
    }

    @Deprecated("Use allowedPickableApps")
    fun musicApps(c: Context): List<ApplicationInfo> = allowedPickableApps(c)

    fun base(c: Context): Set<String> = setOfNotNull(
        c.packageName,
        dialerPackage(c),
        smsPackage(c),
        "com.android.systemui",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.incallui",
        "com.google.android.dialer",
        "com.android.dialer",
        "com.android.emergency"
    )

    fun isAllowed(c: Context, pkg: String): Boolean {
        if (pkg.isEmpty()) return true
        if (pkg == c.packageName) return true
        if (pkg in Store.allowed(c)) return true
        if (pkg.contains("inputmethod")) return true
        if (pkg.endsWith(".ime")) return true
        if (pkg.startsWith("com.android.")) {
            if (pkg.contains("systemui") || pkg.contains("launcher")) return true
        }
        return false
    }
}
