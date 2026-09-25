package co.wethinkcode.healthsafe.mq;

public final class MqConfig {

    public static final String BROKER_URL = "tcp://localhost:61616";
    public static final String QUEUE = "equipment-failure-queue";

    private MqConfig() {
    }
}