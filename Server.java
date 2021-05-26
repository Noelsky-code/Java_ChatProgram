import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    static public void main(String[] args ){
        
        ServerSocket serverSocket = null
        Socket socket = null;
        OutputStream  outputStream = null;
        DataOutputStream dataOutputStream = null;
        InputStream inputStream = null;
        DataInputStream dataInputStream = null;
        try{
            serverSocket = new ServerSocket(9000);
            socket = serverSocket.accept();
            inputStream = socket.getInputStream();
            dataInputStream = new DataInputStream(inputStream);

            outputStream = socket.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);

            while(true){
                String clientMessage = dataInputStream.readUTF(); // 클라이언트로 부터 UTF 인코딩으로 받음
                System.out.println("Received: "+ clientMessage+"time");
                dataOutputStream.writeUTF("메시지 전송 완료");// 클라이언트로 보내줌 
                dataOutputStream.flush(); //버퍼 FLUSH 
                if(clientMessage.equals("exit")){
                    dataOutputStream.writeUTF("exit");//클라이언트에게 exit 메시지 전달
                    System.out.println("Received: exit"+"time");
                    break;
                }
            }
            /*
            RSA 키 생성 
            
            */
        }catch(Exception e){
            e.printStackTrace();
        }
        finally{
            try{
                if(dataOutputStream!=null)dataOutputStream.close();
                if(outputStream!=null)outputStream.close();
                if(dataInputStream!=null)dataInputStream.close();
                if(inputStream!=null)inputStream.close();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

    }
}
