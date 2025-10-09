package com.example;

public class ProposalNumber {
    public long Counter;
    public String MemberID;

    public ProposalNumber(long Counter, String MemberID){
        this.Counter = Counter;
        this.MemberID = MemberID;
    }

    public static ProposalNumber parse(String NewNum){
        if (NewNum == null) return null;
        String[] Parts = NewNum.split("-", 2);
        if(Parts.length != 2) return null;
        return new ProposalNumber(Long.parseLong(Parts[0]), Parts[1]);
    }

    public int compareTo(ProposalNumber Num){
        if(Num == null) return 1;
        int C = Long.compare(this.Counter, Num.Counter);
        if(C != 0) return C;
        return this.MemberID.compareTo(Num.MemberID);
    }
}
