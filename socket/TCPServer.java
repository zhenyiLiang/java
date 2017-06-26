import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings({"UseOfSystemOutOrSystemErr", "ObjectAllocationInLoop", "InfiniteLoopStatement"})
public class TCPServer {
    private static final int LISTENING_PORT = 8000;
    private static final int MAX_CONNECTIONS_NUMBER = 2;

    private List<Socket> m_ConnectionList = new ArrayList<>();
    private ExecutorService m_Executor = Executors.newFixedThreadPool(MAX_CONNECTIONS_NUMBER);

    //处理执行客户端的请求
    private class Task implements Runnable {
        private Socket m_Client;
        private JSONObject m_ResponseJsonData = new JSONObject();

        Task(Socket vSocket) {
            m_Client = vSocket;
            System.out.println(m_Client.getPort() + " 连接服务器成功");

            //定义与客户端通信的协议的格式
            m_ResponseJsonData.put("ConnectionState" , "Successful");
            m_ResponseJsonData.put("TaskExecuteInfo" , "no task is executed");
            m_ResponseJsonData.put("Error" , "no error");
        }

        //模拟服务器端的不同的业务
        private void task0() {
            System.out.println(m_Client.getPort() + " 执行了task 0");
            m_ResponseJsonData.put("TaskExecuteInfo" , "task 0 execute successful");
        }
        private void task1(){
            System.out.println(m_Client.getPort() + " 执行了task 1");
            m_ResponseJsonData.put("TaskExecuteInfo" , "task 1 execute successful");
        }
        private void taskDefault() {
            System.out.println(m_Client.getPort() + " 执行了默认task");
            m_ResponseJsonData.put("TaskExecuteInfo" , "error");
            m_ResponseJsonData.put("Error" , "There is no such task , please check your input");
        }

        //根据任务信息执行不同任务
        private void executeTask(int vTaskNumber, PrintStream vSender) {
            m_ResponseJsonData.put("ConnectionState" , "Connected");
            m_ResponseJsonData.put("Error" , "no error!");

            switch (vTaskNumber) {
                case 0:
                    task0();
                    break;
                case 1:
                    task1();
                    break;
                default:
                    taskDefault();
                    break;
            }
            //向客户端发送业务执行结果
            vSender.println(m_ResponseJsonData);
        }

        @Override
        public void run() {
            try (BufferedReader tReceiver = new BufferedReader(new InputStreamReader(m_Client.getInputStream()));
                 PrintStream tSender = new PrintStream(m_Client.getOutputStream())) {
                //向客户端发送连接成功的信息
                tSender.println(m_ResponseJsonData);

                //接收客户端的信息。
                while (true) {
                    String tReceivedString = tReceiver.readLine();
                    if (null != tReceivedString) {
                        //将接收到的字符串转换为json对象
                        JSONObject tRequireJsonData = new JSONObject(tReceivedString);

                        //获取客户端请求的数据
                        String tConnectionState = tRequireJsonData.getString("ConnectionState");
                        int tTaskNumber = tRequireJsonData.getInt("TaskNumber");

                        //ConnectionState为Disconnect则退出循环，结束该连接
                        if (true == tConnectionState.equals("Disconnect")){
                            System.out.println(m_Client.getPort()+" 请求结束连接");
                            break;
                        }

                        //执行业务
                        executeTask(tTaskNumber , tSender);
                    }
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            } finally {
                try {
                    //结束该连接，从连接数组中删除该连接
                    if (true == m_ConnectionList.contains(m_Client))
                        m_ConnectionList.remove(m_Client);

                    //关闭连接
                    m_Client.close();
                    System.out.println("与 "+m_Client.getPort()+" 的连接已经关闭");
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    //启动服务器
    private void start() {
        System.out.println("服务器启动。。。。");

        try (ServerSocket tServerSocket = new ServerSocket(LISTENING_PORT)) {
            while (true) {
                //监听新的客户端连接
                Socket tClient = tServerSocket.accept();
                //当前连接数大于最大连接数则断开连接
                if(MAX_CONNECTIONS_NUMBER <= m_ConnectionList.size()){
                    try(PrintStream tSender = new PrintStream(tClient.getOutputStream())){
                        //构造错误信息，返回客户端
                        JSONObject tResponseJsonData = new JSONObject();
                        tResponseJsonData.put("ConnectionState" , "error");
                        tResponseJsonData.put("TaskExecuteInfo" , "no Task is execute");
                        tResponseJsonData.put("Error" , "There is so much clients connect to the Server , please try after a minute");
                        tSender.println(tResponseJsonData);
                        tClient.close();
                        continue;
                    }
                }

                //新建任务加入线程池来处理新的连接
                m_Executor.execute(new Task(tClient));
                //将新的连接加入列表来管理连接
                m_ConnectionList.add(tClient);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] vArgs) {
        TCPServer tTCPServer = new TCPServer();
        tTCPServer.start();
    }
}
