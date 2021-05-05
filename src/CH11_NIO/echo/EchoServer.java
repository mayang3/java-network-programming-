package CH11_NIO.echo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 에코 프로토콜은 단순히 클라이언트가 보낸 데이터로 클라이언트에 응답한다.
 * 연결에 대해 읽기와 쓰기 모두를 수행한다.
 *
 */
public class EchoServer {

    public static int DEFAULT_PORT = 7;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        ServerSocketChannel serverChannel;
        Selector selector;

        try {
            // 채널을 연다
            serverChannel = ServerSocketChannel.open();
            // 채널안의 서버 소켓을 생성하고,
            ServerSocket ss = serverChannel.socket();
            InetSocketAddress address = new InetSocketAddress(port);
            // 주소를 바인딩 한다.
            ss.bind(address);
            // 채널을 non-blocking 으로 설정한다.
            serverChannel.configureBlocking(false);
            // selector 를 열고, 채널을 감시한다.
            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        while (true) {
            try {
                // 채널에서 준비된 녀석들을 가져온다.
                selector.select();
            } catch (IOException ex) {
                ex.printStackTrace();
                break;
            }

            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();

            while (iterator.hasNext())  {
                SelectionKey key = iterator.next();
                iterator.remove();

                try {
                    // 소켓 최초 연결시
                    if (key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();
                        System.out.println("Accepted connection from " + client);
                        client.configureBlocking(false);
                        SelectionKey clientKey = client.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
                        // 버퍼는 말 그대로 버퍼이다. 데이터의 총 길이를 의미하지 않는다.
                        // 버퍼의 크기를 1로 설정했다고, 데이터 바이트의 길이가 최대 1인 데이터만 허용한다는 뜻은 아니라는 이야기이다.
                        ByteBuffer buffer = ByteBuffer.allocate(10);
                        clientKey.attach(buffer);
                    }

                    if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        ByteBuffer output = (ByteBuffer) key.attachment();
                        client.read(output);
                    }

                    if (key.isWritable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        ByteBuffer output = (ByteBuffer) key.attachment();
                        output.flip();
                        client.write(output);
                        // compact 를 사용함으로 하나의 output buffer 를 계속 사용할 수 있다.
                        output.compact();
                    }
                } catch (IOException ex) {
                    key.cancel();
                    try {
                        key.channel().close();
                    } catch (IOException cex) {
                    }
                }
            }
        }
    }
}
