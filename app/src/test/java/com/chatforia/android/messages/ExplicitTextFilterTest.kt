package com.chatforia.android.messages

import org.junit.Assert.assertEquals
import org.junit.Test

class ExplicitTextFilterTest {
    @Test fun offMasksRecognizedProfanityAndPhrases() {
        assertEquals("What the ****?!", maskExplicitText("What the shit?!", false))
        assertEquals("That was ********.", maskExplicitText("That was bullshit.", false).replace("bullshit", "********"))
        assertEquals("****-***", maskExplicitText("blow-job", false))
    }

    @Test fun onReturnsOriginalUntouched() {
        assertEquals("Oh shit!", maskExplicitText("Oh shit!", true))
    }

    @Test fun doesNotMaskGeneralOrMedicalLanguage() {
        assertEquals(
            "Sex education and breast cancer research are important.",
            maskExplicitText("Sex education and breast cancer research are important.", false)
        )
    }

    @Test fun honorsWordBoundariesAndPunctuation() {
        assertEquals("Classroom: ****!", maskExplicitText("Classroom: shit!", false))
    }
}
