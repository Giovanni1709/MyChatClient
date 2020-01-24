package main.messages;

public class CREATEMessage extends Message {
    public CREATEMessage(String content) {
        super("CREATE", content);
    }
}
