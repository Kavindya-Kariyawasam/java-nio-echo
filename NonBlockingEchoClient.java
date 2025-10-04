import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NonBlockingEchoClient {
    private static final int BUFFER_SIZE = 256;

    public static void main(String[] args) {
        String host = (args.length >= 1) ? args[0] : "localhost";
        int port = (args.length >= 2) ? Integer.parseInt(args[1]) : 5000;

        try (SocketChannel clientChannel = SocketChannel.open()) {
            clientChannel.configureBlocking(false);
            clientChannel.connect(new InetSocketAddress(host, port));

            Selector selector = Selector.open();
            // Initially register for connect events
            clientChannel.register(selector, SelectionKey.OP_CONNECT);

            // Queue for messages typed by the user
            ConcurrentLinkedQueue<String> pendingMessages = new ConcurrentLinkedQueue<>();

            // Input thread: reads from console and enqueues messages
            Thread inputThread = new Thread(() -> {
                BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Type messages to send (type 'quit' to exit):");
                try {
                    String line;
                    while ((line = console.readLine()) != null) {
                        pendingMessages.add(line);
                        // wake up selector so main thread can register OP_WRITE
                        selector.wakeup();
                        if ("quit".equalsIgnoreCase(line.trim())) break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            inputThread.setDaemon(true);
            inputThread.start();

            ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
            boolean running = true;

            while (running) {
                selector.select();

                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove(); // important

                    if (!key.isValid()) continue;

                    // connection ready to finish
                    if (key.isConnectable()) {
                        SocketChannel sc = (SocketChannel) key.channel();
                        if (sc.finishConnect()) {
                            System.out.println("Connected to server " + sc.getRemoteAddress());
                            // start by listening for reads
                            key.interestOps(SelectionKey.OP_READ);
                        } else {
                            // cannot finish connect -> cancel
                            key.cancel();
                        }
                    }

                    // server sent data
                    if (key.isReadable()) {
                        SocketChannel sc = (SocketChannel) key.channel();
                        readBuffer.clear();
                        int bytesRead = sc.read(readBuffer);
                        if (bytesRead > 0) {
                            readBuffer.flip();
                            byte[] data = new byte[readBuffer.remaining()];
                            readBuffer.get(data);
                            System.out.println("Echo from server: " + new String(data, StandardCharsets.UTF_8));
                        } else if (bytesRead == -1) {
                            System.out.println("Server closed the connection");
                            sc.close();
                            running = false;
                            break;
                        }
                    }

                    // ready to write queued messages
                    if (key.isWritable()) {
                        SocketChannel sc = (SocketChannel) key.channel();
                        String msg;
                        while ((msg = pendingMessages.poll()) != null) {
                            // send 'quit' then break and terminate
                            byte[] bytes = (msg + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
                            ByteBuffer out = ByteBuffer.wrap(bytes);
                            while (out.hasRemaining()) {
                                sc.write(out);
                            }
                            if ("quit".equalsIgnoreCase(msg.trim())) {
                                running = false;
                                break;
                            }
                        }
                        // once we wrote everything, stop listening for OP_WRITE
                        if (key.isValid()) {
                            key.interestOps(SelectionKey.OP_READ);
                        }
                    }
                }

                // If there are pending messages, make sure OP_WRITE is set so we can send them
                SelectionKey sk = clientChannel.keyFor(selector);
                if (sk != null && sk.isValid() && !pendingMessages.isEmpty()) {
                    sk.interestOps(sk.interestOps() | SelectionKey.OP_WRITE);
                    selector.wakeup();
                }
            }

            selector.close();
            System.out.println("Client exiting.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
