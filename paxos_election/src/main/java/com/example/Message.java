package com.example;

public class Message {
    public String Type;
    public String Sender;
    public String ProposalNum;
    public String AcceptedNum;
    public String Value;

    public Message(String Type, String Sender, String ProposalNum, String AcceptedNum, String Value){
        this.Type = Type;
        this.Sender = Sender;
        this.ProposalNum = ProposalNum;
        this.AcceptedNum = AcceptedNum;
        this.Value = Value == null ? "null" : Value;
    }

    public static Message FromNode(String line) {
        if(line == null) return null;
        String[] p = line.split("//|", -1);
        if(p.length < 5) return null;
        return new Message(p[0], p[1], Safe(p[2]), Safe(p[3]), Safe(p[4]));
    }

    public String ToNode() {
        return String.join("|",
        Safe(Type),
        Safe(Sender),
        Safe(ProposalNum),
        Safe(AcceptedNum),
        Safe(Value));
    }

    private static String Safe(String s){
        return s == null ? "null" : s;
    }
}
