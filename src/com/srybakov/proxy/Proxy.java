package com.srybakov.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static com.srybakov.proxy.Utils.close;
import static com.srybakov.proxy.Utils.handleException;
import static com.srybakov.proxy.Utils.log;

public class Proxy implements Runnable {

    private final String localAddress;
    private final String remoteAddress;
    private final int localPort;
    private final int remotePort;

    public Proxy(String localAddress, int localPort, String remoteAddress, int remotePort) {
        log("Create proxy for following params: localAddress = " + localAddress + ", localPort = " + localPort
                + ", remoteAddress = " + remoteAddress + ", remotePort = " + remotePort);
        this.localAddress = localAddress;
        this.localPort = localPort;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
    }

    @Override
    public void run() {
        try {
            Selector selector = Selector.open();
            registerInputChannelWithSelector(localAddress, localPort, selector);
            loop(selector);
        } catch (Exception e){
            handleException(e);
        }
    }

    private void loop(Selector selector) {
        // TODO: remove infinity loop
        while (true) {
            try {
                selector.select();
                for (SelectionKey key : selector.selectedKeys()) {
                    if (key.isAcceptable()) {
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                        SocketChannel inSock = serverSocketChannel.accept();
                        try {
                            SocketChannel outSock = SocketChannel.open(
                                    new InetSocketAddress(remoteAddress, remotePort));
                            Transmitter.getInstance().handleConnection(inSock, outSock);
                        } catch (Exception ex) {
                            close(inSock);
                            throw ex;
                        }
                    }
                }
            }
            catch (Exception e) {
                handleException(e);
            }
        }
    }

    private static void registerInputChannelWithSelector(String hostName, int port, Selector selector)
            throws IOException {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(hostName, port);
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(inetSocketAddress);
        channel.register(selector, SelectionKey.OP_ACCEPT, inetSocketAddress);
    }

}