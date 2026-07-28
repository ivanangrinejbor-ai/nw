package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.ai.localization.LocalizationReport
import org.catrobat.catroid.ai.localization.SpriteLocalizer
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class LocalizeSpritesAction : TemporalAction() {
    var scope: Scope? = null
    var targetLanguage: Formula? = null
    var resultVariableName: Formula? = null
    private var executed = false

    override fun update(percent: Float) {
        if (executed) return
        executed = true

        val language = targetLanguage?.interpretString(scope) ?: "ru"
        val context = CatroidApplication.getAppContext() ?: return

        val localizer = SpriteLocalizer(context, language)

        var finalReport: LocalizationReport? = null
        val lock = Object()

        localizer.onComplete = { report ->
            synchronized(lock) {
                finalReport = report
                (lock as Object).notifyAll()
            }
        }

        localizer.localizeProject()

        val startWait = System.currentTimeMillis()
        while (finalReport == null && (System.currentTimeMillis() - startWait) < 120_000) {
            synchronized(lock) {
                if (finalReport == null) {
                    try { (lock as Object).wait(1000) } catch (_: InterruptedException) { }
                }
            }
        }

        val result = finalReport?.let {
            "Localized ${it.processedSprites}/${it.totalSprites} sprites" +
                    if (it.hasFailures()) ". Failures: ${it.failedSprites}" else ""
        } ?: "Localization timeout"

        val name = resultVariableName?.interpretString(scope) ?: ""
        val variable = scope?.sprite?.getUserVariable(name)
        variable?.value = result
    }
}
