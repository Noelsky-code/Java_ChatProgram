

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;



import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;






public class Server extends Thread{
    
    String id;
    SecretKey secretKey;
    IvParameterSpec iv;
    Socket socket;
    DataOutputStream dataOutputStream;
    DataInputStream dataInputStream;

    public Server(String id,SecretKey secretkey, IvParameterSpec iv,Socket socket, DataOutputStream dataOutputStream, DataInputStream dataInputStream){
        this.id=id;
        this.secretKey = secretkey;
        this.iv=iv;
        this.socket=socket;
        this.dataOutputStream = dataOutputStream;
        this.dataInputStream= dataInputStream;
    }
    @Override
    public void run(){
        if(id.equals("read")){//read일 경우 
            while(true){

                try{
                    //클라이언트가 보낸 메시지 복호, 출력 
                    String clientMessage_encrypted = dataInputStream.readUTF();
                    String clientMessage = AES256_Decrypt(secretKey, iv, clientMessage_encrypted);
                    System.out.println("[server] Received: \""+ clientMessage+ "\" "+get_date());
                    System.out.println("[server] Encrypted Message: \""+ clientMessage_encrypted+"\"");
                    
                    //클라이언트가 exit 신호 보냈을 때 
                    if(clientMessage.equals("exit")){
                        break;//종료 
                    }

                } catch (Exception e) {
                    break;
                }

            }
        }
        else if(id.equals("write")){//Write일 경우 
            Scanner scanner=new Scanner(System.in);
            
            while(true){
                try{
                    //server메시지 암호화후 전달. 
                    String outMessage = scanner.nextLine();
                    String outMessage_encrypted= AES256_Encrypt(secretKey, iv, outMessage);;
                    System.out.println();
                    dataOutputStream.writeUTF(outMessage_encrypted);// 클라이언트로 보내줌 
                    dataOutputStream.flush(); //버퍼 FLUSH
                    
                }
                catch(Exception e){// 소켓이 닫혀 에러가 발생하면 종료시켜줌. 
                    break;
                }
            
            }
            System.out.println("[server] Connection closed");//write 쓰레드가 제일 나중에 종료됨. 
        }


    }

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
            System.out.println("[server] Creating RSA Key Pair ...");
            KeyPair keyPair = genRSAKeyPair();
            PrivateKey privateKey = keyPair.getPrivate();    
            PublicKey publicKey = keyPair.getPublic();
            // 공개키 전달.
            objectOutputStream.writeObject(publicKey);
            objectOutputStream.flush();
            //System.out.println(privateKey);
            //System.out.println(publicKey);
            
            //대칭키 , iv 전달 받음
            byte[] secretKey_encrypted = (byte[])objectInputStream.readObject();
            byte[] iv_encrypted = (byte[])objectInputStream.readObject();
            System.out.println();
            System.out.println("[server] Received AES Key: "+ new String(Base64.getEncoder().encode(secretKey_encrypted)));
            System.out.println();
            System.out.println("[server] Received iv: "+ new String(Base64.getEncoder().encode(iv_encrypted)));
            System.out.println();
            //대칭키 복호화 , iv 복호화. 
            SecretKey secretKey = new SecretKeySpec(RSA_Decrypt(secretKey_encrypted, privateKey),"AES");
            IvParameterSpec iv = new IvParameterSpec(RSA_Decrypt(iv_encrypted,privateKey));
            //키 교환 완료 + 출력 
            System.out.println("[server] Descrypted AES Key: "+ new String(Base64.getEncoder().encode(secretKey.getEncoded())));
            System.out.println();
            System.out.println("[server] Descrpyed iv: "+new String(Base64.getEncoder().encode(iv.getIV())));
            System.out.println();
            //read, write 쓰레드 생성 
            Server t1 = new Server("read",secretKey,iv,socket,dataOutputStream,dataInputStream);
            Server t2 = new Server("write",secretKey,iv,socket,dataOutputStream,dataInputStream);
            //쓰레드 시작
            t1.start();
            t2.start();
            //t1 먼저 종료 => t1 종료시 socket 닫아줌 -> 모든 쓰레드 종료
            //만약 클라이언트에서 socket이 close되었어도 상관 x 
            t1.join();
            socket.close();
            if(dataOutputStream!=null)dataOutputStream.close();
            if(outputStream!=null)outputStream.close();
            if(dataInputStream!=null)dataInputStream.close();
            if(inputStream!=null)inputStream.close();
            t2.join();
        
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally{
            //만약 main에서 exception 발생시 stream 종료해줌. 
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
    //날짜 정보 얻기 함수
    public static String get_date(){
        LocalDateTime date= LocalDateTime.now();
        String s= date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm:ss"));
        return "["+s+"]";
    }
    //rsa 키 생성 
    public static KeyPair genRSAKeyPair() throws NoSuchAlgorithmException{
        SecureRandom secureRandom = new SecureRandom();
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048,secureRandom);
        KeyPair keyPair = gen.genKeyPair();
        return keyPair;
    }
    //rsa 복호화
    public static byte[] RSA_Decrypt(byte[] encrypted,PrivateKey privateKey) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException{ 
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE,privateKey);
        byte[] decrypted = cipher.doFinal(encrypted);

        return decrypted; 
    }
    //aes256 암호화
    public static String AES256_Encrypt(SecretKey secretKey,IvParameterSpec iv,String in) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException{
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding"); // 자바에서 PKCS5 == PKCS7 
        c.init(Cipher.ENCRYPT_MODE,secretKey,iv);
        
        byte[] encrypted = c.doFinal(in.getBytes("UTF-8"));
        String out = new String(Base64.getEncoder().encode(encrypted),"UTF-8");
        return out;
    }
    //aes256 복호화
    public static String AES256_Decrypt(SecretKey secretKey,IvParameterSpec iv,String encrypted) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException{
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE,secretKey,iv);
        byte[] decrypted = Base64.getDecoder().decode(encrypted);
        return new String(cipher.doFinal(decrypted),"UTF-8");
        
    }
}

