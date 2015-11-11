package com.srybakov.proxy;

import java.io.Closeable;
import java.io.IOException;

public final class Utils {

    public static void close(Closeable... closeables) {
        for (Closeable closeable : closeables){
            try {
                if (closeable != null){
                    closeable.close();
                }
            } catch (IOException e) {
                //TODO: process it
            }
        }
    }

    public static void handleException(Exception e){
        //TODO: add handler
        log("Something going wrong...Exception is: " + e.getMessage());
    }

    public static void log(String message){
        System.out.println(message);
    }
}
