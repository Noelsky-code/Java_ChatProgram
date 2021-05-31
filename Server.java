

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Scanner;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;
import java.text.SimpleDateFormat;





public class Server {
    static public void main(String[] args ){
        
        ServerSocket serverSocket = null;
        Socket socket = null;

        OutputStream  outputStream = null;
        DataOutputStream dataOutputStream = null;
        ObjectOutputStream objectOutputStream = null; 

        InputStream inputStream = null;
        DataInputStream dataInputStream = null;
        ObjectInputStream objectInputStream = null;
        
        try{
            serverSocket = new ServerSocket();
            InetAddress inetAddress = InetAddress.getLocalHost();
            String localhost = inetAddress.getHostAddress();
            serverSocket.bind(new InetSocketAddress(localhost, 9000));

            //System.out.println("[server] binding " + localhost);
            System.out.println("[server] waiting client");
            socket = serverSocket.accept();
            InetSocketAddress socketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
            System.out.println("[server] connected by client");
            System.out.println("[server] Connect with " + socketAddress.getHostString() + " " + socket.getPort());
            
            inputStream = socket.getInputStream();
            objectInputStream = new ObjectInputStream(inputStream); 
            dataInputStream = new DataInputStream(inputStream);

            outputStream = socket.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);
            objectOutputStream = new ObjectOutputStream(outputStream);
            Scanner scanner = new Scanner(System.in);
              
            //RSA 키 생성 
            System.out.println("Creating RSA Key Pair ...");
            KeyPair keyPair = genRSAKeyPair();
            PrivateKey privateKey = keyPair.getPrivate();    
            PublicKey publicKey = keyPair.getPublic();
            // 공개키 전달.
            objectOutputStream.writeObject(publicKey);
            objectOutputStream.flush();
            //System.out.println(privateKey);
            //System.out.println(publicKey); 
            
            

            while(true){
                //System.out.println(123123);
                String clientMessage = dataInputStream.readUTF(); // 클라이언트로 부터 UTF 인코딩으로 받음
                System.out.println("[server] Received: \""+ clientMessage+ "\" "+get_date());
                if(clientMessage.equals("exit")){
                    dataOutputStream.writeUTF("exit");//클라이언트가 exit 입력 -> 클라이언트에게 exit 메시지 전달
                    dataOutputStream.flush();
                    socket.close();
                    break;
                }
                String outMessage = scanner.nextLine();
                if(outMessage.equals("exit")){//서버가 exit 입력 -> 클라이언트에게 전달 ,종료 
                    dataOutputStream.writeUTF("exit");
                    dataOutputStream.flush();
                    socket.close();
                    break;
                }
                dataOutputStream.writeUTF(outMessage);// 클라이언트로 보내줌 
                dataOutputStream.flush(); //버퍼 FLUSH 
                

                
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
    public static String get_date(){
        Date date= new Date();
        SimpleDateFormat simpl = new SimpleDateFormat("[yyyy/mm/dd/  hh:mm:ss]");
        String s= simpl.format(date);
        return s;
    }
    public static KeyPair genRSAKeyPair() throws NoSuchAlgorithmException{
        SecureRandom secureRandom = new SecureRandom();
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048,secureRandom);
        KeyPair keyPair = gen.genKeyPair();
        return keyPair;
    }
}
