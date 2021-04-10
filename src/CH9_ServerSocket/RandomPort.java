package CH9_ServerSocket;

import java.io.IOException;
import java.net.ServerSocket;

public class RandomPort {
    public static void main(String[] args) {
        try {
            ServerSocket server = new ServerSocket(0); // 랜덤포트를 할당한 후에,
            System.out.println("This server runs on port " + server.getLocalPort()); // 실제 바인딩된 포트를 확인한다.
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }
}
