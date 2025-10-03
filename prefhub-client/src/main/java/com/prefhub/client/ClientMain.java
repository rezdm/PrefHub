package com.prefhub.client;

import com.prefhub.client.ui.ConsoleUI;

public class ClientMain {
    public static void main(final String[] args) {
        final var serverUrl = args.length > 0 ? args[0] : "http://localhost:8080";

        System.out.println("Подключение к серверу: " + serverUrl);
        final var ui = new ConsoleUI(serverUrl);
        ui.start();
    }
}
