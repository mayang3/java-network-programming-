package CH11_NIO.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

public class NonBlockingSingleFileHTTPServer {
    private ByteBuffer contentBuffer;
    private int port = 80;

    public NonBlockingSingleFileHTTPServer(ByteBuffer data, String encoding, String MIMEType, int port) {
        this.port = port;
        String header = new StringBuilder("HTTP/1.0 200 OK\r\n")
                .append("Server: NonblockingSingleFileHTTPServer\r\n")
                .append("Content-length: ").append(data.limit()).append("\r\n")
                .append("Content-Type: ").append(MIMEType).append("\r\n\r\n").toString();
        byte [] headerData = header.getBytes(StandardCharsets.US_ASCII);

        ByteBuffer buffer = ByteBuffer.allocate(data.limit() + headerData.length);
        buffer.put(headerData);
        buffer.put(data);
        buffer.flip();
        this.contentBuffer = buffer;
    }

    public void run() throws IOException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        ServerSocket serverSocket = serverChannel.socket();
        Selector selector = Selector.open();
        InetSocketAddress localPort = new InetSocketAddress(port);
        serverSocket.bind(localPort);
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            selector.select();
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();

                try {
                    if (key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel channel = server.accept();
                        channel.configureBlocking(false);
                        channel.register(selector, SelectionKey.OP_READ);
                    } else if (key.isWritable()) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        ByteBuffer buffer = (ByteBuffer) key.attachment();

                        if (buffer.hasRemaining()) {
                            channel.write(buffer);
                        } else {
                            channel.close();
                        }
                    } else if (key.isReadable()) {
                        // HTTP 헤더는 분석하지 않고 읽기만 한다.
                        SocketChannel channel = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(4096);
                        channel.read(buffer);

                        // flip 을 실행해주면 position~limit 이 데이터가 있는 범위가 된다.
                        buffer.flip();
                        byte [] bytes = new byte[buffer.limit()];
                        buffer.get(bytes);

                        System.out.println("Headers : [" + new String(bytes) + "]");

                        // 채널을 쓰기만 가능하도록 전환한다.
                        key.interestOps(SelectionKey.OP_WRITE);
                        // 서버 로드시 가져왔던 파일의 버퍼를 복사한다.
                        // 동일한 정보를 둘 이상의 채널에 전송하기 위해서는 버퍼를 복사해야 하는데, duplicate 메소드가 이러한 일을 한다.
                        key.attach(contentBuffer.duplicate());
                    }
                } catch (IOException ex) {
                    key.cancel();

                    try {
                        key.channel().close();
                    } catch (IOException cex) {}
                }
            }
        }
    }

    public static void main(String[] args) {
        String fileName = "content.txt";

        try {
            // 제공할 단일 파일을 읽는다.
            String contentType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
            Path file = FileSystems.getDefault().getPath(fileName);
            byte [] data = Files.readAllBytes(file);
            ByteBuffer input = ByteBuffer.wrap(data);

            // 대기할 포트 설정
            int port = 80;
            String encoding = "UTF-8";

            NonBlockingSingleFileHTTPServer server = new NonBlockingSingleFileHTTPServer(input, encoding, contentType, port);
            server.run();
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }
}
