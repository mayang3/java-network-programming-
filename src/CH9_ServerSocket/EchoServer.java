package CH9_ServerSocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

public class EchoServer {
    public static int DEFAULT_PORT = 7;

    public static void main(String[] args) {
        int port;

        try {
            port = Integer.parseInt(args[0]);
        } catch (RuntimeException ex) {
            port = DEFAULT_PORT;
        }

        System.out.println("Listening for connections on port " + port);

        ServerSocketChannel serverSocketChannel;
        Selector selector;

        try {
            serverSocketChannel = ServerSocketChannel.open();
            ServerSocket ss = serverSocketChannel.socket();
            InetSocketAddress address = new InetSocketAddress(port);
            ss.bind(address);
            serverSocketChannel.configureBlocking(false);

            // TODO...
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
    }
}
