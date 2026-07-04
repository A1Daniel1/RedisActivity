package edu.eci.redisconsumer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.resps.StreamEntryInfo;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class RedisConsumerApplication implements CommandLineRunner {

    private static final String STREAM_KEY = "banco.transferencias";
    private static final String GROUP_NAME = "auditoria-group";
    private static final String CONSUMER_NAME = "consumidor-auditor-1";

    private final String redisHost;
    private final int redisPort;

    public RedisConsumerApplication() {
        this.redisHost = System.getProperty("redis.host", "localhost");
        this.redisPort = Integer.parseInt(System.getProperty("redis.port", "6379"));
    }

    public static void main(String[] args) {
        SpringApplication.run(RedisConsumerApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            System.out.println("Consumidor conectado a Redis en " + redisHost + ":" + redisPort);

            try {
                jedis.xgroupCreate(STREAM_KEY, GROUP_NAME, StreamEntryID.LAST_ENTRY, true);
                System.out.println("Grupo '" + GROUP_NAME + "' creado en el stream '" + STREAM_KEY + "'");
            } catch (Exception e) {
                System.out.println("El grupo '" + GROUP_NAME + "' ya existe");
            }

            System.out.println("Esperando eventos...\n");

            while (true) {
                List<Map.Entry<String, List<StreamEntryInfo>>> results = jedis.xreadGroup(
                        GROUP_NAME, CONSUMER_NAME, 1, 5000, false,
                        new AbstractMap.SimpleEntry<>(STREAM_KEY, StreamEntryInfo.UNDELIVERED_ENTRY)
                );

                if (results != null && !results.isEmpty()) {
                    for (var streamResult : results) {
                        for (StreamEntryInfo entry : streamResult.getValue()) {
                            String eventId = entry.getFields().get("eventId");
                            String transferId = entry.getFields().get("transferId");
                            String amount = entry.getFields().get("amount");
                            String currency = entry.getFields().get("currency");

                            System.out.println("Procesando transferencia:");
                            System.out.println("  Evento:     " + eventId);
                            System.out.println("  Transfer:   " + transferId);
                            System.out.println("  Monto:      " + amount + " " + currency);

                            jedis.xack(STREAM_KEY, GROUP_NAME, entry.getID());
                            System.out.println("  -> Evento " + entry.getID() + " confirmado (XACK)\n");
                        }
                    }
                }
            }
        }
    }
}
