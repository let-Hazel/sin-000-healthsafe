package co.wethinkcode.healthsafe;

import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IngestionServiceAppTest {

    @Test
    void shouldParseBedsValidNumber() {
        assertEquals(3, IngestionServiceApp.parseBeds("3"));
    }

    @Test
    void shouldConvertWordToNumberBesds() {
        assertEquals(5, IngestionServiceApp.parseBeds("five"));
    }

    @Test
    void shouldDefaultToZeroIfInvalidOrNegative() {
        assertEquals(0, IngestionServiceApp.parseBeds("N/A"));
        assertEquals(0, IngestionServiceApp.parseBeds("-1"));
        assertEquals(0, IngestionServiceApp.parseBeds("2023"));
        assertEquals(0, IngestionServiceApp.parseBeds(null));
        
    }
}
