package CH10_SecureSocket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;

/**
 * 이 프로그램을 실행해보면 예상보다 응답이 훨씬 느리다.
 * 공개키를 생성하고 교환하는데 소요되는 CPU 와 네트워크 오버헤드가 눈에 띌 정도이다.
 * 아무리 네트워크가 빠르다 해도 최초 연결을 맺는 데 수초가 걸린다.
 *
 * 결과적으로 모든 콘텐츠를 HTTPS 로 제공하기보다는 보안이 필요하거나 지연에 민감하지 않은 콘텐츠에 한해서 HTTPS 를 제공해보자.
 */
public class HTTPsClient {

    public static void main(String[] args) {
//        if (args.length == 0) {
//            System.out.println("Usage: java HTTPsClient2 host");
//            return;
//        }

        int port = 443;
        String host = "www.usps.com";

        // SSLFactory 는 추상 팩토리 디자인 패턴을 따르는 abstract class
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket socket = null;

        try {
            socket = (SSLSocket) factory.createSocket(host, port);

            // 모든 암호화 조합을 사용하도록 설정
            String [] supported = socket.getSupportedCipherSuites(); // 이 커넥션에서 이용할 수 있는 모든 암호화 조합의 이름을 리턴
            socket.setEnabledCipherSuites(supported); // 설정

            Writer out = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
            // https 는 GET 요청시 전체 URL 을 사용해야 한다. (URI 만 명시해서는 안된다.)
            out.write("GET http://" + host + "/ HTTP/1.1\r\n");
            out.write("Host: " + host + "\r\n");
            out.write("\r\n");
            out.flush();

            // 응답 읽기
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 헤더 읽기
            String s;

            while (!(s = in.readLine()).equals("")) {
                System.out.println(s);
            }

            // 길이 읽기
            String contentLength = in.readLine();
            int length = Integer.MAX_VALUE;
            try {
                length = Integer.parseInt(contentLength.trim(), 16);
            } catch (NumberFormatException ex) {
                // 서버가 응답 본문의 첫째줄에 content-length 를 보내지 않은 경우
            }


            int c;
            int i=0;
            while ((c = in.read()) != -1 && i++ < length) {
                System.out.write(c);
            }

            System.out.println();
        } catch (IOException ex) {
            System.err.println(ex);
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {}
        }
    }
}
