package br.com.gui.carlembrete

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG_INSTALL_DIAGNOSTICS = "InstallDiagnostics"
private const val PREFS_NAME = "install_diagnostics"
private const val KEY_LAUNCH_COUNT = "launch_count"
private const val KEY_FIRST_SEEN_AT = "first_seen_at"
private const val KEY_LAST_VERSION_CODE = "last_version_code"
private const val KEY_LAST_VERSION_NAME = "last_version_name"
private const val KEY_LAST_FIRST_INSTALL = "last_first_install"
private const val KEY_LAST_LAST_UPDATE = "last_last_update"
private const val KEY_LAST_INSTALLER = "last_installer"
private const val KEY_LAST_INSTALLING_PKG = "last_installing_pkg"
private const val KEY_LAST_INITIATING_PKG = "last_initiating_pkg"
private const val KEY_LAST_ORIGINATING_PKG = "last_originating_pkg"

object InstallDiagnostics {
    fun logDetailedSnapshot(context: Context, trigger: String) {
        val packageName = context.packageName
        val packageInfo = runCatching { getPackageInfo(context, packageName) }
            .getOrElse {
                Log.e(TAG_INSTALL_DIAGNOSTICS, "[$trigger] failed_to_read_package_info", it)
                return
            }
        val versionCode = getLongVersionCode(packageInfo)
        val versionName = packageInfo.versionName.orEmpty()
        val firstInstallTime = packageInfo.firstInstallTime
        val lastUpdateTime = packageInfo.lastUpdateTime
        val installer = resolveInstallSources(context, packageName)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previousVersionCode = prefs.getLong(KEY_LAST_VERSION_CODE, -1L)
        val previousVersionName = prefs.getString(KEY_LAST_VERSION_NAME, null)
        val previousFirstInstall = prefs.getLong(KEY_LAST_FIRST_INSTALL, -1L)
        val previousLastUpdate = prefs.getLong(KEY_LAST_LAST_UPDATE, -1L)
        val launchCount = prefs.getInt(KEY_LAUNCH_COUNT, 0) + 1
        val firstSeenAt = prefs.getLong(KEY_FIRST_SEEN_AT, 0L).takeIf { it > 0L } ?: System.currentTimeMillis()

        val firstRunEver = launchCount == 1
        val firstRunAfterInstall = previousFirstInstall != -1L && previousFirstInstall != firstInstallTime
        val updateDetected = previousLastUpdate != -1L && previousLastUpdate != lastUpdateTime
        val versionChanged = previousVersionCode != -1L && previousVersionCode != versionCode

        val summary = buildString {
            append("trigger=").append(trigger)
            append(" | pkg=").append(packageName)
            append(" | version=").append(versionName).append(" (").append(versionCode).append(")")
            append(" | firstInstall=").append(formatEpoch(firstInstallTime))
            append(" | lastUpdate=").append(formatEpoch(lastUpdateTime))
            append(" | installer=").append(installer.installerPackage ?: "unknown")
            append(" | installingPkg=").append(installer.installingPackage ?: "unknown")
            append(" | initiatingPkg=").append(installer.initiatingPackage ?: "unknown")
            append(" | originatingPkg=").append(installer.originatingPackage ?: "unknown")
            append(" | launchCount=").append(launchCount)
            append(" | firstRunEver=").append(firstRunEver)
            append(" | firstRunAfterInstall=").append(firstRunAfterInstall)
            append(" | updateDetected=").append(updateDetected)
            append(" | versionChanged=").append(versionChanged)
            if (previousVersionCode != -1L) {
                append(" | prevVersion=").append(previousVersionName ?: "?").append(" (").append(previousVersionCode).append(")")
            }
            if (previousFirstInstall != -1L) {
                append(" | prevFirstInstall=").append(formatEpoch(previousFirstInstall))
            }
            if (previousLastUpdate != -1L) {
                append(" | prevLastUpdate=").append(formatEpoch(previousLastUpdate))
            }
            append(" | firstSeenAt=").append(formatEpoch(firstSeenAt))
        }
        Log.i(TAG_INSTALL_DIAGNOSTICS, summary)

        prefs.edit()
            .putInt(KEY_LAUNCH_COUNT, launchCount)
            .putLong(KEY_FIRST_SEEN_AT, firstSeenAt)
            .putLong(KEY_LAST_VERSION_CODE, versionCode)
            .putString(KEY_LAST_VERSION_NAME, versionName)
            .putLong(KEY_LAST_FIRST_INSTALL, firstInstallTime)
            .putLong(KEY_LAST_LAST_UPDATE, lastUpdateTime)
            .putString(KEY_LAST_INSTALLER, installer.installerPackage)
            .putString(KEY_LAST_INSTALLING_PKG, installer.installingPackage)
            .putString(KEY_LAST_INITIATING_PKG, installer.initiatingPackage)
            .putString(KEY_LAST_ORIGINATING_PKG, installer.originatingPackage)
            .apply()
    }

    private fun getPackageInfo(context: Context, packageName: String): PackageInfo {
        val packageManager = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
    }

    private fun getLongVersionCode(packageInfo: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    private fun formatEpoch(epochMs: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return formatter.format(Date(epochMs))
    }

    private fun resolveInstallSources(context: Context, packageName: String): InstallSourceSnapshot {
        val packageManager = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val info = runCatching { packageManager.getInstallSourceInfo(packageName) }.getOrNull()
            InstallSourceSnapshot(
                installerPackage = info?.installingPackageName,
                installingPackage = info?.installingPackageName,
                initiatingPackage = info?.initiatingPackageName,
                originatingPackage = info?.originatingPackageName
            )
        } else {
            @Suppress("DEPRECATION")
            val installerPackage = runCatching { packageManager.getInstallerPackageName(packageName) }.getOrNull()
            InstallSourceSnapshot(
                installerPackage = installerPackage,
                installingPackage = installerPackage,
                initiatingPackage = null,
                originatingPackage = null
            )
        }
    }

    private data class InstallSourceSnapshot(
        val installerPackage: String?,
        val installingPackage: String?,
        val initiatingPackage: String?,
        val originatingPackage: String?
    )
}
