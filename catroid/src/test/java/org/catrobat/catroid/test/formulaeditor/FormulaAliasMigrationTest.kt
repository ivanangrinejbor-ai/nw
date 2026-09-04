package org.catrobat.catroid.test.formulaeditor

import org.catrobat.catroid.formulaeditor.FormulaElement
import org.catrobat.catroid.io.LEGACY_FORMULA_ALIASES
import org.catrobat.catroid.io.migrateLegacyFormulaElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FormulaAliasMigrationTest {

    @Test
    fun battarySensorMigratesToBatteryPercentFunction() {
        val element = FormulaElement(FormulaElement.ElementType.SENSOR, "BATTARY", null)

        migrateLegacyFormulaElement(element)

        assertEquals(FormulaElement.ElementType.FUNCTION, element.elementType)
        assertEquals("BATTERY_PERCENT", element.value)
    }

    @Test
    fun archSensorMigratesToCpuArchitectureFunction() {
        val element = FormulaElement(FormulaElement.ElementType.SENSOR, "ARCH", null)

        migrateLegacyFormulaElement(element)

        assertEquals(FormulaElement.ElementType.FUNCTION, element.elementType)
        assertEquals("CPU_ARCHITECTURE", element.value)
    }

    @Test
    fun joinNumberKeepsChildrenAndBecomesJoin() {
        val element = FormulaElement(FormulaElement.ElementType.FUNCTION, "JOINNUMBER", null)
        val left = FormulaElement(FormulaElement.ElementType.NUMBER, "1", element)
        val right = FormulaElement(FormulaElement.ElementType.NUMBER, "2", element)
        element.leftChild = left
        element.rightChild = right

        migrateLegacyFormulaElement(element)

        assertEquals(FormulaElement.ElementType.FUNCTION, element.elementType)
        assertEquals("JOIN", element.value)
        assertEquals(left, element.leftChild)
        assertEquals(right, element.rightChild)
    }

    @Test
    fun unknownNamesAreUntouched() {
        val sensor = FormulaElement(FormulaElement.ElementType.SENSOR, "TIMER", null)
        val function = FormulaElement(FormulaElement.ElementType.FUNCTION, "JOIN", null)

        migrateLegacyFormulaElement(sensor)
        migrateLegacyFormulaElement(function)

        assertEquals(FormulaElement.ElementType.SENSOR, sensor.elementType)
        assertEquals("TIMER", sensor.value)
        assertEquals(FormulaElement.ElementType.FUNCTION, function.elementType)
        assertEquals("JOIN", function.value)
    }

    @Test
    fun aliasTableCoversExactlyThreeLegacyNames() {
        assertEquals(3, LEGACY_FORMULA_ALIASES.size)
        assertTrue(LEGACY_FORMULA_ALIASES.containsKey("BATTARY"))
        assertTrue(LEGACY_FORMULA_ALIASES.containsKey("ARCH"))
        assertTrue(LEGACY_FORMULA_ALIASES.containsKey("JOINNUMBER"))
        assertNull(LEGACY_FORMULA_ALIASES["JOIN"])
    }
}
