package com.main.drones;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Drone {

    private static final SecureRandom random = new SecureRandom();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
    private static final String DATACENTER_CONN = "droneConnection";
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private static int readCount = 1;
    private static final int mins = 1;
    private static final Scanner scan = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        CountDownLatch latch = new CountDownLatch(1); // Para sincronizar o término

        try (Connection conn = factory.newConnection();
             Channel canal = conn.createChannel()) {

            canal.queueDeclare(DATACENTER_CONN, true, false, false, null);

            System.out.println("\t\t\t\tDrone");
            System.out.println("--------------------------------------");
            System.out.println("Aperte Enter para iniciar a coleta de dados.");
            System.out.println("--------------------------------------");
            scan.nextLine();

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    EnvironmentRead currentRead = environmentRead();
                    String jsonMessage = objectWriter.writeValueAsString(currentRead);

                    canal.basicPublish("", DATACENTER_CONN,
                            MessageProperties.PERSISTENT_TEXT_PLAIN,
                            jsonMessage.getBytes(StandardCharsets.UTF_8));
                    System.out.println("Envio " + readCount++ + " --> " + jsonMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, 0, 3, TimeUnit.SECONDS);

            // definindo tempo da simulação
            scheduler.schedule(() -> {
                try {
                    Thread.sleep(1000);
                    scheduler.shutdown(); // encerra o scheduler
                    latch.countDown(); // p decrescentar o countdownlatch p liberar a aplicação
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, mins, TimeUnit.MINUTES);

            latch.await();

            /* canal.basicPublish("", DATACENTER_CONN,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    "drone read done".getBytes(StandardCharsets.UTF_8)); */

            System.out.println("------------------------------------------");
            System.out.println("Tempo de simulação (" + mins + "min) encerrado.");
            System.out.println("------------------------------------------");
        }

    }

    private static EnvironmentRead environmentRead() {
        EnvironmentRead currentRead = new EnvironmentRead();

        float atmosphericPressure = 950 + random.nextFloat() * (1050 - 950);
        float solarRadiation = roundFloat(random.nextFloat() * 1200);
        float temperature = roundFloat(-30 + random.nextFloat() * (50 - (-30)));
        float humidity = roundFloat(random.nextFloat() * 100);

        currentRead.setAtmosphericPressure(roundFloat(atmosphericPressure));
        currentRead.setSolarRadiation(roundFloat(solarRadiation));
        currentRead.setTemperature(roundFloat(temperature));
        currentRead.setHumidity(roundFloat(humidity));

        return currentRead;
    }


    private static float roundFloat(float value) {
        BigDecimal bd = new BigDecimal(Float.toString(value));
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP); // Arredonda para 2 casas decimais

        return bd.floatValue();
    }

}
