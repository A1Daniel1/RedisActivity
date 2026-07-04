package edu.eci.redisproducer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.StreamEntryID;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SpringBootApplication
public class RedisProducerApplication implements CommandLineRunner {

    private static final String STREAM_KEY = "banco.transferencias";

    private final String redisHost;
    private final int redisPort;

    public RedisProducerApplication() {
        this.redisHost = System.getProperty("redis.host", "localhost");
        this.redisPort = Integer.parseInt(System.getProperty("redis.port", "6379"));
    }

    public static void main(String[] args) {
        SpringApplication.run(RedisProducerApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            System.out.println("Productor conectado a Redis en " + redisHost + ":" + redisPort);

            while (true) {
                Map<String, String> fields = new HashMap<>();
                fields.put("eventType", "TransferenciaCreada");
                fields.put("eventId", "evt-" + UUID.randomUUID().toString().substring(0, 8));
                fields.put("transferId", "tr-" + (int) (Math.random() * 10000));
                fields.put("amount", String.valueOf((int) (Math.random() * 10000000)));
                fields.put("currency", Math.random() > 0.5 ? "COP" : "USD");

                StreamEntryID id = jedis.xadd(STREAM_KEY, StreamEntryID.NEW_ENTRY, fields);
                System.out.println("Publicado evento " + fields.get("eventId")
                        + " | transferId=" + fields.get("transferId")
                        + " | monto=" + fields.get("amount") + " " + fields.get("currency")
                        + " | ID=" + id);

                Thread.sleep(3000);
            }
        }
    }
}
