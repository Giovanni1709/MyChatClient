package main.messages;

public class KICKMessage extends Message {
    public KICKMessage( String content) {
        super("KICK", content);
    }
}
