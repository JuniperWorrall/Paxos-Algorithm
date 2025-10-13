package com.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class ConfigReader {

    public static Map<String, InetSocketAddress> Load(String FilePath) throws IOException{
        Map<String, InetSocketAddress> NetworkMap = new HashMap<>();

        try (BufferedReader Reader = new BufferedReader(new FileReader(FilePath))){
            String Line;

            while((Line = Reader.readLine()) != null){
                Line = Line.trim();

                if(Line.isEmpty() || Line.startsWith("#")){
                    continue;
                }

                String[] Parts = Line.split(",");
                if(Parts.length != 3){
                    System.err.println("Invalid config line: " + Line);
                    continue;
                }

                String MemberID = Parts[0].trim();
                String HostName = Parts[1].trim();
                int Port;

                try {
                    Port = Integer.parseInt(Parts[2].trim());
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number in line: " + Line);
                    continue;
                }

                NetworkMap.put(MemberID, new InetSocketAddress(HostName, Port));
            }
        }
        return NetworkMap;
    }
}
