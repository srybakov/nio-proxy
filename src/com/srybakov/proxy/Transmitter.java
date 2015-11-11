package com.srybakov.proxy;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.srybakov.proxy.Utils.close;
import static com.srybakov.proxy.Utils.handleException;

public class Transmitter {

    private static final int BUFFER_SIZE = 1024;
    private static final int MIN_THREAD_POOL_SIZE = 2;
    private static final int MAX_THREAD_POOL_SIZE = 64;
    private static final int THREAD_ALIVE_TIME = 30000;
    private static final int BLOCKING_QUEUE_CAPACITY = 1000;

    private static volatile Transmitter instance;
    private static ThreadPoolExecutor executor;

    private Transmitter(){
    }

    public static Transmitter getInstance() {
        if (instance == null){
            synchronized (Transmitter.class){
                if (instance == null){
                    instance = new Transmitter();
                    executor = new ThreadPoolExecutor(MIN_THREAD_POOL_SIZE, MAX_THREAD_POOL_SIZE, THREAD_ALIVE_TIME,
                            TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(BLOCKING_QUEUE_CAPACITY));
                }
            }
        }
        return instance;
    }

    public void handleConnection(final SocketChannel inChannel, final SocketChannel outChannel) {
        executor.execute(new Runnable() {
            public void run() {
                ByteBuffer transferBuf = ByteBuffer.allocate(BUFFER_SIZE);
                Selector sel = null;
                try {
                    sel = Selector.open();
                    inChannel.configureBlocking(false);
                    outChannel.configureBlocking(false);
                    inChannel.register(sel, SelectionKey.OP_READ);
                    outChannel.register(sel, SelectionKey.OP_READ);

                    while (sel.select() > 0) {
                        for (SelectionKey key : sel.selectedKeys()) {
                            transmit(key, inChannel, outChannel, transferBuf);
                        }
                    }
                } catch (Exception e) {
                    handleException(e);
                } finally {
                    close(sel, inChannel, outChannel);
                }
            }
        });
    }

    private static void transmit(SelectionKey key, SocketChannel inChannel, SocketChannel outChannel,
                                 ByteBuffer transferBuf) throws Exception {
        SocketChannel tmp = (SocketChannel) key.channel();
        if (tmp == null) {
            return;
        }
        if (key.isReadable()) {
            if (tmp == inChannel) {
                if (!transmit(tmp, outChannel, transferBuf)){
                    return;
                }
            }
            if (tmp == outChannel) {
                if (!transmit(tmp, inChannel, transferBuf)){
                    return;
                }
            }
        }
    }

    private static boolean transmit(SocketChannel from, SocketChannel to, ByteBuffer buf) throws Exception {
        buf.clear();
        while (true) {
            int num = from.read(buf);
            if (num < 0){
                return false;
            } else {
                if (num == 0){
                    return true;
                }
            }
            buf.flip();
            to.write(buf);
            buf.flip();
        }
    }

}