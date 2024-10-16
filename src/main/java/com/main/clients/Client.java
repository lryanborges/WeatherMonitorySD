package com.main.clients;

import com.rabbitmq.client.*;

import java.util.*;

public class Client {

    private static final String DATACENTER_CONN = "clientsConnection";

    private static final Scanner scan = new Scanner(System.in);

    public static void main(String[] args) throws Exception {

        Set<String> options = subscribeMonitory();

        ConnectionFactory fabrica = new ConnectionFactory();
        fabrica.setHost("localhost");
        Connection conexao = fabrica.newConnection();
        Channel canal = conexao.createChannel();

        canal.exchangeDeclare(DATACENTER_CONN, BuiltinExchangeType.TOPIC);
        String nomeFila = canal.queueDeclare().getQueue();

        System.out.println(" [*] Esperando mensagens...");

        for(String routingKey : options){
            canal.queueBind(nomeFila, DATACENTER_CONN, routingKey);

            DeliverCallback callbackEntrega = (tagConsumidor, entrega) -> {
                String mensagem = new String(entrega.getBody(), "UTF-8");
                //Float data = objectMapper.readValue(entrega.getBody(), Float.class);
                System.out.println(" Recebida <-- \"" + entrega.getEnvelope().getRoutingKey() + "\" : " + mensagem);
            };
            canal.basicConsume(nomeFila, true, callbackEntrega, tagConsumidor -> { });
        }

    }
    
    private static Set<String> subscribeMonitory() {
        Set<String> options = new HashSet<>();

        System.out.println("--------------------------------------");
        System.out.println("\t\t\tMonitorar dados");
        System.out.println("--------------------------------------");
        System.out.println("[1] - Pressão atmosférica");
        System.out.println("[2] - Radiação solar");
        System.out.println("[3] - Temperatura");
        System.out.println("[4] - Umidade");
        System.out.println("[5] - Todas os dados");
        System.out.println("[0] - Iniciar a monitoria");
        System.out.println("--------------------------------------");
        System.out.print("Opções: ");

        int opc = 0;
        do {
            opc = scan.nextInt();
            switch (opc) {
                case 1:
                    options.add("atmosphericPressure");
                    break;
                case 2:
                    options.add("solarRadiation");
                    break;
                case 3:
                    options.add("temperature");
                    break;
                case 4:
                    options.add("humidity");
                    break;
                case 5:
                    options.add("atmosphericPressure");
                    options.add("solarRadiation");
                    options.add("temperature");
                    options.add("humidity");
                    break;
                case 0:
                    break;
                default:
                    System.out.println("Opção inválida!");
            }

        } while(opc != 0);

        return options;

    }

}
