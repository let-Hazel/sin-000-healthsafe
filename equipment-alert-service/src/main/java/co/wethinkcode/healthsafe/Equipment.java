package co.wethinkcode.healthsafe;

public record Equipment(
    String equipmentId,
    String name,
    String type,
    String wardId,
    String status
) {}