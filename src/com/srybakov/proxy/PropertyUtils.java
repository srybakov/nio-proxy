package com.srybakov.proxy;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

public class PropertyUtils {

    public static final String LOCAL_PORT = "localPort";
    public static final String REMOTE_PORT = "remotePort";
    public static final String REMOTE_HOST = "remoteHost";

    private static final String PROPERTY_NAME_VALUE_DELIMETER = "=";
    private static final String ENTITY_PROPERTY_DELIMETER = ".";

    public static Map<String, Map<String, String>> createPropertiesMap(String propertyFilePath)
            throws IOException {
        Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
        String propertyFileContent = PropertyUtils.readPropertyFileAsString(propertyFilePath);
        List<String> propertiesList = getPropertiesList(propertyFileContent);
        Set<String> entitiesList = getPropertyFileEntitiesList(propertiesList);
        fillProperties(properties, propertyFilePath, entitiesList);
        return properties;
    }

    private static void fillProperties(Map<String, Map<String, String>> properties, String propertyFilePath,
                                       Set<String> entitiesList) throws IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream(propertyFilePath));
        for (String entity : entitiesList){
            String localPort = prop.getProperty(entity + ENTITY_PROPERTY_DELIMETER + LOCAL_PORT);
            String remotePort = prop.getProperty(entity + ENTITY_PROPERTY_DELIMETER + REMOTE_PORT);
            String remoteHost = prop.getProperty(entity + ENTITY_PROPERTY_DELIMETER + REMOTE_HOST);
            if (isParametersValid(localPort, remotePort, remoteHost)){
                Map<String, String> nameValue = new HashMap<String, String>();
                nameValue.put(LOCAL_PORT, localPort);
                nameValue.put(REMOTE_PORT, remotePort);
                nameValue.put(REMOTE_HOST, remoteHost);
                properties.put(entity, nameValue);
            }
        }
    }

    private static boolean isParametersValid(String localPort, String remotePort, String remoteHost){
        return localPort != null && remotePort != null && remoteHost != null
                && isNumeric(localPort) && isNumeric(remotePort);
    }

    //Simple way to check that string is numeric or not without regex :)
    public static boolean isNumeric(String str) {
        try {
            int d = Integer.parseInt(str);
        } catch(NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private static Set<String> getPropertyFileEntitiesList(List<String> propertiesList){
        Set<String> result = new HashSet<String>();
        for (String propertyLine : propertiesList){
            StringTokenizer entitryTokenizer = new StringTokenizer(propertyLine, ENTITY_PROPERTY_DELIMETER);
            result.add(entitryTokenizer.nextToken());
        }
        return result;
    }

    private static String readPropertyFileAsString(String filePath) throws IOException {
        StringBuilder builder = new StringBuilder();
        RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "r");
        FileChannel inChannel = randomAccessFile.getChannel();
        ByteBuffer buffer = createReadyForReadByteBuffer(inChannel);
        writeFileContentToBuilder(builder, buffer, inChannel.size());
        close(inChannel, randomAccessFile);
        return builder.toString();
    }

    private static List<String> getPropertiesList(String propertyFileContent){
        StringTokenizer stringTokenizer = new StringTokenizer(propertyFileContent, "\n");
        List<String> propertiesList = new ArrayList<String>();
        while (stringTokenizer.hasMoreTokens()){
            String propertyString = stringTokenizer.nextToken().replaceAll("\\r|\\n", "");
            if (propertyString.contains(PROPERTY_NAME_VALUE_DELIMETER)){
                propertiesList.add(propertyString);
            }
        }
        return propertiesList;
    }

    private static void writeFileContentToBuilder(StringBuilder builder, ByteBuffer buffer, long size){
        for (int i = 0; i < size; i++) {
            builder.append((char) buffer.get());
        }
    }

    private static ByteBuffer createReadyForReadByteBuffer(FileChannel inChannel) throws IOException {
        ByteBuffer buffer = createEmptyByteBuffer(inChannel);
        inChannel.read(buffer);
        buffer.flip();
        return buffer;
    }

    private static ByteBuffer createEmptyByteBuffer(FileChannel inChannel) throws IOException {
        long fileSize = inChannel.size();
        return ByteBuffer.allocate((int) fileSize);
    }

    private static void close(Closeable... closeable){
        for (Closeable toClose : closeable){
            try {
                toClose.close();
            } catch (IOException e) {
                //TODO: should be processed
            }
        }
    }
}
