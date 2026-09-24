package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class WardServiceApp {

    //initialize logger for pam & audit tracking
    private static final Logger logger = LoggerFactory.getLogger(WardServiceApp.class);

    //concurrenthashmap
    private static final Map<String, Ward> wardsMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7031);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Provides lists of wards and departments.)
        // Add domain endpoints for ward-service here.

        //NPAM Route: Unprivileged access to list all wards
        app.get("/wards", ctx -> {
            logger.info("[Audit] event=FETCH_ALL_WARDS client_ip={}", ctx.ip());
            ctx.json(wardsMap.values());
        });

        //NPAM Route: Unprivileged access to fetch a single ward by id
        app.get("/wards/{id}", ctx -> {
            String id = ctx.pathParam("id");
            Ward ward = findWardById(id);
            if (ward != null) {
                logger.info("[AUDIT] event=FETCH_WARD_BY_ID ward_id={} client_ip={}", id, ctx.ip());
                ctx.json(ward);
            } else {
                ctx.status(404).result("Ward not found");
            }
        });

        //PAM Route: Privileged access to mutate bed availability count
        app.patch("/wards/{id}/beds", ctx -> {
            String id = ctx.pathParam("id");
            String role = ctx.header("X-User-Role");

            int newBeds = 0;
            try {
                String countParam = ctx.queryParam("count");
                if (countParam != null) {
                    newBeds = Integer.parseInt(countParam);
                }
            } catch (NumberFormatException e) {
                ctx.status(400).result("Invalid bed count value");
                return;
            }

            boolean success = updateWardBeds(id, newBeds, role);
            if (success) {
                ctx.json(findWardById(id));
            } else {
                ctx.status(403).result("Forbidden: Insufficient privileges or ward not found.");
            }
        });

        logger.info("Ward Service actie on port 7031");
    }
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
// MQ TODO: publishes to ActiveMQ queue MqConfig.QUEUE when it detects an equipment failure on one of its wards.
