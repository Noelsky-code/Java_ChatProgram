package NS;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Scanner;


public class Client {
    public static void main(String[] args){
        Socket socket = new Socket();


        OutputStream outputStream = null;
        DataOutputStream dataOutputStream = null;

        InputStream inputStream = null;
        DataInputStream dataInputStream = null;
        Scanner scanner = new Scanner(System.in);

        try{
            socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), 9000)); // 서버와 연결
            System.out.println("[client] connected with server");

            outputStream = socket.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);
            

            inputStream = socket.getInputStream();
            dataInputStream = new DataInputStream(inputStream);


            while(true){
                System.out.println("메시지 입력");
                
                String outMessage = scanner.nextLine();
                dataOutputStream.writeUTF(outMessage);// 입력받은 메시지 전송
                dataOutputStream.flush();
                
                String receiveString = dataInputStream.readUTF();

                System.out.println("받은 메시지: "+receiveString);
                
                if(receiveString.equals("exit")){// 클라이언트가 exit를 보내 서버를 종료 시키고 나서 .. 
                    
                    break;
                }
            }

        }
        catch(Exception e){
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
