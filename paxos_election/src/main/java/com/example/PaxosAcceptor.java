package com.example;

import java.util.concurrent.atomic.AtomicReference;


public class PaxosAcceptor {
    private CouncilMember Node;

    private AtomicReference<ProposalNumber> Promised = new AtomicReference<>(null);

    private AtomicReference<ProposalNumber> AcceptedProposalNum = new AtomicReference<>(null);
    private String AcceptedValue = null;

    public PaxosAcceptor(CouncilMember Node) {
        this.Node = Node;
    }

    public void HandlePrepare(Message Msg) {
        ProposalNumber Incoming = ProposalNumber.Parse(Msg.ProposalNum);
        if (Incoming == null) return;

        if (!DelaySimulator.ApplyDelay(Node.Profile)) return;

        synchronized (this) {
            if (Promised.get() == null || Incoming.CompareTo(Promised.get()) > 0) {
                Promised.set(Incoming);
                Message Promise = new Message("PROMISE", Node.MemberID,
                        Msg.ProposalNum,
                        AcceptedProposalNum.get() == null ? null : AcceptedProposalNum.get().toString(),
                        AcceptedValue);
                Node.SendMessage(Msg.Sender, Promise);
            }
        }
    }

    public void HandleAcceptRequest(Message Msg) {
        ProposalNumber Incoming = ProposalNumber.Parse(Msg.ProposalNum);
        if (Incoming == null) return;

        if (!DelaySimulator.ApplyDelay(Node.Profile)) return;

        synchronized (this) {
            if (Promised.get() == null || Incoming.CompareTo(Promised.get()) >= 0) {
                Promised.set(Incoming);
                AcceptedProposalNum.set(Incoming);
                AcceptedValue = Msg.Value;

                Message Accepted = new Message("ACCEPTED", Node.MemberID, Msg.ProposalNum, 
                AcceptedProposalNum.get() == null ? null : AcceptedProposalNum.get().toString(), AcceptedValue);
                
                for(String Target : Node.NetworkMap.keySet()){
                    Node.SendMessage(Target, Accepted);
                }
            } 
        }
    }
}