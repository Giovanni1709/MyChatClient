package main.messages;

public class OKMessage extends Message {
    private Message confirmedMessage;

    public OKMessage(String content) {
        super("+OK", content);
        if (!content.equals("goodbye")) {
            confirmedMessage = Message.create(content);
        }
    }

    public OKMessage(Message message) {
        super("+OK", message.toStringForm());
    }

    public Message getConfirmedMessage() {
        return confirmedMessage;
    }
}
