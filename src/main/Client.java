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

public class Client extends Thread {
    private ArrayList<BCSTMessage> broadcastmessages;
    private ArrayList<PMSGMessage> privatemessages;
    private ArrayList<BCSTMessage> groupMessages;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private Scanner scanner;
    private boolean runApp;
    private boolean messageConfirmed = false;
    private String group;

    private Reader readerThread;

    public Client(String hostURL, int portNumber) {
        try {
            socket = new Socket(hostURL, portNumber);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println("Something went wrong with the server.");
            e.printStackTrace();
        }

        this.start();
    }

    public void run() {
        if (handshake()) {
            boolean loggedIn = false;
            while (!loggedIn) {
                System.out.println("Please enter your username (only characters, numbers and underscores are allowed): ");
                scanner = newScanner();
                String username = scanner.nextLine();
                sendToServer(new HELOMessage(username));

                Message message = Message.create(readFromServer());

                if (message instanceof OKMessage) {
                    OKMessage okMessage = (OKMessage) message;
                    if (okMessage.getConfirmedMessage() instanceof HELOMessage) {
                        if (okMessage.getConfirmedMessage().getContent().equals(username)) {
                            System.out.println("Login Successful.");
                            loggedIn = true;
                            runApp = true;
                            runClient(username);
                        } else {
                            terminateConnection();
                        }
                    } else {
                        terminateConnection();
                    }
                } else if (message instanceof ERRMessage) {
                    System.out.println("Error occured:" + message.getContent());
                }
            }
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

    private synchronized void runClient(String username) {
        broadcastmessages = new ArrayList<>();
        privatemessages = new ArrayList<>();
        groupMessages= new ArrayList<>();

        this.readerThread = new Reader();
        readerThread.start();

        while (runApp) {
            messageConfirmed = false;
            System.out.println("Logged in as: " + username);
            System.out.println(" 1. Read broadcasted messages. (" +broadcastmessages.size()+")" );
            System.out.println(" 2. Broadcast a message.");
            System.out.println(" 3. User Messages menu.");
            System.out.println(" 4. Group Messages menu.");
            System.out.println(" 0. Quit.");
            System.out.println(" Select a menu option:");

            scanner = newScanner();
            String menuChoice = scanner.nextLine();

            if (menuChoice.equals("1")) {
                printBroadcastedMessages();
            } else if (menuChoice.equals("2")) {
                broadcastMessageToAll();
                if (!messageConfirmed) {
                    runApp = false;
                    readerThread.setInactive();
                    terminateConnection();
                }
            } else if (menuChoice.equals("3")) {
                System.out.println("Interact with other users.");
                System.out.println(" 1. read private messages.");
                System.out.println(" 2. Get list of online users.");
                System.out.println(" 3. Send private message to another user.");
                System.out.println("Select a menu option:");

                scanner = newScanner();
                String menuChoiceUser = scanner.nextLine();

                if (menuChoiceUser.equals("1")) {
                    readMyPrivateMessages();
                } else if (menuChoiceUser.equals("2")) {
                    getOnlineUsernames();
                    checkMessageConfirmation();
                } else if (menuChoiceUser.equals("3")) {
                    sendPrivateMessage();
                    checkMessageConfirmation();
                }
            } else if (menuChoice.equals("4")) {
                String currentgroup= "";
                if (group != null) {
                    currentgroup = group;
                }
                System.out.println("Interact in groups.");
                System.out.println("your current group: " + currentgroup);
                System.out.println(" 1. Get list of available groups.");
                System.out.println(" 2. Create a group.");
                System.out.println(" 3. Join a group. ");
                System.out.println(" 4. Read group broadcasted messages. ("+groupMessages.size()+")");
                System.out.println(" 5. Broadcast to members of the group.");
                System.out.println(" 6. Leave Current Group. ");
                System.out.println(" 7. Kick a member out.");
                System.out.println("Select a menu option:");

                scanner = newScanner();
                String menuChoiceGroup = scanner.nextLine();

                if (menuChoiceGroup.equals("1")) {
                    getAvailableGroups();
                    checkMessageConfirmation();
                } else if (menuChoiceGroup.equals("2")) {
                    createNewGroup();
                    checkMessageConfirmation();
                } else if (menuChoiceGroup.equals("3")) {
                    joinAGroup();
                    checkMessageConfirmation();
                } else if (menuChoiceGroup.equals("4")) {
                    readGroupBroadcastedMessages();
                } else if (menuChoiceGroup.equals("5")) {
                    broadcastToGroup();
                    checkMessageConfirmation();
                } else if (menuChoiceGroup.equals("6")) {
                    leaveGroup();
                    checkMessageConfirmation();
                    groupMessages.clear();
                } else if (menuChoiceGroup.equals("7")) {
                    kickAGroupMember();
                    checkMessageConfirmation();
                }
            } else if (menuChoice.equals("0")) {
                sendToServer(new QUIT());
                runApp = false;
            } else {
                System.out.println("Invalid menu choice. Please try again.");
            }
        }
    }

    private void checkMessageConfirmation() {
        if (!messageConfirmed) {
            runApp = false;
            readerThread.setInactive();
            terminateConnection();
        }
    }

    private void printBroadcastedMessages() {
        if (!broadcastmessages.isEmpty()) {
            System.out.println("Broadcasted messages:");
            for (BCSTMessage bcstMessage : broadcastmessages) {
                String message = "";
                String[] broadcastedmessage = bcstMessage.getContent().split(" ");
                message += broadcastedmessage[0] + ": ";
                for (int i = 1; i < broadcastedmessage.length - 1; i++) {
                    message += broadcastedmessage[i] + " ";
                }
                message += broadcastedmessage[broadcastedmessage.length - 1];
                System.out.println(message);
            }
            broadcastmessages.clear();
        } else {
            System.out.println("No unread broadcast messages yet.");
        }
    }

    private void broadcastMessageToAll() {
        System.out.println("Enter the message that you would like to be broadcast:");
        scanner = newScanner();
        BCSTMessage message = new BCSTMessage(scanner.nextLine());
        sendToServer(message);

        try {
            wait(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void readMyPrivateMessages() {
        if (!privatemessages.isEmpty()) {
            System.out.println("Your Private Messages:");
            for (PMSGMessage privateMessage : privatemessages) {
                System.out.println(privateMessage.getUsername() + ": " + privateMessage.getPrivateMessage());
            }
            privatemessages.clear();
        } else {
            System.out.println("No unread private messages yet.");
        }
    }

    private void getOnlineUsernames() {
        sendToServer(new USERMessage(new LISTMessage()));
        try {
            wait(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendPrivateMessage() {
        System.out.println("Enter the designated user's username:");
        scanner = newScanner();
        String designatedUsername = scanner.nextLine();
        System.out.println("Enter the  message do you want to send to this user: ");
        scanner = newScanner();
        String message = scanner.nextLine();

        sendToServer(new USERMessage(new PMSGMessage(designatedUsername, message)));

        try {
            wait(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createNewGroup() {
        System.out.println("Please provide the group name:");

        scanner = newScanner();
        String groupname = scanner.nextLine();

        sendToServer(new GROUPMessage(new CREATEMessage(groupname)));

        try {
            wait(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void getAvailableGroups() {
        sendToServer(new GROUPMessage(new LISTMessage()));

        try {
            wait(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void joinAGroup() {
        System.out.println("Please enter the name of the group that you want to join:");
        scanner = newScanner();

        sendToServer(new GROUPMessage(new JOINMessage(scanner.nextLine())));

        try {
            wait(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void readGroupBroadcastedMessages() {
        if (!groupMessages.isEmpty()) {
            System.out.println("Current group: " + group);
            System.out.println("Broadcasted messages:");
            for (BCSTMessage bcstMessage : groupMessages) {
                String message = "";
                String[] broadcastedmessage = bcstMessage.getContent().split(" ");
                message += broadcastedmessage[0] + ": ";
                for (int i = 1; i < broadcastedmessage.length - 1; i++) {
                    message += broadcastedmessage[i] + " ";
                }
                message += broadcastedmessage[broadcastedmessage.length - 1];
                System.out.println(message);
            }
            groupMessages.clear();
        } else {
            System.out.println("No unread Group broadcast messages yet.");
        }
    }

    private void leaveGroup() {
        sendToServer(new GROUPMessage(new LEAVEMEssage()));

        try {
            wait(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void broadcastToGroup() {
        System.out.println("Please enter the message that you want to broadcast to your group:");
        scanner = newScanner();

        sendToServer(new GROUPMessage(new BCSTMessage(scanner.nextLine())));
        try {
            wait(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void kickAGroupMember() {
        System.out.println("Please enter the name of the member that you want to kick: ");
        scanner = newScanner();

        sendToServer(new GROUPMessage(new KICKMessage(scanner.nextLine())));

        try {
            wait(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendToServer(Message message) {
        System.out.println("sent:" + message.toStringForm());
        writer.println(message.toStringForm());
    }

    public String readFromServer() {
        try {
            String message = reader.readLine();
            System.out.println("received: " +message);
            return message;
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

    public synchronized void notifyMessageAnswered() {
        messageConfirmed = true;
        this.notify();
    }

    class Reader extends Thread {
        private boolean activeread;

        public Reader() {
            activeread = true;
        }

        public void run() {
            while (activeread) {
                Message message = Message.create(readFromServer());
                if (message instanceof OKMessage) {
                    Message confirmedMessage = ((OKMessage) message).getConfirmedMessage();
                    if (confirmedMessage instanceof BCSTMessage) {
                        System.out.println("Successfully broadcasted: " + ((OKMessage) message).getConfirmedMessage().getContent());
                        notifyMessageAnswered();
                    } else if (confirmedMessage instanceof USERMessage) {
                        Message containedMessage = ((USERMessage) confirmedMessage).getUserOrientedMessage();
                        if (containedMessage instanceof LISTMessage) {
                            String[] usernames = containedMessage.getContent().split(" ");
                            System.out.println("Online users: ");
                            for (int i = 0; i < usernames.length; i++) {
                                System.out.println(" - " + usernames[i]);
                            }
                            notifyMessageAnswered();
                        } else if (containedMessage instanceof PMSGMessage) {
                            System.out.println("Message :" + ((PMSGMessage) containedMessage).getPrivateMessage());
                            System.out.println("Has been sent to user: " + ((PMSGMessage) containedMessage).getUsername());
                            notifyMessageAnswered();
                        }
                    } else if (confirmedMessage instanceof GROUPMessage) {
                        Message containedMessage = ((GROUPMessage) confirmedMessage).getGroupOrientedMessage();
                        if (containedMessage instanceof CREATEMessage) {
                            System.out.println("The group " + containedMessage.getContent() + " has been created");
                            group = containedMessage.getContent();
                            notifyMessageAnswered();
                        } else if (containedMessage instanceof LISTMessage) {
                            String[] groupnames = containedMessage.getContent().split(" ");
                            System.out.println("Available groups:");
                            for (int i = 0; i < groupnames.length; i++) {
                                System.out.println(" - " + groupnames[i]);
                            }
                            notifyMessageAnswered();
                        } else if (containedMessage instanceof JOINMessage) {
                            group = containedMessage.getContent();
                            System.out.println("Joined group " + group);
                            notifyMessageAnswered();
                        } else if (containedMessage instanceof BCSTMessage) {
                            System.out.println("Succesfully broadcasted: " + containedMessage.getContent() + " to group: " + group);
                            notifyMessageAnswered();
                        } else if (containedMessage instanceof LEAVEMEssage) {
                            group = null;
                            System.out.println("You have left your currently joined group ");
                            notifyMessageAnswered();
                        } else if (containedMessage instanceof KICKMessage) {
                            System.out.println("You have successfully kicked " + containedMessage.getContent() + " out of your group");
                            notifyMessageAnswered();
                        }
                    }
                    if ((message.getContent().equals("goodbye"))) {
                        System.out.println("Disconnected from server: " + message.getContent());
                        runApp = false;
                        this.activeread = false;
                        terminateConnection();
                    }
                } else if (message instanceof BCSTMessage) {
                    broadcastmessages.add((BCSTMessage) message);
                } else if (message instanceof PMSGMessage) {
                    privatemessages.add((PMSGMessage) message);
                } else if (message instanceof GROUPMessage) {
                    Message containedMessage = ((GROUPMessage) message).getGroupOrientedMessage();
                    if (containedMessage instanceof BCSTMessage) {
                        groupMessages.add((BCSTMessage) containedMessage);
                    }
                } else if (message instanceof PING) {
                    sendToServer(new PONG());
                } else if (message instanceof ERRMessage) {
                    System.out.println("Error occured:" + message.getContent());
                    notifyMessageAnswered();
                } else if (message instanceof DSCN) {
                    System.out.println("Disconnected from server: " + message.getContent());
                    runApp = false;
                    this.activeread = false;
                    terminateConnection();
                }
            }
        }

        public void setInactive() {
            activeread = false;
        }
    }
}
