package com.example;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PaxosLearner {
    private final CouncilMember Node;
    private final int Quorum;

    private final Map<String, Integer> AcceptedCounts = new ConcurrentHashMap<>();
    private final AtomicBoolean Decided = new AtomicBoolean(false);

    public PaxosLearner(CouncilMember Node) {
        this.Node = Node;
        this.Quorum = (Node.NetworkMap.size() / 2) + 1;
    }

    public void HandleAccepted(Message Msg) {
        if (Msg == null || Msg.Value == null) return;
        AcceptedCounts.merge(Msg.Value, 1, Integer::sum);
        int Cnt = AcceptedCounts.get(Msg.Value);
        if (!Decided.get() && Cnt >= Quorum) {
            if (Decided.compareAndSet(false, true)) {
                System.out.println("CONSENSUS: " + Msg.Value + " has been elected Council President!");
            }
        }
    }
}