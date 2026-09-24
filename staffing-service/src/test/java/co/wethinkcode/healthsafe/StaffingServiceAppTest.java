package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StaffingServiceAppTest {

    @BeforeEach
    void setUp() {
        // Reset state before each test
        StaffingServiceApp.getStaffMap().clear();
        StaffingServiceApp.getStaffMap().put(
            "ST01",
            new StaffAssignment("ST01", "Dr. Alice Smith", "DOCTOR", "W1")
        );
    }

    @Test
    void testFindStaffById() {
        // NPAM Check: Fetching shift assignment
        StaffAssignment staff = StaffingServiceApp.findStaffById("ST01");
        assertNotNull(staff);
        assertEquals("Dr. Alice Smith", staff.name());
        assertEquals("W1", staff.assignedWardId());
    }

    @Test
    void testFindStaffByIdNotFound() {
        StaffAssignment staff = StaffingServiceApp.findStaffById("NON_EXISTENT");
        assertNull(staff);
    }

    @Test
    void testAssignStaffToWardWithAuthorizedRole() {
        // PAM Check: Roster modification allowed with STAFF_ADMIN
        boolean updated = StaffingServiceApp.assignStaffToWard("ST01", "W2", "STAFF_ADMIN");
        assertTrue(updated);
        assertEquals("W2", StaffingServiceApp.findStaffById("ST01").assignedWardId());
    }

    @Test
    void testAssignStaffToWardWithUnauthorizedRoleFails() {
        // PAM Check: Reject roster modification from unauthorized roles
        boolean updated = StaffingServiceApp.assignStaffToWard("ST01", "W3", "NURSE");
        assertFalse(updated);
        // Ward assignment should remain unchanged (W1)
        assertEquals("W1", StaffingServiceApp.findStaffById("ST01").assignedWardId());
    }
}