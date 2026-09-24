package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentServiceAppTest {

    @BeforeEach
    void setUp() {
        // Reset state before each test
        EquipmentAlertServiceApp.getEquipmentMap().clear();
        EquipmentAlertServiceApp.getEquipmentMap().put(
            "EQ101",
            new Equipment("EQ101", "Ventilator V1", "VENTILATOR", "W1", "OPERATIONAL")
        );
    }

    @Test
    void testFindEquipmentById() {
        // NPAM Check: Unprivileged lookups
        Equipment eq = EquipmentAlertServiceApp.findEquipmentById("EQ101");
        assertNotNull(eq);
        assertEquals("Ventilator V1", eq.name());
        assertEquals("OPERATIONAL", eq.status());
    }

    @Test
    void testFindEquipmentByIdNotFound() {
        Equipment eq = EquipmentAlertServiceApp.findEquipmentById("NON_EXISTENT");
        assertNull(eq);
    }

    @Test
    void testUpdateStatusWithAuthorizedRole() {
        // PAM Check: Equipment status modification allowed with BIOMED_ENGINEER
        boolean updated = EquipmentAlertServiceApp.updateEquipmentStatus("EQ101", "FAULTY", "BIOMED_ENGINEER");
        assertTrue(updated);
        assertEquals("FAULTY", EquipmentAlertServiceApp.findEquipmentById("EQ101").status());
    }

    @Test
    void testUpdateStatusWithUnauthorizedRoleFails() {
        // PAM Check: Reject equipment status modification from unauthorized role
        boolean updated = EquipmentAlertServiceApp.updateEquipmentStatus("EQ101", "DECOMMISSIONED", "VISITOR");
        assertFalse(updated);
        // Status should remain unchanged (OPERATIONAL)
        assertEquals("OPERATIONAL", EquipmentAlertServiceApp.findEquipmentById("EQ101").status());
    }
}