package org.catrobat.catroid.ai.modify

/**
 * Structured description of a single project change applied by the AI agent, used to
 * render a compact "change card" in the chat (object - scene, with +added / -removed counts).
 */
data class ChangeCard(
    val label: String,
    val objectName: String? = null,
    val sceneName: String? = null,
    val added: Int = 0,
    val removed: Int = 0
)
