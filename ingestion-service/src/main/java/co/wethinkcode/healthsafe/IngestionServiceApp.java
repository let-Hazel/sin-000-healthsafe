package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IngestionServiceApp {

    //initialize SLF4J Logger for security audit logging
    private static final Logger loger = LoggerFactory.getLogger(IngestionServiceApp.class);

    public static void main(String[] args) {
        //create  and start a java web service using javalin, and make it listen for requests on port
        //7030
        Javalin app = Javalin.create().start(7030);

        
        //check standard health route for microservice monitoring
        app.get("/health", ctx -> {
            logger.info("[Audit] event=HEALTH_CHECK status=UP client_ip={}, ctx.ip());
                        ctx.result("OK");
        });    

        logger.info("Ingestion Service active on port 7030");

        // TODO: read and clean src/main/resources/wards-outdated.csv (wards, wings, specialist departments data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.
    }

    public static int parseBeds(String bed) {
        if (bed == null) {
            logger.warn("[AUDIT] event=DATA_CLEANUP action=DEFAULT_TO_ZERO reason=NULL_INPUT");
        return 0;
        }    

        String clean = bed.trim().toLowerCase();

        //word to number inputs
        switch (clean) {
            case "one": return 1;
            case "two": return 2;
            case "three": return 3;
            case "four": return 4;
            case "five": return 5;  
        }

        try {
            int beds = Integer.parseInt(clean);

            //Anomaly check: reject unrealistic counts

            if (beds < 0 || beds > 100) {
                logger.warn("[AUDIT] event=ANOMALY_DETECTED action=DEFAULT_TO_ZERO raw_value={} reason=OUT_OF_BOUNDS",
                            bed);
                return 0;
            }
            return beds;
        } catch (NumberFormatException e) {
            //ANOMALY Check: Handle sentinels
            logger.warn("[AUDIT] event=ANOMALY_DETECTED action=DEFAULT_TO_ZERO raw_value={} reason=NON_NUMERIC_STRING", bed);
            
            return 0;
        }
    }
}
