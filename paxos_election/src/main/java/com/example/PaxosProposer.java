package com.example;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


public class PaxosProposer {
    private CouncilMember Node;
    private AtomicLong Counter = new AtomicLong(0);
    private int Quorum;
    private Map<String, ProposalState> States = new ConcurrentHashMap<>();

    private long WAIT_TIMEOUT = 3000;

    public PaxosProposer(CouncilMember Node) {
        this.Node = Node;
        this.Quorum = (Node.NetworkMap.size() / 2) + 1;
    }

    public void InitiateProposal(String Value) {
        long Count = Counter.incrementAndGet();
        String ProposalID = Count + "-" + Node.MemberID;
        ProposalState st = new ProposalState(ProposalID, Value);
        States.put(ProposalID, st);

        Message Prepare = new Message("PREPARE", Node.MemberID, ProposalID, null, null);
        for (String Target : Node.NetworkMap.keySet()) {
            Node.SendMessage(Target, Prepare);
        }

        long Start = System.currentTimeMillis();
        while (System.currentTimeMillis() - Start < WAIT_TIMEOUT) {
            if (st.PromiseCount() >= Quorum) break;
            Sleep(100);
        }

        if (st.PromiseCount() < Quorum) {
            System.out.println(Node.MemberID + " -> failed to gather majority PROMISEs for " + ProposalID);
            return;
        }

        if (st.HighestAcceptedValue != null) {
            st.Value = st.HighestAcceptedValue;
        }

        Message AcceptReq = new Message("ACCEPT_REQUEST", Node.MemberID, ProposalID, null, st.Value);
        for (String Target : Node.NetworkMap.keySet()) {
            Node.SendMessage(Target, AcceptReq);
        }

        Start = System.currentTimeMillis();
        while (System.currentTimeMillis() - Start < WAIT_TIMEOUT) {
            if (st.AcceptedCount() >= Quorum) break;
            Sleep(100);
        }

        if (st.AcceptedCount() < Quorum) {
            System.out.println(Node.MemberID + " -> failed to gather majority ACCEPTED for " + ProposalID);
        } else {
            Node.Learner.HandleAccepted(new Message("ACCEPTED", Node.MemberID, ProposalID, null, st.Value));
        }
    }

    public void HandlePromise(Message Msg) {
        if (Msg == null || Msg.AcceptedProposalNum == null) return;
        ProposalState st = States.get(Msg.AcceptedProposalNum);
        if (st == null) return;

        st.AddPromise(Msg.Sender);

        if (Msg.AcceptedProposalNum != null && !Msg.AcceptedProposalNum.isEmpty() && Msg.AcceptedProposalNum.length() > 0) {
            ProposalNumber an = ProposalNumber.Parse(Msg.AcceptedProposalNum);
            if (an != null) {
                st.MaybeAdoptHigher(an, Msg.Value);
            }
        }
    }

    public void HandleAccepted(Message Msg) {
        if (Msg == null || Msg.ProposalNum == null) return;
        ProposalState st = States.get(Msg.ProposalNum);
        if (st == null) return;
        st.AddAccepted(Msg.Sender);
    }

    private void Sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static class ProposalState {
        String ProposalID;
        String Value;

        Map<String, Boolean> Promises = new ConcurrentHashMap<>();
        Map<String, Boolean> Accepts = new ConcurrentHashMap<>();

        ProposalNumber HighestAcceptedNum = null;
        String HighestAcceptedValue = null;

        ProposalState(String ProposalID, String Value) {
            this.ProposalID = ProposalID;
            this.Value = Value;
        }

        void AddPromise(String Member) { Promises.put(Member, true); }
        int PromiseCount() { return Promises.size(); }

        void AddAccepted(String Member) { Accepts.put(Member, true); }
        int AcceptedCount() { return Accepts.size(); }

        void MaybeAdoptHigher(ProposalNumber Num, String Val) {
            if (Num == null || Val == null) return;
            if (HighestAcceptedNum == null || Num.CompareTo(HighestAcceptedNum) > 0) {
                HighestAcceptedNum = Num;
                HighestAcceptedValue = Val;
            }
        }
    }
}