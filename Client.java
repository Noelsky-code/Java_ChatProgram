

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Scanner;


public class Client {
    public static void main(String[] args) throws IOException, InterruptedException{
        Socket socket = null;

        OutputStream outputStream = null;
        DataOutputStream dataOutputStream = null;

        InputStream inputStream = null;
        DataInputStream dataInputStream = null;
        
        Scanner scanner = new Scanner(System.in);


        //서버 연결 기다림 
        
        boolean scanning = true;
        while(scanning){
            try{
                //SocketChannel.open(new InetSocketAddress(InetAddress.getLocalHost(), 9000));
                socket = new Socket(InetAddress.getLocalHost(), 9000);
                scanning =false;
            }
            catch(java.net.ConnectException e) {
                System.out.println("[client] waiting server");
                try {
                    Thread.sleep(2000);//2 seconds
                } catch(InterruptedException ie){
                    ie.printStackTrace();
                }
            } 
            catch(java.net.SocketException e){
                System.out.println("[client] waiting server");
                try {
                    Thread.sleep(2000);//2 seconds
                } catch(InterruptedException ie){
                    ie.printStackTrace();
                }
            }
        }
    
        
       //socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), 9000)); // 서버와 연결
        System.out.println("[client] connected with server");

        outputStream = socket.getOutputStream();
        dataOutputStream = new DataOutputStream(outputStream);
            

        inputStream = socket.getInputStream();
        dataInputStream = new DataInputStream(inputStream);

        while(true){
            System.out.println("[client] Write Message: ");
                
            String outMessage = scanner.nextLine();
            dataOutputStream.writeUTF(outMessage);// 입력받은 메시지 전송
            dataOutputStream.flush();
                
            String receiveString = dataInputStream.readUTF();

            System.out.println("[client] Received: "+receiveString);
                
            if(receiveString.equals("exit")){// 클라이언트가 exit를 보내 서버를 종료 시키고 나서 .. 
                socket.close();
                break;
            }
        }

    }
        
}

/*

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
        }*/