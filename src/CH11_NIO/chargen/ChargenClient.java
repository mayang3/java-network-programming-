package CH11_NIO.chargen;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

/**
 * 논블록 문자 발생기 클라이언트
 */
public class ChargenClient {

    public static int DEFAULT_PORT = 9099;

    public static void main(String[] args) {
//        if (args.length == 0) {
//            System.err.println("Usage: java ChargenClient host [port]");
//            return;
//        }

        int port;

        try {
            port = Integer.parseInt(args[1]);
        } catch (RuntimeException ex) {
            port = DEFAULT_PORT;
        }

        try {
//            SocketAddress address = new InetSocketAddress(args[0], port);
            SocketAddress address = new InetSocketAddress("localhost", port);
            SocketChannel client = SocketChannel.open(address);
            client.configureBlocking(false); // non block mode

            ByteBuffer buffer = ByteBuffer.allocate(74);
            WritableByteChannel out = Channels.newChannel(System.out);

            while (client.read(buffer) != -1) {
                buffer.flip();
                out.write(buffer);
                buffer.clear();
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
