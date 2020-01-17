package main;

import main.messages.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

public class Main {
    private ArrayList<BCSTMessage> broadcastmessages;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private Scanner scanner;
    private boolean runApp;

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        try {
            socket = new Socket("127.0.0.1", 1337);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println("Something went wrong with the server.");
            e.printStackTrace();
        }

        if (handshake()) {
            runApp = true;
            startClient();
        } else {
            terminateConnection();
        }
    }

    public boolean handshake() {
        Message message = Message.create(readFromServer());
        if (message instanceof Message) {
            System.out.println(message.getContent());
            return true;
        }
        System.out.println("Wrong protocol.");
        System.out.println("Handshake failed.");
        return false;
    }

    public void startClient() {
        while (runApp) {
            System.out.println("Please enter your username (only characters, numbers and underscores are allowed): ");
            scanner = newScanner();
            String username = scanner.nextLine();

            HELOMessage firstReply = new HELOMessage(username);
            System.out.println(firstReply.toStringForm());
            sendToServer(firstReply);
            Message message = Message.create(readFromServer());
            System.out.println(message.toStringForm());

            if (message instanceof OKMessage) {
                OKMessage okMessage = (OKMessage) message;
                if (okMessage.getContent().equals(firstReply.toStringForm())) {
                    System.out.println("Login successful.");
                    broadcastmessages = new ArrayList<>();
                    new Reader().start();
                    while (runApp) {
                        System.out.println("    Logged in as: " + username);
                        System.out.println(" 1. Read broadcasted messages.");
                        System.out.println(" 2. Broadcast a message.");
                        System.out.println(" 0. Quit.");
                        System.out.println(" Select a menu option:");

                        scanner = newScanner();
                        int menuChoice = scanner.nextInt();
                        boolean handled = false;
                        switch (menuChoice) {
                            case 1: {
                                if (!broadcastmessages.isEmpty()) {
                                    System.out.println("Broadcasted messages:");
                                    for (BCSTMessage bcstMessage : broadcastmessages) {
                                        System.out.println();
                                    }
                                }else {
                                    System.out.println("No unread broadcast messages yet.");
                                }
                                break;
                            }
                            case 2:{
                                scanner = newScanner();
                                String broadcastMessage = scanner.nextLine();
                                sendToServer(new BCSTMessage(broadcastMessage));
                                break;
                            }
                            case 0:{
                                sendToServer(new QUIT());
                            }
                        }
                        if (!handled) {
                            System.out.println("Invalid menu choice. Please try again.");
                        }
                    }
                }
            } else if (message.getType().equals("-ERR")) {
                System.out.println("Error: " + message.getContent() + ".");
            } else {
                runApp = false;
                terminateConnection();
            }
        }
    }


    public synchronized void sendToServer(Message message) {
        writer.println(message.toStringForm());
    }

    public synchronized String readFromServer() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void terminateConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Socket failed to close.");
            e.printStackTrace();
        }
        System.out.println("Socket closed, connection successfully terminated.");
    }

    public Scanner newScanner() {
        return new Scanner(System.in);
    }

    class Reader extends Thread {
        private boolean activeread ;

        public Reader() {
            activeread = true;
        }

        public void run() {
            while (activeread) {
                Message message = Message.create(readFromServer());
                if (message instanceof OKMessage) {
                    if (((OKMessage) message).getConfirmedMessage() instanceof BCSTMessage) {
                        System.out.println("Successfully broadcasted:" + ((OKMessage) message).getConfirmedMessage().getContent());
                    } else {

                    }
                } else if (message instanceof BCSTMessage){
                    broadcastmessages.add((BCSTMessage) message);
                } else if (message instanceof PING) {
                    writer.println(new PONG());
                } else if (message instanceof ERRMessage) {
                    System.out.println("Error occured:" + message.getContent());
                } else if (message instanceof DSCN) {
                    System.out.println("Disconnected from server.");
                    runApp = false;
                    terminateConnection();
                }
            }
        }
    }
}
