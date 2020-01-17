package main.messages;

public class ERRMessage extends Message {
    public ERRMessage( String content) {
        super( "-ERR", content);
    }
}
