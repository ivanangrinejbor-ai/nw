package org.catrobat.catroid.ai.modify

data class ChangeCard(
    val label: String,
    val objectName: String? = null,
    val sceneName: String? = null,
    val added: Int = 0,
    val removed: Int = 0
)
