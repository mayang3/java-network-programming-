package CH9_ServerSocket.HttpServer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SingleFileHTTPServer {
    private static final Logger logger = Logger.getLogger("SingleFileHTTPServer");

    private final byte [] content;
    private final byte [] header;
    private final int port;
    private final String encoding;

    public SingleFileHTTPServer(String data, String encoding, String mimeType, int port) throws UnsupportedEncodingException {
        this(data.getBytes(encoding), encoding, mimeType, port);
    }

    public SingleFileHTTPServer(byte [] data, String encoding, String mimeType, int port) {
        this.content = data;
        this.port = port;
        this.encoding = encoding;

        StringBuilder header = new StringBuilder("HTTP/1.0 200 OK\r\n");
        header.append("Server: OneFile 2.0\r\n");
        header.append("Content-length: ").append(this.content.length).append("\r\n");
        header.append("Content-Type: ").append(mimeType).append("; charset=").append(encoding).append("\r\n\r\n");

        this.header = header.toString().getBytes(StandardCharsets.US_ASCII);
    }

    public void start() {
        ExecutorService pool = Executors.newFixedThreadPool(100);
        try (ServerSocket server = new ServerSocket(this.port)) {
            logger.info("Accepting connections on port " + server.getLocalPort());
            logger.info("Data to be sent:");
            logger.info(new String(this.content, encoding));

            while (true) {
                try {
                    Socket connection = server.accept();
                    pool.submit(new HTTPHandler(connection));
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "Exception  accepting connection", ex);
                } catch (RuntimeException ex) {
                    logger.log(Level.SEVERE, "Unexpected error", ex);
                }
            }

        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not start server", ex);
        }
    }

    private class HTTPHandler implements Callable<Void> {
        private final Socket connection;

        HTTPHandler(Socket connection) {
            this.connection = connection;
        }

        @Override
        public Void call() throws Exception {
            try {
                OutputStream out = new BufferedOutputStream(connection.getOutputStream());
                InputStream in = new BufferedInputStream(connection.getInputStream());

                // 여기서는 필요한 첫 번째 줄만 읽는다.
                StringBuilder request = new StringBuilder(80);

                while (true) {
                    int c = in.read();
                    if (c == 'r' || c == '\n' || c == -1) break;
                    request.append((char)c);
                }

                // HTTP/1.0 이나 그 이후 버전을 지원할 경우 MIME 헤더를 전송한다.
                if (request.toString().indexOf("HTTP/") != -1) {
                    out.write(header);
                }

                out.write(content);
                out.flush();

            } catch (IOException ex) {
                logger.log(Level.WARNING, "Error writing to client", ex);
            } finally {
                connection.close();
            }

            return null;
        }
    }

    /**
     * args[0] : path
     * args[1] : port
     * args[2] : encoding
     *
     * @param args
     */
    public static void main(String[] args) {
        int port;

        try {
            port = Integer.parseInt(args[1]);
        } catch (RuntimeException ex) {
            port = 80;
        }

        String encoding = "UTF-8";

        if (args.length > 2) {
            encoding = args[2];
        }

        try {
            String file = "content.txt";
//            Path path = Paths.get(args[0]);
            Path path = Paths.get(file);
            byte [] data = Files.readAllBytes(path);

            String contentType = URLConnection.getFileNameMap().getContentTypeFor(file);
            SingleFileHTTPServer server = new SingleFileHTTPServer(data, encoding, contentType, port);
            server.start();

        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.println("Usage: java SingleFileHTTPServer filename port encoding");
        } catch (IOException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}
