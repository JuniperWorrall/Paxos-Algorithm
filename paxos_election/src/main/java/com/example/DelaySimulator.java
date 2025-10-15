package com.example;

import java.util.Random;

public class DelaySimulator {
    public static Random RAND = new Random();

    public static boolean ApplyDelay(Profile profile){
        try {
            switch (profile) {
                case RELIABLE:
                    Thread.sleep(RAND.nextInt(50));
                    return true;
                case STANDARD:
                    Thread.sleep(50 + RAND.nextInt(150));
                    return true;
                case LATENT:
                    Thread.sleep(500 + RAND.nextInt(1500));
                    return true;
                case FAILURE:
                    if (RAND.nextDouble() < 0.2) return false;
                    Thread.sleep(100 + RAND.nextInt(400));
                    return true;
                default:
                    Thread.sleep(50);
                    return true;
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
