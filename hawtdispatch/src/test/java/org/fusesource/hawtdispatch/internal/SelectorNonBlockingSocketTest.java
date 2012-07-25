package org.fusesource.hawtdispatch.internal;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: cposta
 * Date: 6/28/12
 * Time: 8:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class SelectorNonBlockingSocketTest {

    @Test(expected = NotYetBoundException.class)
    public void testServerSocketAccept() throws IOException {
        // cannot accept a server socket that has not been bound
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.accept();
    }

    @Test
    public void testSelectorBlockingSelect() throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(4440));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("About to select...");
        // this will block...
//        selector.select();
        System.out.println("finished selecting");
    }

    public static void main(String[] args) {

        Selector selector = null;

        try {
            selector = Selector.open();
            ServerSocketChannel channel1 = createSocketChannel(4444);
            ServerSocketChannel channel2 = createSocketChannel(4445);

            // register all selection options on the channel to the selector
            channel1.register(selector, SelectionKey.OP_ACCEPT);
            channel2.register(selector, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
            System.out.println("Errors creating the selector");
        }

        System.out.println("Waiting for an event...");

        while (true) {
            try {
                selector.select();

            } catch (IOException e) {
                break;
            }

            Iterator it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey selectionKey = (SelectionKey) it.next();

                // remote it from the list to indicate it's being processed
                it.remove();

                if (selectionKey.isAcceptable()) {
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
                    int localPort = serverSocketChannel.socket().getLocalPort();
                    try {
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        processSocketChannel(socketChannel);
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }

                }
            }
        }
    }

    private static void processSocketChannel(SocketChannel socketChannel) {
        if (socketChannel == null) {
            // ignore because there are no events
        }
        else {
            try {
                ByteBuffer buf = ByteBuffer.allocate(1024);
                int numBytes = socketChannel.read(buf);
                if (numBytes == -1) {
                    socketChannel.close();
                } else {
                    buf.flip();
                    System.out.println("this is what we go: " + new String(buf.array()));
                    socketChannel.write(buf);
                }


            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    private static ServerSocketChannel createSocketChannel(int port) throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(new InetSocketAddress(port));
        System.out.println("Opened a socket");
        return channel;
    }


    public static void processSelection(SelectionKey selectionKey) throws IOException {

        if (selectionKey.isValid() && selectionKey.isConnectable()) {
            // get the channel with the connection request
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

            boolean success = socketChannel.finishConnect();
            if (!success) {
                // An error occurred, handle it
                System.out.println("Error happened!!");

                selectionKey.cancel();
            }

        }

        if (selectionKey.isValid() && selectionKey.isReadable()) {
            // get channel with bytes to read
//            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
//
//            ByteBuffer buf = ByteBuffer.allocate(1024);
//            try {
//                buf.clear();
//                int numBytesRead = socketChannel.read(buf);
//                if (numBytesRead == -1) {
//                    // no more bytes to read
//                    socketChannel.close();
//                } else {
//                    buf.flip();
//                    System.out.println("What we got: ");
//                    System.out.write(buf.array());
//                }
//
//            } catch (IOException e) {
//                // conn could be closed?
//                System.out.println("Connection could be closed");
//            }
            System.out.println("Read event...");

        }

        if (selectionKey.isValid() && selectionKey.isWritable()) {
            // get channel with bytes to write
//            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
//            ByteBuffer buf = ByteBuffer.allocate(1024);
//            try {
//                buf.put("you made it!".getBytes());
//                buf.flip();
//                int numBytesWritten = socketChannel.write(buf);
//                System.out.println("Number of bytes written: " + numBytesWritten);
//
//            } catch (IOException e) {
//                System.out.println("Connection could be closed");
//            }

            System.out.println("write event");


        }
    }
}
