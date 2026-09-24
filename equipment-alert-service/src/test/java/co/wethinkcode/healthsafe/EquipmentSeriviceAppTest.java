package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentServiceAppTest {

    @BeforeEach
    void setUp() {
        // Reset state before each test
        EquipmentServiceApp.getEquipmentMap().clear();
        EquipmentServiceApp.getEquipmentMap().put(
            "EQ101",
            new Equipment("EQ101", "Ventilator V1", "VENTILATOR", "W1", "OPERATIONAL")
        );
    }

    @Test
    void testFindEquipmentById() {
        // NPAM Check: Unprivileged lookups
        Equipment eq = EquipmentServiceApp.findEquipmentById("EQ101");
        assertNotNull(eq);
        assertEquals("Ventilator V1", eq.name());
        assertEquals("OPERATIONAL", eq.status());
    }

    @Test
    void testFindEquipmentByIdNotFound() {
        Equipment eq = EquipmentServiceApp.findEquipmentById("NON_EXISTENT");
        assertNull(eq);
    }

    @Test
    void testUpdateStatusWithAuthorizedRole() {
        // PAM Check: Equipment status modification allowed with BIOMED_ENGINEER
        boolean updated = EquipmentServiceApp.updateEquipmentStatus("EQ101", "FAULTY", "BIOMED_ENGINEER");
        assertTrue(updated);
        assertEquals("FAULTY", EquipmentServiceApp.findEquipmentById("EQ101").status());
    }

    @Test
    void testUpdateStatusWithUnauthorizedRoleFails() {
        // PAM Check: Reject equipment status modification from unauthorized role
        boolean updated = EquipmentServiceApp.updateEquipmentStatus("EQ101", "DECOMMISSIONED", "VISITOR");
        assertFalse(updated);
        // Status should remain unchanged (OPERATIONAL)
        assertEquals("OPERATIONAL", EquipmentServiceApp.findEquipmentById("EQ101").status());
    }
}