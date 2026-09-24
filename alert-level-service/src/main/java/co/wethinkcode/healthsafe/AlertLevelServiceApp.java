package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlertLevelServiceApp {

    private static final Logger logger = LoggerFactory.getLogger(AlertLevelServiceApp.class);

    private static volatile AlertStatus currentStatus = new AlertStatus(0, "Normal Operations", "SYSTEM");

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7032);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Tracks the hospital Emergency Status (0-8, 8 = full Code Blue).)
        // Add domain endpoints for alert-level-service here.

        app.get("/alerts/current", ctx -> {
            logger.info("[AUDIT] event=FETCH_ALERT_STATUS client_ip={}", ctx.ip());
            ctx.json(getCurrentAlertStatus());
        });

        //PAM Route: Unprivileged access to read current alert status
        app.post("/alerts/update", ctx -> {
            String role = ctx.header("X-User-Role");

            int code;

            try {
                String codeParam = ctx.queryParam("code");
                code = (codeParam != null) ? Integer.parseInt(codeParam) : 0;
            } catch (NumberFormatException e) {
                ctx.status(400).result("Invalid alert code format");
                return;
            }

            String description = ctx.queryParam("description");
            if (description == null || description.isBlank()) {
                description = "General Alert";
            }

            boolean updated = updateAlertLevel(code, description, role);
            if (updated) {
                ctx.json(getCurrentAlertStatus());
            } else {
                ctx.status(403).result("Forbidden: Insufficient privileges or invalid code bounds (0-8).");
            }
        });

        logger.info("Alert Level Service active on port 7032");
    }

    public static AlertStatus getCurrentAlertStatus() {
        return currentStatus;
    }

    public static void setCurrentAlertLevel(int code, String description, String updatedBy) {
        currentStatus = new AlertStatus(code, description, updatedBy);
    }

    public static boolean updateAlertLevel(int code, String description, String role) {

        if (!"EMERGENCY_ADMIN".equalsIgnoreCase(role)) {
            logger.warn("[SECURITY_ALERT] event=UNAUTHORIZED_ALERT_MUTATION user_role={} attempted_code={}", role, code);
            return false;
        }

        if (code < 0 || code > 8) {
            logger.warn("[SECURITY_ALERT] event=INVALID_ALERT_BOUNDS code={}", code);
            return false;
        }

        currentStatus = new AlertStatus(code, description, role);
        logger.info("[PAM_AUDIT] event=ALERT_LEVEL_MUTATED new_code={} description={} actor={}", code, description, role);
        return true;
    }
}
