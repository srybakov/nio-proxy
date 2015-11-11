package com.srybakov.proxy;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.srybakov.proxy.Utils.handleException;
import static com.srybakov.proxy.Utils.log;

public class Main {

    private static final String PROPERTY_FILE_PATH;
    private static final String PROPERTY_FILE_NAME = "proxy.properties";
    private static final String LOCALHOST = "localhost";

    static {
        PROPERTY_FILE_PATH = new File("").getAbsolutePath() + File.separator + PROPERTY_FILE_NAME;
    }

    public static void main(String[] args) throws Exception {
        try {
            log("Proxy server starting...");
            Map<String, Map<String, String>> proxyProperties = PropertyUtils.createPropertiesMap(PROPERTY_FILE_PATH);
            for (Map<String, String> entityPropertyMap : proxyProperties.values()){
                int localPort = Integer.parseInt(entityPropertyMap.get(PropertyUtils.LOCAL_PORT));
                int remotePort = Integer.parseInt(entityPropertyMap.get(PropertyUtils.REMOTE_PORT));
                String remoteHost = entityPropertyMap.get(PropertyUtils.REMOTE_HOST);
                new Thread(new Proxy(LOCALHOST, localPort, remoteHost, remotePort)).start();
            }
            log("Proxy server started");
        } catch (IOException e) {
            handleException(e);
            System.exit(1);
        }
    }


}
