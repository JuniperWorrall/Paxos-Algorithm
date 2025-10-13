package com.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Network {
    public static void Send(InetSocketAddress Addr, Message Msg){
        try(Socket Socket = new Socket()){
            Socket.connect(Addr, 2000);
            PrintWriter pw = new PrintWriter(Socket.getOutputStream(), true);
            pw.println(Msg.ToNode());
        } catch (Exception e){
            System.err.println("Send failed to connect to " + Addr + ": " + e.getMessage());
        }
    }

    public static Message Recieve(Socket Socket){
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(Socket.getInputStream()));
            String Line = br.readLine();
            return Message.FromNode(Line);
        } catch (Exception e){
            System.err.println("Recieve failed: " + e.getMessage());
            return null;
        } finally {
            try {Socket.close();} catch (Exception i) {}
        }
    }
}
