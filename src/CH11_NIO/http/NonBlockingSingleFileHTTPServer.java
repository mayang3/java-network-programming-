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
        serverChannel.bind(new InetSocketAddress(port));
        Selector selector = Selector.open();
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
                        // HTTP ????????? ???????????? ?????? ????????? ??????.
                        SocketChannel channel = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(5);
                        channel.read(buffer);

                        // flip ??? ??????????????? position~limit ??? ???????????? ?????? ????????? ??????.
                        buffer.flip();
                        byte [] bytes = new byte[buffer.limit()];
                        buffer.get(bytes);

                        System.out.println("Headers : [" + new String(bytes) + "]");

                        // ????????? ????????? ??????????????? ????????????.
                        key.interestOps(SelectionKey.OP_WRITE);
                        // ?????? ????????? ???????????? ????????? ????????? ????????????.
                        // ????????? ????????? ??? ????????? ????????? ???????????? ???????????? ????????? ???????????? ?????????, duplicate ???????????? ????????? ?????? ??????.
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
            // ????????? ?????? ????????? ?????????.
            String contentType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
            Path file = FileSystems.getDefault().getPath(fileName);
            byte [] data = Files.readAllBytes(file);
            ByteBuffer input = ByteBuffer.wrap(data);

            // ????????? ?????? ??????
            int port = 80;
            String encoding = "UTF-8";

            NonBlockingSingleFileHTTPServer server = new NonBlockingSingleFileHTTPServer(input, encoding, contentType, port);
            server.run();
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }
}
