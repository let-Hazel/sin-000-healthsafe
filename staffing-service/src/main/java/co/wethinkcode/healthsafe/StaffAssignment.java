package co.wethinkcode.healthsafe;

public record StaffAssignment(
    String staffId,
    String name,
    String role,
    String assignedWardId
) {}