package main;

public class Main {
    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        Client client = new Client("127.0.0.1", 1337);
    }
}
