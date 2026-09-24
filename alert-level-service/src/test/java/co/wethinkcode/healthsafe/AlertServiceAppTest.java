package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertServiceAppTest {

    @BeforeEach
    void setUp() {
        //Reset to default status
        AlertLevelServiceApp.setCurrentAlertLevel(0, "Normal Operations", "SYSTEM");
    }

    @Test
    void shouldGetDefaultAlertLevel() {
        //NPAM check: Anyone can read current alert level
        AlertStatus status = AlertLevelServiceApp.getCurrentAlertStatus();
        assertEquals(0, status.code());
        assertEquals("Normal Operations", status.description());
    }

    @Test
    void shouldUpdateAlertLevelWithEmergencyAdminRole() {
        //PAM check: Escalationallowed with EMERGENCY_ADMIN
        boolean updated = AlertLevelServiceApp.updateAlertLevel(5, "Code Red - Fire Escalation", "EMERGENCY_ADMIN");
        assertTrue(updated);
        assertEquals(5, AlertLevelServiceApp.getCurrentAlertStatus().code());
    }

    @Test
    void shouldUpdateAlertLevelUnauthorizedRoleFails() {
        //Pam check: Reject unauthorized escalation attempts
        boolean updated = AlertLevelServiceApp.updateAlertLevel(8, "Mass casualty", "NURSE");
        assertFalse(updated);
        assertEquals(0, AlertLevelServiceApp.getCurrentAlertStatus().code());

    }

    @Test
    void shouldFailInvalidAlertCodeBounds() {
        //Input Validation: Reject codes outside valid range (0-8)
        boolean updated = AlertLevelServiceApp.updateAlertLevel(99, "Invalid Code", "EMERGENCY_ADMIN");
        assertFalse(updated);
        assertEquals(0, AlertLevelServiceApp.getCurrentAlertStatus().code());
    }

}