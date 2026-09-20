package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

public class IngestionServiceApp {

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7030);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO: read and clean src/main/resources/wards-outdated.csv (wards, wings, specialist departments data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.
    }

    public static int parseBeds(String bed) {
        if (bed == null) return 0;

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

            if (beds < 0 || beds > 100) {
                return 0;
            }
            return beds;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
