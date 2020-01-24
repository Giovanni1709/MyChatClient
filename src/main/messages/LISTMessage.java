package main.messages;

public class LISTMessage extends Message {
    public LISTMessage(String content) {
        super("LIST", content);
    }

    public LISTMessage() {
        super("LIST", "");
    }
}
