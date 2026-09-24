package co.wethinkcode.healthsafe;

import io.javalin.Javalin;




import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class StaffingServiceApp {

    private static final Logger logger = LoggerFactory.getLogger(StaffingServiceApp.class);

    private static final Map<String, StaffAssignment> staffMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7033);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Provides on-call schedules for doctors based on ward and status.)
        // Add domain endpoints for staffing-service here.

        app.get("/staff", ctx -> {
            logger.info("[AUDIT] event=FETCH_ALL_STAFF client_ip={}", ctx.ip());
            ctx.json(staffMap.values());
        });

        // NPAM Route: Unprivileged access to fetch single staff assignment
        app.get("/staff/{id}", ctx -> {
            String id = ctx.pathParam("id");
            StaffAssignment staff = findStaffById(id);
            if (staff != null) {
                logger.info("[AUDIT] event=FETCH_STAFF_BY_ID staff_id={} client_ip={}", id, ctx.ip());
                ctx.json(staff);
            } else {
                ctx.status(404).result("Staff record not found");
            }
        });

        // PAM Route: Privileged route to reassign staff to a ward
        app.patch("/staff/{id}/reassign", ctx -> {
            String id = ctx.pathParam("id");
            String role = ctx.header("X-User-Role");
            String newWardId = ctx.queryParam("wardId");

            if (newWardId == null || newWardId.isBlank()) {
                ctx.status(400).result("Missing target wardId parameter");
                return;
            }

            boolean success = assignStaffToWard(id, newWardId, role);
            if (success) {
                ctx.json(findStaffById(id));
            } else {
                ctx.status(403).result("Forbidden: Insufficient privileges or staff record not found.");
            }
        });

        logger.info("Staffing Service active on port 7033");
    }

    public static StaffAssignment findStaffById(String id) {
        return staffMap.get(id);
    }

    public static boolean assignStaffToWard(String id, String newWardId, String userRole) {

        if (!"STAFF_ADMIN".equalsIgnoreCase(userRole) && !"HR_ADMIN".equalsIgnoreCase(userRole)) {
            logger.warn("[SECURITY_ALERT] event=UNAUTHORIZED_ROSTER_MUTATION actor_role={} staff_id={} target_ward={}", userRole, id, newWardId);
            return false;
        }

        StaffAssignment existing = staffMap.get(id);
        if (existing == null) {
            return false;
        }

        StaffAssignment updated = new StaffAssignment(existing.staffId(), existing.name(), existing.role(), newWardId);

        staffMap.put(id, updated);

        logger.info("[PAM_AUDIT] event=STAFF_REASSIGNED actor_role={} staff_id={} new_ward={}", 
                userRole, id, newWardId);
        return true;
    }

    public static Map<String, StaffAssignment> getStaffMap() {
        return staffMap;

    }
 }

// MQ TODO: publishes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
