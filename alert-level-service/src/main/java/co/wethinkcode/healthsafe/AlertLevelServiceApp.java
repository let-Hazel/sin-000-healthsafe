package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.wethinkcode.healthsafe.mq.MqConfig;
import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.*;

public class AlertLevelServiceApp {

    private static final Logger logger = LoggerFactory.getLogger(AlertLevelServiceApp.class);

    private static volatile AlertStatus currentStatus = new AlertStatus(0, "Normal Operations", "SYSTEM");

    public static void main(String[] args) {

        // Start background MQ Consumer listener
        startMqConsumer();

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

    public static void startMqConsumer() {
    new Thread(() -> {
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(MqConfig.QUEUE);
            MessageConsumer consumer = session.createConsumer(destination);

            logger.info("[MQ_CONSUMER] Listening for critical events on queue: {}", MqConfig.QUEUE);

            consumer.setMessageListener(message -> {
                if (message instanceof TextMessage textMessage) {
                    try {
                        String payload = textMessage.getText();
                        logger.info("[MQ_CONSUMER_EVENT] received_payload={}", payload);
                        
                        // Automatically escalate alert level on critical equipment failure
                        setCurrentAlertLevel(3, "Equipment Failure Detected via ActiveMQ Queue", "MQ_SYSTEM_LISTENER");
                            logger.info("[PAM_AUDIT] event=SYSTEM_ALERT_ESCALATED new_code=3 actor=MQ_SYSTEM_LISTENER");
                    } catch (JMSException e) {
                        logger.error("[MQ_CONSUMER_ERROR] Failed to read message text: {}", e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            logger.warn("[MQ_CONSUMER_WARNING] ActiveMQ broker unreachable at {}. Continuing in standalone mode.", MqConfig.BROKER_URL);
        }
    }).start();
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
