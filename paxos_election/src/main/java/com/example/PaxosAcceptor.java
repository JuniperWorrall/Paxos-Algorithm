package com.example;

import java.util.concurrent.atomic.AtomicReference;


public class PaxosAcceptor {
    private final CouncilMember Node;

    private final AtomicReference<ProposalNumber> Promised = new AtomicReference<>(null);

    private final AtomicReference<ProposalNumber> AcceptedNum = new AtomicReference<>(null);
    private volatile String AcceptedValue = null;

    public PaxosAcceptor(CouncilMember Node) {
        this.Node = Node;
    }

    public void HandlePrepare(Message Msg) {
        ProposalNumber Incoming = ProposalNumber.Parse(Msg.ProposalNum);
        if (Incoming == null) return;

        if (!DelaySimulator.ApplyDelay(Node.Profile)) return;

        synchronized (this) {
            ProposalNumber CurrentPromised = Promised.get();
            if (CurrentPromised == null || Incoming.CompareTo(CurrentPromised) > 0) {
                Promised.set(Incoming);
                Message Promise = new Message("PROMISE", Node.MemberID,
                        Incoming.toString(),
                        AcceptedNum.get() == null ? null : AcceptedNum.get().toString(),
                        AcceptedValue);
                Node.SendMessage(Msg.Sender, Promise);
            } else {
            }
        }
    }

    public void HandleAcceptRequest(Message Msg) {
        ProposalNumber Incoming = ProposalNumber.Parse(Msg.ProposalNum);
        if (Incoming == null) return;

        if (!DelaySimulator.ApplyDelay(Node.Profile)) return;

        synchronized (this) {
            ProposalNumber CurrentPromised = Promised.get();
            if (CurrentPromised == null || Incoming.CompareTo(CurrentPromised) >= 0) {
                Promised.set(Incoming);
                AcceptedNum.set(Incoming);
                AcceptedValue = Msg.Value;
                Message Accepted = new Message("ACCEPTED", Node.MemberID,
                        Incoming.toString(),
                        AcceptedNum.get() == null ? null : AcceptedNum.get().toString(),
                        AcceptedValue);
                Node.SendMessage(Msg.Sender, Accepted);
                Node.Learner.HandleAccepted(Accepted);
            } else {
            }
        }
    }
}