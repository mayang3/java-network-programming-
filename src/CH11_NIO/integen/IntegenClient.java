package CH11_NIO.integen;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.SocketChannel;

/**
 * 정수 생성기 클라이언트
 */
public class IntegenClient {
    public static int DEFAULT_PORT = 1919;

    public static void main(String[] args) {
        try {
            SocketAddress address = new InetSocketAddress("localhost", DEFAULT_PORT);
            SocketChannel client = SocketChannel.open(address);

            ByteBuffer buffer = ByteBuffer.allocate(4);
            IntBuffer view = buffer.asIntBuffer();

            for (int expected = 0; ; expected++) {
                client.read(buffer); // 채널에서 읽어 버퍼에 넣는다.
                int actual = view.get();
                buffer.clear();
//                view.rewind();
                view.clear();

                if (actual != expected) {
                    System.err.println("Expected " + expected + "; was " + actual);
                    break;
                }

                System.out.println(actual);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
