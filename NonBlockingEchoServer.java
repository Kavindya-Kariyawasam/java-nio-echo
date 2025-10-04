import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class NonBlockingEchoServer {
    private static final int BUFFER_SIZE = 256;
    private static final int PORT = 5000;

    public static void main(String[] args) {
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            // Configure server channel to be non-blocking
            serverChannel.configureBlocking(false);

            // Bind to port
            serverChannel.bind(new InetSocketAddress(PORT));
            System.out.println("Server started on port " + PORT);

            // Open selector and register server channel for accept events
            Selector selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            // Main server loop
            while (true) {
                // block until at least one event is ready
                selector.select();

                // get keys and iterate
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();

                    // Accept new connections
                    if (key.isAcceptable()) {
                        ServerSocketChannel srvChannel = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = srvChannel.accept();
                        if (clientChannel != null) {
                            clientChannel.configureBlocking(false);
                            clientChannel.register(selector, SelectionKey.OP_READ);
                            System.out.println("Connected to client: " + clientChannel.getRemoteAddress());
                        }
                    }
                    // Read from client
                    else if (key.isReadable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

                        int bytesRead = clientChannel.read(buffer);
                        if (bytesRead > 0) {
                            buffer.flip();
                            // Echo back the data
                            while (buffer.hasRemaining()) {
                                clientChannel.write(buffer);
                            }
                            buffer.clear();
                        } else if (bytesRead == -1) {
                            // client closed connection
                            System.out.println("Client disconnected: " + clientChannel.getRemoteAddress());
                            clientChannel.close();
                        }
                    }

                    // remove the key from selected set - it's been handled
                    keyIterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
