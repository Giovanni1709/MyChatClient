package main.messages;

public class USERMessage extends Message {
    private Message userOrientedMessage;

    public USERMessage(String content) {
        super("USER", content);

        userOrientedMessage = Message.create(content);
    }

    public USERMessage(Message message) {
        super("USER", message.toStringForm());
    }

    public Message getUserOrientedMessage() {
        return userOrientedMessage;
    }
}
