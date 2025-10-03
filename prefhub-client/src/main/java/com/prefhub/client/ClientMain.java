package com.prefhub.client;

import com.prefhub.client.ui.ConsoleUI;

public class ClientMain {
    public static void main(String[] args) {
        String serverUrl = args.length > 0 ? args[0] : "http://localhost:8080";

        System.out.println("Подключение к серверу: " + serverUrl);
        ConsoleUI ui = new ConsoleUI(serverUrl);
        ui.start();
    }
}
