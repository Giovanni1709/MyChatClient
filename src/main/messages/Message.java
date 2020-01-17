package main.messages;


public abstract class Message {
    static public Message create(String line) {
        String[] messageParts = line.split(" ", 2);
        String content = "";
        if (messageParts.length > 1) {
            for (int i = 1; i < messageParts.length - 1; i++) {
                content += messageParts[i] + " ";
            }
            content += messageParts[messageParts.length - 1];
        }
        switch (messageParts[0]) {
            case "HELO":
                return new HELOMessage(content);
            case "+OK":
                return new OKMessage(content);
            case "-ERR":
                return new ERRMessage(content);
            case "PING":
                return new PING();
            case "PONG":
                return new PONG();
            case "DSCN":
                return new DSCN(content);
            case "BCST":
                return new BCSTMessage(content);
            case "QUIT":
                return new QUIT();
        }
        throw new RuntimeException("invalid message type " + messageParts[0]);
    }

    private String type;
    private String content;

    public Message(String type, String content){
        this.type = type;
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public String toStringForm() {
        return type + " " + content;
    }
}
