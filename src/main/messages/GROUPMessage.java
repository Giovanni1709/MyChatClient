package main.messages;

public class GROUPMessage extends Message {
    private Message groupOrientedMessage;

    public GROUPMessage(String content) {
        super("GROUP", content);

        groupOrientedMessage = Message.create(content);
    }

    public GROUPMessage(Message message) {
        super("GROUP", message.toStringForm());
    }

    public Message getGroupOrientedMessage() {
        return groupOrientedMessage;
    }
}
