package com.nmlp.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenderTypeTest {

    @Test
    void fromInputMaleVariants() {
        assertEquals(GenderType.MALE, GenderType.fromInput("male").orElseThrow());
        assertEquals(GenderType.MALE, GenderType.fromInput("M").orElseThrow());
        assertEquals(GenderType.MALE, GenderType.fromInput("  m  ").orElseThrow());
    }

    @Test
    void fromInputFemaleVariants() {
        assertEquals(GenderType.FEMALE, GenderType.fromInput("female").orElseThrow());
        assertEquals(GenderType.FEMALE, GenderType.fromInput("f").orElseThrow());
    }

    @Test
    void fromInputEmptyForUnknown() {
        assertTrue(GenderType.fromInput("other").isEmpty());
        assertTrue(GenderType.fromInput("").isEmpty());
    }

    @Test
    void codes() {
        assertEquals("male", GenderType.MALE.code());
        assertEquals("female", GenderType.FEMALE.code());
    }
}
