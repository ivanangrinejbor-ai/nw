package org.catrobat.catroid.apkbuildV3

import android.os.Parcel
import android.os.Parcelable
import java.io.File

data class ApkBuilderV3Config(
    val appName: String,
    val packageName: String = "org.neocatroid.runtime.v3",
    val versionName: String = "1.0",
    val versionCode: Int = 1,
    val minSdk: Int = 21,
    val targetSdk: Int = 35,
    val iconFile: File? = null,
    val permissions: List<String> = DEFAULT_PERMISSIONS,
    val templateType: TemplateType = TemplateType.FULL,
    val firebaseConfig: FirebaseConfig? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
        appName = parcel.readString() ?: "",
        packageName = parcel.readString() ?: "org.neocatroid.runtime.v3",
        versionName = parcel.readString() ?: "1.0",
        versionCode = parcel.readInt(),
        minSdk = parcel.readInt(),
        targetSdk = parcel.readInt(),
        iconFile = parcel.readString()?.let { File(it) },
        permissions = parcel.createStringArrayList() ?: DEFAULT_PERMISSIONS,
        templateType = TemplateType.valueOf(parcel.readString() ?: TemplateType.FULL.name),
        firebaseConfig = parcel.readSerializable() as? FirebaseConfig
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(appName)
        parcel.writeString(packageName)
        parcel.writeString(versionName)
        parcel.writeInt(versionCode)
        parcel.writeInt(minSdk)
        parcel.writeInt(targetSdk)
        parcel.writeString(iconFile?.absolutePath)
        parcel.writeStringList(permissions)
        parcel.writeString(templateType.name)
        @Suppress("UNCHECKED_CAST")
        parcel.writeSerializable(firebaseConfig as java.io.Serializable?)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ApkBuilderV3Config> {
        override fun createFromParcel(parcel: Parcel): ApkBuilderV3Config = ApkBuilderV3Config(parcel)
        override fun newArray(size: Int): Array<ApkBuilderV3Config?> = arrayOfNulls(size)

        val DEFAULT_PERMISSIONS = listOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.WAKE_LOCK",
            "android.permission.VIBRATE"
        )

        val ALL_PERMISSIONS = listOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_AUDIO",
            "android.permission.VIBRATE",
            "android.permission.WAKE_LOCK",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.NFC",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION"
        )
    }
}
