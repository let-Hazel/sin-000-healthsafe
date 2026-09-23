package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.standardCharsets;
import java.util.ArrayLists;
import java.util.List;


public class IngestionServiceApp {

    //initialize SLF4J Logger for security audit logging
    private static final Logger logger = LoggerFactory.getLogger(IngestionServiceApp.class);

    //cleaned ward data in-memory store
    private static final List<Ward> cleanedWards = new ArrayList<>();

    public static void main(String[] args) {

        //Read and clean csv data
        loadAndCleanCsv();

        
        //create  and start a java web service using javalin, and make it listen for requests on port
        //7030
        Javalin app = Javalin.create().start(7030);

        
        //check standard health route for microservice monitoring
        app.get("/health", ctx -> {
            logger.info("[Audit] event=HEALTH_CHECK status=UP client_ip={}", ctx.ip());
                        ctx.result("OK");
        });  

        //Expose cleaned  wards endpoint for other services to consume
        app.get("wards", ctx -> {
            logger.info("[AUDIT' event=FETCH_WARDS count={} client_ip={}, cleanedWards.size(),
            ctx.ip(();
            ctx.json(cleanedWards);

        logger.info("Ingestion Service active on port 7030");

        // TODO: read and clean src/main/resources/wards-outdated.csv (wards, wings, specialist departments data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.
    }

    public static void loadAndCleanCsv() {

            cleanedWards.clear();
            InpuputStream inputStream = IngestionServiceApp.class.getClassLoader()
                .getResourceAsStream("wards-outdated.csv");

            if (inputStream == null) {
                logger.error("[AUDIT] event=CSV_LOAD_FAILED reason=FILE_NOT_FOUND");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream,StandardCharsets.UTF_8))) {
                String line;
                boolean isHeader = true;

                while ((line = reader.readline()) != null) {
                    if (isHeader) {
                        isHeader = false;
                        continue;
                    }

                    String[] cols = line.split(",", -1);
                    if (cols.length < 4) continue;

                    //Clean columns: normalize whitespace (including non-breaking space \u00a0)
                    String rawId = cols[0].replace("\u00a0", " ").trim().toUpperCase();
                    String rawWing = cols[1].replace("\u00a0", " ").trim();
                    String rawDept = cleanDepartment(cols[2].replace("\u00a0", " ").trim());
                    int beds = parseBeds(cols[3]);

                    Ward ward = new Ward(rawId, rawWing, rawDept, beds);
                    cleanedWards.add(ward);

                }
            
            



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
