package CH10_SecureSocket;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * SSL ServerSocket 을 사용하여 order 를 처리하는 보안서버소켓을 구현
 */
public class SecureOrderTaker {
    public final static int PORT = 7000;
    public final static String algorithm = "SSL";

    public static void main(String[] args) {
        try {
            SSLContext context = SSLContext.getInstance(algorithm);

            // 참조 구현에는 X.509 키만 지원한다.
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");

            // 오라클의 기본 키스토어 유형
            KeyStore ks = KeyStore.getInstance("JKS");

            // 보안 문제로, 모든 키스토어는 디스크에서 읽어 오기 전에 passphrase 로 암호화되어야 한다.
            // 패스는 char[] 배열에 저장되어 있으며 가비지 컬렉터를 기다리지 않고 재빨리 메모리에서 지워야 한다.
            char [] password = new Scanner(System.in).next().toCharArray();
            ks.load(new FileInputStream("jnp4e.keys"), password);
            kmf.init(ks, password);
            context.init(kmf.getKeyManagers(), null, null);

            // 패스워드 제거
            Arrays.fill(password, '0');

            SSLServerSocketFactory factory = context.getServerSocketFactory();
            SSLServerSocket server = (SSLServerSocket) factory.createServerSocket(PORT);

            // 익명 (인증되지 않음) 암호화 조합 추가
            String [] supported = server.getSupportedCipherSuites();
            String [] anonCipherSuitesSupported = new String[supported.length];
            int numAnonCipherSuitesSupported = 0;
            for (int i = 0; i < supported.length; i++) {
                if (supported[i].indexOf("_anon_") > 0) {
                    anonCipherSuitesSupported[numAnonCipherSuitesSupported++] = supported[i];
                }
            }

            String [] oldEnabled = server.getEnabledCipherSuites();
            String [] newEnabled = new String[oldEnabled.length + numAnonCipherSuitesSupported];

            System.arraycopy(oldEnabled, 0, newEnabled, 0, oldEnabled.length);
            System.arraycopy(anonCipherSuitesSupported, 0, newEnabled, oldEnabled.length, numAnonCipherSuitesSupported);

            server.setEnabledCipherSuites(newEnabled);

            System.out.println("Load complete");

            // 모든 설정이 끝나고 이제 실제 통신에 초점을 맞춘다.
            while (true) {
                // 이 소켓은 보안이 적용된 소켓이지만 소켓 사용 방법에 별 차이가 없다.
                try (Socket theConnection = server.accept()) {
                    InputStream in = theConnection.getInputStream();
                    int c;
                    while ((c = in.read()) != -1) {
                        System.out.write(c);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

        } catch (IOException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException ex) {
            ex.printStackTrace();
        }
    }
}
