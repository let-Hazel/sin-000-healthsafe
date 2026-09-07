package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

public record WardService(String wardId, String wing, String department, int bedsAvailable) {};
