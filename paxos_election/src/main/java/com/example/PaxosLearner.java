package com.example;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PaxosLearner {
    private CouncilMember Node;
    private int Quorum;
    private String ConsensusValue;

    private Map<String, Integer> AcceptedCounts = new ConcurrentHashMap<>();
    private AtomicBoolean Decided = new AtomicBoolean(false);

    public PaxosLearner(CouncilMember Node) {
        this.Node = Node;
        this.Quorum = (Node.NetworkMap.size() / 2) + 1;
    }

    public String GetConsensusValue() {
        return ConsensusValue;
    }

    public void HandleAccepted(Message Msg) {
        if (Msg == null || Msg.Value == null) return;
        AcceptedCounts.merge(Msg.Value, 1, Integer::sum);
        int Cnt = AcceptedCounts.get(Msg.Value);
        if (!Decided.get() && Cnt >= Quorum) {
            if (Decided.compareAndSet(false, true)) {
                ConsensusValue = Msg.Value;
                System.out.println("CONSENSUS: " + Msg.Value + " has been elected Council President!");
            }
        }
    }
}