package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import co.wethinkcode.healthsafe.mq.MqConfig;
import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.*;

public class EquipmentAlertServiceApp {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentAlertServiceApp.class);

    private static final Map<String, Equipment> equipmentMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7034);

        app.get("/health", ctx -> ctx.result("OK"));

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

    public static void publishEquipmentAlert(String equipmentId, String status, String wardId) {
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(MqConfig.QUEUE);
            MessageProducer producer = session.createProducer(destination);

            String payload = String.format("{\"event\":\"EQUIPMENT_FAILURE\", \"equipmentId\":\"%s\", \"status\":\"%s\", \"wardId\":\"%s\"}",
                    equipmentId, status, wardId);

            TextMessage message = session.createTextMessage(payload);
            producer.send(message);

            logger.info("[MQ_PRODUCER] published=EQUIPMENT_FAILURE equipment_id={} status={} destination={}", 
                    equipmentId, status, MqConfig.QUEUE);

            producer.close();
            session.close();
            connection.close();
        } catch (Exception e) {
            logger.error("[MQ_PRODUCER_ERROR] Failed to publish message to ActiveMQ: {}", e.getMessage());
        }
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

        // 1. Retrieve existing record first
        Equipment existing = equipmentMap.get(id);
        if (existing == null) {
            return false;
        }

        // 2. Mutate in-memory state
        Equipment updated = new Equipment(
                existing.equipmentId(),
                existing.name(),
                existing.type(),
                existing.wardId(),
                newStatus.toUpperCase()
        );
        equipmentMap.put(id, updated);

        // 3. Publish to ActiveMQ if faulty/decommissioned
        if ("FAULTY".equalsIgnoreCase(newStatus) || "DECOMMISSIONED".equalsIgnoreCase(newStatus)) {
            publishEquipmentAlert(id, newStatus.toUpperCase(), existing.wardId());
        }

        logger.info("[PAM_AUDIT] event=EQUIPMENT_STATUS_MUTATED actor_role={} equipment_id={} new_status={}", 
                userRole, id, newStatus.toUpperCase());
        return true;
    }

    public static Map<String, Equipment> getEquipmentMap() {
        return equipmentMap;
    }
}