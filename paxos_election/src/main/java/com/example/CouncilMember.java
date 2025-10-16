package com.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class CouncilMember {
    public String MemberID;
    public Profile Profile;
    public Map<String, InetSocketAddress> NetworkMap;
    public ServerSocket Listener;
    public PaxosProposer Proposer;
    public PaxosAcceptor Acceptor;
    public PaxosLearner Learner;

    private boolean Running = true;

    public CouncilMember(String ID, Profile Profile, String ConfigFile) throws IOException {
        this.MemberID = ID;
        this.Profile = Profile;
        this.NetworkMap = ConfigReader.Load(ConfigFile);
        InetSocketAddress addr = NetworkMap.get(ID);
        if (addr == null) throw new IllegalArgumentException("No address for id: " + ID);
        this.Listener = new ServerSocket(addr.getPort());
        this.Proposer = new PaxosProposer(this);
        this.Acceptor = new PaxosAcceptor(this);
        this.Learner = new PaxosLearner(this);

        new Thread(this::ReceiveLoop, "recv-" + ID).start();
    }

    private void ReceiveLoop() {
        System.out.println(MemberID + " listening on port " + Listener.getLocalPort());
        while (Running) {
            try {
                Socket s = Listener.accept();
                new Thread(() -> {
                    Message msg = Network.Recieve(s);
                    if (msg == null) return;

                    if (!DelaySimulator.ApplyDelay(this.Profile)) return;
                    Dispatch(msg);
                }).start();
            } catch (IOException e) {
                if (Running) System.err.println("Accept error: " + e.getMessage());
            }
        }
    }

    private void Dispatch(Message Msg) {
        switch (Msg.Type) {
            case "PREPARE":
                Acceptor.HandlePrepare(Msg);
                break;
            case "PROMISE":
                Proposer.HandlePromise(Msg);
                break;
            case "ACCEPT_REQUEST":
                Acceptor.HandleAcceptRequest(Msg);
                break;
            case "ACCEPTED":
                Proposer.HandleAccepted(Msg);
                Learner.HandleAccepted(Msg);
                break;
            default:
                System.err.println(MemberID + " unknown message type: " + Msg.Type);
        }
    }

    public void SendMessage(String TargetID, Message Msg) {
        InetSocketAddress Addr = NetworkMap.get(TargetID);
        if (Addr == null) {
            System.err.println("Unknown target: " + TargetID);
            return;
        }
        if (!DelaySimulator.ApplyDelay(this.Profile)) {
            System.out.println(MemberID + " simulated drop sending to " + TargetID);
            return;
        }
        Network.Send(Addr, Msg);
    }

    public void Shutdown() {
        Running = false;
        try { Listener.close(); } catch (IOException i) {}
    }

    public void CLILoop() {
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(System.in))) {
            System.out.println("[" + MemberID + "] CLI started. Type candidate name or 'quit'.");
            String LineRaw;
            while ((LineRaw = br.readLine()) != null) {
                String Line = LineRaw.trim();
                if (Line.equalsIgnoreCase("quit")) {
                    Shutdown();
                    break;
                }
                if (!Line.isEmpty()) {
                    System.out.println(MemberID + " proposing " + Line);
                    new Thread(() -> Proposer.InitiateProposal(Line)).start();
                }
            }
        } catch (Exception e) {
            System.err.println("CLI error: " + e.getMessage());
        }
    }

    public static void Main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java com.example.CouncilMember <MemberId> <profile> [configFile]");
            System.err.println("Example: java com.example.CouncilMember M1 reliable network.config");
            return;
        }
        String ID = args[0];
        Profile profile = com.example.Profile.valueOf(args[1].toUpperCase());
        String Config = args.length >= 3 ? args[2] : "network.config";
        CouncilMember Node = new CouncilMember(ID, profile, Config);
        Node.CLILoop();
    }
}