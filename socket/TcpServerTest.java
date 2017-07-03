import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings({"ObjectAllocationInLoop", "UseOfSystemOutOrSystemErr", "AssignmentToStaticFieldFromInstanceMethod"})
public class TcpServerTest {
    private static final String SERVER_IP = "localhost";
    private static final int PORT = 8000;
    private static final int CONNECT_TIMEOUT = 5000;

    private static int m_ConnectionCount;
    private SocketAddress m_ServerAddress = new InetSocketAddress(SERVER_IP , PORT);
    private JSONObject m_RequireJsonData = new JSONObject();

    //连接服务器
    private void connect() {
        //与服务器通信的协议的格式
        m_RequireJsonData.put("ConnectionState" , "require connect");
        m_RequireJsonData.put("TaskNumber" , "-1");

        //连接服务器
        try(Socket tClient = new Socket()){
            tClient.connect(m_ServerAddress , CONNECT_TIMEOUT);

            //记录连接数
           m_ConnectionCount = m_ConnectionCount+1;

            //新建线程监听服务器的信息
            new ListenerThread(tClient);

            //向服务器发送信息
            try (PrintStream tSender = new PrintStream(tClient.getOutputStream())) {
                while (false == tClient.isClosed()) {
                    //向服务器发送一个任务，测试并发数
                    int tTaskNumber = (int) (Math.random()*3);
                    m_RequireJsonData.put("TaskNumber" , tTaskNumber);
                    tSender.println(m_RequireJsonData);
                    Thread.sleep(200);
                }
            } catch (IOException e) {
//                System.out.println(e.getMessage());
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
//                e.printStackTrace();
            }
        } catch (IOException e) {
//            System.out.println(e.getMessage());
        }
    }


    //接收服务器信息的线程
    @SuppressWarnings({"ObjectAllocationInLoop", "UseOfSystemOutOrSystemErr"})
    private class ListenerThread extends Thread {

        private Socket m_Socket;

        ListenerThread(Socket vSocket) {
            super();
            m_Socket = vSocket;
            start();
        }

        @Override
        public void run() {
            try (BufferedReader tReceiver = new BufferedReader(new InputStreamReader(m_Socket.getInputStream()))){
                while (true) {
                    //接收消息
                    String tReceivedString = tReceiver.readLine();
                    if (null != tReceivedString) {
                        //将字符串转换为json对象
                        JSONObject tResponseJsonData = new JSONObject(tReceivedString);
                        //获取服务器响应数据
                        String tConnectionState = tResponseJsonData.getString("ConnectionState");
                        String tTaskExecuteInfo = tResponseJsonData.getString("TaskExecuteInfo");
                        String tError = tResponseJsonData.getString("Error");

//                        System.out.println("ConnectionState: " + tConnectionState + "\n"
//                                + "TaskExecuteInfo: " + tTaskExecuteInfo + "\n"
//                                + "Error: "+ tError + "\n");

                        //ConnectionState 为Disconnect/error时退出循环，结束连接
                        if (true == tConnectionState.equals("Disconnect"))  break;
                        if(true == tConnectionState.equals("error"))        break;
                    }
                }
            } catch (IOException e) {
//                System.out.println(e.getMessage());
            } finally {
                try {
                    m_Socket.close();
                } catch (IOException e) {
//                    System.out.println(e.getMessage());
                }
            }
        }
    }

    public static void main(String[] vArgs) {
        int tThreadCount = 2000;
        ExecutorService tExecutor = Executors.newFixedThreadPool(tThreadCount);
//        for (int i = 0; tThreadCount > i; i++) {
//            tExecutor.execute(() -> new TcpServerTest().connect());
//        }
        while(true){
            System.out.println(m_ConnectionCount);
            if(tThreadCount <= m_ConnectionCount){
                System.out.println("并发数超过2000");
                tExecutor.shutdownNow();
                break;
            }
            tExecutor.execute(() -> new TcpServerTest().connect());
        }
    }
}
