package com.main.servers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.main.drones.EnvironmentRead;
import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class Server {

    // database
    private static final Set<EnvironmentRead> database = new HashSet<EnvironmentRead>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String MAINSERVER_CONN = "serversConnection";
    private static final int serverNumber = 2;
    private static final String serverKey = "server" + serverNumber;

    public static void main(String[] args) throws Exception {

        System.out.println("Server " + serverNumber + " iniciado.");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        final Channel channel;
        Connection conn = factory.newConnection();
        channel = conn.createChannel();

        channel.exchangeDeclare(MAINSERVER_CONN, "direct");

        // criar uma fila temporária p vincular à exchange com a chave dos servers
        String temporaryQueue = channel.queueDeclare().getQueue();
        channel.queueBind(temporaryQueue, MAINSERVER_CONN, serverKey);

        System.out.println(" [*] Esperando mensagens...");

        DeliverCallback callbackEntrega = (tagConsumidor, environmentRead) -> {
            // essa aq de baixo é p transformar em de json p objeto e ler os valores individualmente mais fácil
            //EnvironmentRead lastRead = objectMapper.readValue(environmentRead.getBody(), EnvironmentRead.class);
            String jsonMessage = new String(environmentRead.getBody(), StandardCharsets.UTF_8);
            System.out.println(" Recebida <-- " + jsonMessage);

            try {
                // salvar no seu bd
                EnvironmentRead lastRead = objectMapper.readValue(environmentRead.getBody(), EnvironmentRead.class);
                database.add(lastRead); // add na base de dados

            } finally {
                System.out.println("------------------------------------------");
                // faz o reconhecimento das mensagens da fila vinculada
                channel.basicAck(environmentRead.getEnvelope().getDeliveryTag(), false); // tira da fila do RabbitMQ
            }

        };

        // consome as mensagens da fila vinculada
        channel.basicConsume(temporaryQueue, false, callbackEntrega, consumerTag -> { });

    }

}
