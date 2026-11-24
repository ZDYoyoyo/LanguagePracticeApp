package io.github.zdyoyoyo.englishpractice

import io.github.zdyoyoyo.englishpractice.speech.VoicePicker
import io.github.zdyoyoyo.englishpractice.speech.VoicePicker.Candidate
import io.github.zdyoyoyo.englishpractice.speech.VoicePicker.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoicePickerTest {
    @Test fun `gender from voice names`() {
        assertEquals(Gender.MALE, VoicePicker.genderOf("en-us-x-iom-local"))
        assertEquals(Gender.MALE, VoicePicker.genderOf("en-us-x-tpd-network"))
        assertEquals(Gender.FEMALE, VoicePicker.genderOf("en-us-x-sfg-local"))
        assertEquals(Gender.MALE, VoicePicker.genderOf("en-us-x-sfg#male_1-local"))
        assertEquals(Gender.FEMALE, VoicePicker.genderOf("en-us-x-sfg#female_2-local"))
        assertEquals(Gender.FEMALE, VoicePicker.genderOf("Samantha Female"))
        assertEquals(Gender.UNKNOWN, VoicePicker.genderOf("en-us-language"))
        assertEquals(Gender.UNKNOWN, VoicePicker.genderOf("en-gb-x-german-local"))
    }

    @Test fun `prefers US English and offline voices`() {
        val voices = listOf(
            Candidate("en-gb-x-gbd-local", "en-GB", false),
            Candidate("en-us-x-iom-network", "en-US", true),
            Candidate("en-us-x-iol-local", "en-US", false),
            Candidate("zh-tw-x-ctd-local", "zh-TW", false),
            Candidate("en-us-x-sfg-local", "en-US", false),
        )
        assertEquals("en-us-x-iol-local", VoicePicker.pick(voices, Gender.MALE)?.name)
        assertEquals("en-us-x-sfg-local", VoicePicker.pick(voices, Gender.FEMALE)?.name)
        assertNull(VoicePicker.pick(voices.take(1), Gender.MALE))
    }
}
