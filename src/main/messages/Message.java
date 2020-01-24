package main.messages;


public abstract class Message {
    static public Message create(String line) {
        if(line != null) {
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
                case "USER":
                    return new USERMessage(content);
                case "LIST":
                    return new LISTMessage(content);
                case "PMSG":
                    return new PMSGMessage(content);
                case "GROUP":
                    return new GROUPMessage(content);
                case "CREATE":
                    return new CREATEMessage(content);
                case "JOIN":
                    return new JOINMessage(content);
                case "LEAVE":
                    return new LEAVEMEssage();
                case "KICK":
                    return new KICKMessage(content);
            }
        }
        return null;
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
