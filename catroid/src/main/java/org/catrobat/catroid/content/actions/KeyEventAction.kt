package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import com.gaurav.avnc.vnc.XKeySymUnicode
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class KeyEventAction : TemporalAction() {
    var scope: Scope? = null
    var character: Formula? = null
    var isDown: Formula? = null

    private fun stringToKeysym(input: String): Int {
        if (input.isEmpty()) return 0

        val tag = input.uppercase().trim()

        return when (tag) {
            "<ESC>", "ESCAPE" -> 0xFF1B
            "<TAB>" -> 0xFF09
            "<CAPS_LOCK>", "<CAPSLOCK>" -> 0xFFE5
            "<SHIFT_L>" -> 0xFFE1
            "<SHIFT_R>" -> 0xFFE2
            "<CTRL_L>" -> 0xFFE3
            "<CTRL_R>" -> 0xFFE4
            "<ALT_L>" -> 0xFFE9
            "<ALT_R>" -> 0xFFEA
            "<WIN>", "<WIN_L>", "<COMMAND>", "<SUPER>" -> 0xFFEB
            "<WIN_R>" -> 0xFFEC
            "<MENU>" -> 0xFF67
            "<ENTER>", "\n" -> 0xFF0D
            "<BACKSPACE>", "<BKSP>" -> 0xFF08
            "<SPACE>", " " -> 0x0020

            "<F1>" -> 0xFFBE
            "<F2>" -> 0xFFBF
            "<F3>" -> 0xFFC0
            "<F4>" -> 0xFFC1
            "<F5>" -> 0xFFC2
            "<F6>" -> 0xFFC3
            "<F7>" -> 0xFFC4
            "<F8>" -> 0xFFC5
            "<F9>" -> 0xFFC6
            "<F10>" -> 0xFFC7
            "<F11>" -> 0xFFC8
            "<F12>" -> 0xFFC9

            "<PRT_SCR>", "<PRINTSCREEN>" -> 0xFF61
            "<SCROLL_LOCK>" -> 0xFF14
            "<PAUSE>", "<BREAK>" -> 0xFF13
            "<INS>", "<INSERT>" -> 0xFF63
            "<DEL>", "<DELETE>" -> 0xFFFF

            "<HOME>" -> 0xFF50
            "<END>" -> 0xFF57
            "<PGUP>", "<PAGE_UP>" -> 0xFF55
            "<PGDN>", "<PAGE_DOWN>" -> 0xFF56
            "<UP>" -> 0xFF52
            "<DOWN>" -> 0xFF54
            "<LEFT>" -> 0xFF51
            "<RIGHT>" -> 0xFF53
            "<NUM_0>" -> 0xFFB0
            "<NUM_1>" -> 0xFFB1
            "<NUM_2>" -> 0xFFB2
            "<NUM_3>" -> 0xFFB3
            "<NUM_4>" -> 0xFFB4
            "<NUM_5>" -> 0xFFB5
            "<NUM_6>" -> 0xFFB6
            "<NUM_7>" -> 0xFFB7
            "<NUM_8>" -> 0xFFB8
            "<NUM_9>" -> 0xFFB9
            "<NUM_DOT>", "<NUM_.>" -> 0xFFAE
            "<NUM_ENTER>" -> 0xFF8D
            "<NUM_PLUS>", "<NUM_+>" -> 0xFFAB
            "<NUM_MINUS>", "<NUM_->" -> 0xFFAD
            "<NUM_MUL>", "<NUM_*>" -> 0xFFAA
            "<NUM_DIV>", "<NUM_/>" -> 0xFFAF
            "<NUM_LOCK>" -> 0xFF7F

            else -> {
                if (input.length == 1) {
                    XKeySymUnicode.getKeySymForUnicodeChar(input[0].code)
                } else {
                    0
                }
            }
        }
    }

    override fun update(percent: Float) {
        val keyStr = character?.interpretString(scope) ?: ""
        val down = isDown?.interpretBoolean(scope) ?: true

        if (keyStr.isEmpty()) return

        val keysym = stringToKeysym(keyStr)
        if (keysym == 0) return

        val stageActivity = StageActivity.activeStageActivity.get()
        stageActivity?.sendVmKeyEvent(keysym, down)
    }
}