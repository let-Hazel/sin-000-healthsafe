package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WardServiceAppTest {


    @BeforeEach
    void setUp() {
        //reset in-memory sate before each test
        WardServiceApp.getWardsMap().clear();
        WardServiceApp.getWardsMap().put("W1", new Ward("W1", "North Wing", "General", 10));
    }

    @Test
    void shouldFindWardById() {
        //NPAM check: Fetching ward state
        Ward ward = WardServiceApp.findWardById("W1");
        assertNotNull(ward);
        assertEquals("North Wing", ward.wing());
    }

    @Test
    void shouldFindWardByIdNotFound() {
        Ward ward = WardServiceApp.findWardById("NON_EXISTENT");
        assertNull(ward);
    }

    @Test
    void shouldUpdateBedsWithValidRole() {
        //PAM Check: Privileged update with WARD_ADMIN role
        boolean updated = WardServiceApp.updateWardBeds("W1", 5, "WARD_ADMIN");
        assertTrue("updated");
        assertEquals(5, WardServiceApp.findWardById("W1").bedsAvailable());
    }

    @Test
    void shouldUpdateBedsWithUnauthorizedRoleFails() {
        //PAM Check: reject update from non-privileged actors
        boolean updated = WardServiceApp.updateWardBeds("W1", 5, "READ_ONLY");
        assertFalse(updated);
        //Capacity should remain unchanged 
        assertEquals(10, WardServiceApp.findWardById("W1").bedsAvailable());
    }
}