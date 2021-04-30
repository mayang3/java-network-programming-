package CH11_NIO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class ChargenServer {
    public static int DEFAULT_PORT = 9099;

    public static void main(String[] args) {
        int port;

        try {
            port = Integer.parseInt(args[0]);
        } catch (RuntimeException ex) {
            port = DEFAULT_PORT;
        }

        System.out.println("Listening for connections on port " + port);

        // 두 개의 연속적인 데이터의 복사본을 저장한 배열을 만들어두면,
        // 전송할 시작 위치에 상관없이 모든 라인을 배열에서 연속적으로 이용할 수 있기 때문에 코드를 작성하기 훨씬 쉽다.
        byte [] rotation = new byte[95*2];

        for (byte i = ' '; i <= '~'; i++) {
           rotation[i - ' '] = i; // 첫번째 시작지점
           rotation[i + 95 - ' '] = i; // 두번째 시작지점
        }

        ServerSocketChannel serverChannel;
        Selector selector;

        try {
            serverChannel = ServerSocketChannel.open();
            ServerSocket ss = serverChannel.socket();
            InetSocketAddress address = new InetSocketAddress(port);
            ss.bind(address);
            serverChannel.configureBlocking(false);
            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        while (true) {
            try {
                // 셀렉터가 준비된 채널을 찾는다.
                selector.select();
            } catch (IOException ex) {
                ex.printStackTrace();
                break;
            }

            // 셀렉터가 준비된 채널을 찾은 경우 not empty Set 을 반환한다.
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                // 중복처리를 방지하기 위해 처리한 키를 제거한다.
                iterator.remove();

                try {
                    // 최초 socket 연결이 수립되었을 때
                    if (key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();

                        System.out.println("Accepted connection from " + client);

                        client.configureBlocking(false);
                        SelectionKey key2 = client.register(selector, SelectionKey.OP_WRITE);
                        // 버퍼는 다음 줄을 쓰기 위해, 버퍼의 끝에 쓰기보다는 버퍼의 시작 부분으로 돌아와서 다시 쓴다.
                        // rotation 배열은 초기화 이후에 읽기 용도로만 사용할 것이기 때문에, 다수의 채널에서 함께 사용될 수 있다.
                        // 각각의 채널은 이 배열의 데이터를 사용해 자신들의 버퍼를 채운다.
                        ByteBuffer buffer = ByteBuffer.allocate(74);
                        // 즉, rotation 배열의 처음 72 바이트를 사용해 버퍼를 채운 다음
                        buffer.put(rotation, 0, 72);
                        // 라인을 구분하기 위한 캐리지 리턴/ 라인피드를 추가한다.
                        buffer.put((byte) '\r');
                        buffer.put((byte) '\n');
                        // 전송 준비를 위해 버퍼에 대해 flip() 메소드를 호출한다.
                        buffer.flip();
                        // 버퍼를 채널의 OP_WRITE 키에 첨부한다.
                        key2.attach(buffer);
                    } else if (key.isWritable()) {
                        // 채널에 데이터를 쓰는 경우
                        SocketChannel client = (SocketChannel) key.channel();
                        // 키에 첨부된 버퍼를 구한다.
                        ByteBuffer buffer = (ByteBuffer) key.attachment();

                        // 버퍼에 아직 쓰지 않은 데이터가 남아 있는지 확인하기 위해 hasRemaining 을 호출해준다.
                        // 남아 있는 데이터가 있는 경우 출력하고,
                        // 남아 있는 데이터가 없는 경우, 회전하는 배열에 있는 데이터의 다음 줄로 버퍼를 채우고 채워진 버퍼를 쓴다.
                        if (!buffer.hasRemaining()) {
                            // 버퍼의 position 을 1로 바꾼다. (이전에 보낸 줄의 시작 문자를 구하기 위해서..)
                            buffer.rewind();
                            // 이전에 보낸 줄의 시작 문자를 구한다. (이때 position 이 +1 증가한다.)
                            int first = buffer.get();
                            // 버퍼의 데이터를 변경할 준비를 한다. (position 을 1로 다시 바꾼다.)
                            buffer.rewind();
                            // rotation 에서 새로운 시작 문자의 위치를 찾는다. (이전 시작 문자의 다음 문자가 된다. - 한칸 rotation)
                            int position = first - ' ' + 1;
                            // rotation 에서 buffer 로 데이터를 복사한다.
                            buffer.put(rotation, position, 72);
                            // 버퍼의 마지막에 라인 구분자를 저장한다.
                            buffer.put((byte) '\r');
                            buffer.put((byte) '\n');
                            // 버퍼를 출력할 준비를 한다.
                            buffer.flip();
                        }

                        // 버퍼의 내용을 client 에 전달한다.
                        client.write(buffer);
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
