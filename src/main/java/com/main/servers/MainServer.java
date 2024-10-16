package com.main.servers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.main.drones.EnvironmentRead;
import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class MainServer {

    // database
    private static final Set<EnvironmentRead> database = new HashSet<EnvironmentRead>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();

    private static final String CONNECTION_NAME = "droneConnection";
    private static final String SERVERS_CONN = "serversConnection";
    private static final String CLIENTS_CONN = "clientsConnection";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection conn = factory.newConnection();

        final Channel channel = conn.createChannel(); // canal de comunic. drone -> mainServer -> servers

        final Channel clientsChannel = conn.createChannel(); // canal de comunic. mainServer -> client

        channel.queueDeclare(CONNECTION_NAME, true, false, false, null);
        System.out.println(" [*] Esperando mensagens...");

        channel.basicQos(1);

        DeliverCallback callbackEntrega = (tagConsumidor, environmentRead) -> {

            // recebe a mensagem pelo corpo
            String jsonMessage = new String(environmentRead.getBody(), StandardCharsets.UTF_8);
            System.out.println("Recebida <-- " + jsonMessage);

            //verifyStop(jsonMessage);

            try {
                // salvar no seu bd
                EnvironmentRead lastRead = objectMapper.readValue(environmentRead.getBody(), EnvironmentRead.class);
                database.add(lastRead); // add na base de dados

                // enviar pros outros servidores pelo canal
                sendToServers(channel, jsonMessage);

                // enviar pros canais de topico
                clientsChannel.exchangeDeclare(CLIENTS_CONN, BuiltinExchangeType.TOPIC);

                String atPress = objectWriter.writeValueAsString(lastRead.getAtmosphericPressure());
                clientsChannel.basicPublish(CLIENTS_CONN,
                        "atmosphericPressure", null, atPress.getBytes(StandardCharsets.UTF_8));

                String solRad = objectWriter.writeValueAsString(lastRead.getSolarRadiation());
                clientsChannel.basicPublish(CLIENTS_CONN,
                        "solarRadiation", null, solRad.getBytes(StandardCharsets.UTF_8));

                String temp = objectWriter.writeValueAsString(lastRead.getTemperature());
                clientsChannel.basicPublish(CLIENTS_CONN,
                        "temperature", null, temp.getBytes(StandardCharsets.UTF_8));

                String hum = objectWriter.writeValueAsString(lastRead.getHumidity());
                clientsChannel.basicPublish(CLIENTS_CONN,
                        "humidity", null, hum.getBytes(StandardCharsets.UTF_8));

            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                System.out.println("------------------------------------------");
                channel.basicAck(environmentRead.getEnvelope().getDeliveryTag(), false); // tira da fila do RabbitMQ
            }

        };

        channel.basicConsume(CONNECTION_NAME, false, callbackEntrega, tagConsumidor -> { });

    }

    private static void sendToServers(Channel channel, String message) throws Exception {
        // enviar pros outros dois servidores
        channel.exchangeDeclare(SERVERS_CONN, "direct");

        String[] serverKeys = {"server2", "server3"};
        for (String serverKey : serverKeys) {
            // publica a mensagem com uma chave de roteamento específica
            channel.basicPublish(SERVERS_CONN, serverKey, null, message.getBytes("UTF-8"));
            System.out.println(" --> Encaminhado ao servidor com chave: '" + serverKey + "'");
        }
    }

    /*
    private static void verifyStop(String msg) {
        if(msg.equals("drone read done")) {
            channel.basicAck(environmentRead.getEnvelope().getDeliveryTag(), false); // tira da fila do RabbitMQ
            System.exit(0);
        }
    } */

}
