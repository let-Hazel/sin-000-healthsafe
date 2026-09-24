package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EquipmentAlertServiceApp {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentAlertServiceApp.class);

    private static final Map<String, Equipment> equipmentMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7034);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Uses a Queue to guarantee delivery of critical medical equipment failure alerts.)
        // Mechanism: ActiveMQ Queue (guaranteed delivery)

        // NPAM Route: Unprivileged access to list all equipment
        app.get("/equipment", ctx -> {
            logger.info("[AUDIT] event=FETCH_ALL_EQUIPMENT client_ip={}", ctx.ip());
            ctx.json(equipmentMap.values());
        });

        // NPAM Route: Unprivileged access to fetch single equipment record
        app.get("/equipment/{id}", ctx -> {
            String id = ctx.pathParam("id");
            Equipment eq = findEquipmentById(id);
            if (eq != null) {
                logger.info("[AUDIT] event=FETCH_EQUIPMENT_BY_ID equipment_id={} client_ip={}", id, ctx.ip());
                ctx.json(eq);
            } else {
                ctx.status(404).result("Equipment record not found");
            }
        });

        // PAM Route: Privileged route to update equipment status
        app.patch("/equipment/{id}/status", ctx -> {
            String id = ctx.pathParam("id");
            String role = ctx.header("X-User-Role");
            String newStatus = ctx.queryParam("status");

            if (newStatus == null || newStatus.isBlank()) {
                ctx.status(400).result("Missing target status parameter");
                return;
            }

            boolean success = updateEquipmentStatus(id, newStatus, role);
            if (success) {
                ctx.json(findEquipmentById(id));
            } else {
                ctx.status(403).result("Forbidden: Insufficient privileges or equipment record not found.");
            }
        });

        logger.info("Equipment Service active on port 7034");
    }

    public static Equipment findEquipmentById(String id) {
        return equipmentMap.get(id);
    }

    public static boolean updateEquipmentStatus(String id, String newStatus, String userRole) {
        // PAM Security Enforcement: Requires BIOMED_ENGINEER or EQUIPMENT_ADMIN
        if (!"BIOMED_ENGINEER".equalsIgnoreCase(userRole) && !"EQUIPMENT_ADMIN".equalsIgnoreCase(userRole)) {
            logger.warn("[SECURITY_ALERT] event=UNAUTHORIZED_EQUIPMENT_MUTATION actor_role={} equipment_id={} target_status={}", 
                    userRole, id, newStatus);
            return false;
        }

        Equipment existing = equipmentMap.get(id);
        if (existing == null) {
            return false;
        }

        Equipment updated = new Equipment(
                existing.equipmentId(),
                existing.name(),
                existing.type(),
                existing.wardId(),
                newStatus.toUpperCase()
        );
        equipmentMap.put(id, updated);

        logger.info("[PAM_AUDIT] event=EQUIPMENT_STATUS_MUTATED actor_role={} equipment_id={} new_status={}", 
                userRole, id, newStatus.toUpperCase());
        return true;
    }

    public static Map<String, Equipment> getEquipmentMap() {
        return equipmentMap;
    }
}

// MQ TODO: consumes ActiveMQ queue MqConfig.QUEUE at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
// Producer: ward-service publishes here when it detects an equipment failure on one of its wards.
