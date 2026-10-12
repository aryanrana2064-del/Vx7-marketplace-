package com.vx7.marketing

import org.junit.Assert.*
import org.junit.Test

class UtilTest {
    @Test fun qtyFormatKeepsWhatWasEntered() {
        assertEquals("2", fmtQty(2.0))
        assertEquals("1.5", fmtQty(1.5))
        assertEquals("0.25", fmtQty(0.25))
    }

    @Test fun qtyParsingRejectsInvalid() {
        assertNull(parseQty(""))
        assertNull(parseQty("abc"))
        assertNull(parseQty("0"))
        assertNull(parseQty("-3"))
        assertNull(parseQty("NaN"))
        assertEquals(1.5, parseQty("1,5")!!, 0.0)
        assertEquals(10.0, parseQty(" 10 ")!!, 0.0)
    }

    @Test fun similarNamesDetection() {
        assertTrue(similarNames("Parle-G", "parle g"))
        assertTrue(similarNames("Tata Salt", "Tata Salt 1kg"))
        assertTrue(similarNames("Maggi", "Maggie"))
        assertFalse(similarNames("Rice", "Salt"))
    }

    @Test fun unitsContainOther() {
        assertEquals("Other", UNITS.last())
        assertEquals(13, UNITS.size)
    }
}
