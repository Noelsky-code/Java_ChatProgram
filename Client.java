

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.net.InetAddress;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import java.util.Base64;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Client extends Thread {

    //쓰레드에서 사용할 객체들 
    String id;
    SecretKey secretKey;
    IvParameterSpec iv;
    Socket socket;
    DataOutputStream dataOutputStream;
    DataInputStream dataInputStream;

    
    public Client(String id,SecretKey secretkey, IvParameterSpec iv,Socket socket, DataOutputStream dataOutputStream, DataInputStream dataInputStream){
        this.id=id;
        this.secretKey = secretkey;
        this.iv=iv;
        this.socket=socket;
        this.dataOutputStream = dataOutputStream;
        this.dataInputStream= dataInputStream;
    }

    
    @Override
    public void run(){
        if(id.equals("read")){//read 쓰레드일 경우 
            while(true){

                try{
                    //server 메시지 복호화 후 출력
                    String serverMessage_encrypted = dataInputStream.readUTF();
                    String serverMessage = AES256_Decrypt(secretKey, iv, serverMessage_encrypted);

                    System.out.println("[client] Received: \""+serverMessage+"\" "+get_date());
                    System.out.println("[client] Encrypted: \""+serverMessage_encrypted+"\"");
                    System.out.println();    
                    if(serverMessage.equals("exit")){//서버가 exit을 보낼 경우 -> 쓰레드 종료 -> 소켓 종료 -> 에러발생 -> 모든 쓰레드 종료. 
                        break;
                    }
                }catch (Exception e) {//소켓 닫혀 에러 발생시 쓰레드 종료. 
                    break;
                }
            }
        }
        else if(id.equals("write")){//Write일 경우 
            Scanner scanner=new Scanner(System.in);
            
            while(true){
                try{
                    //client 메시지 암호화 후 전달 
                    String outMessage = scanner.nextLine();
                    String outMessage_encrpyted = AES256_Encrypt(secretKey, iv, outMessage);
                    dataOutputStream.writeUTF(outMessage_encrpyted);
                    dataOutputStream.flush();
                    
                }
                catch(Exception e){// 소켓이 닫혀 에러가 발생하면 종료시켜줌. 
                    break;
                }
            
            }
            System.out.println("[client] Connection closed");//write 쓰레드가 제일 나중에 종료되므로 

        }


    }


    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException{
        


        Socket socket = null;

        OutputStream outputStream = null;
        DataOutputStream dataOutputStream = null;
        ObjectOutputStream objectOutputStream = null;

        InputStream inputStream = null;
        DataInputStream dataInputStream = null;
        ObjectInputStream objectInputStream = null;
        
        Scanner scanner = new Scanner(System.in);


        //서버 연결 기다림  -> 서버 존재 x -> 연결안됨 예외 발생 -> loop 돌도록 설정. 
        
        boolean scanning = true;
        while(scanning){
            try{
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
    
        
       //서버 연결 및 스트림 객체 생성. 
        System.out.println("[client] connected with server");

        try{
            outputStream = socket.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);
            objectOutputStream = new ObjectOutputStream(outputStream);
    
            inputStream = socket.getInputStream();
            dataInputStream = new DataInputStream(inputStream);
            objectInputStream = new ObjectInputStream(inputStream);

            //공개키 받기  
            PublicKey publicKey = (PublicKey) objectInputStream.readObject();
            // 공개키 출력 
            System.out.println();
            System.out.println("[client] Received Public Key: " +new String(Base64.getEncoder().encode(publicKey.getEncoded()))  );
            System.out.println();
            //대칭키 , iv 생성
            System.out.println("[client] Creating AES 256 KEY...");
            SecretKey secretKey = secretKey_generator();
            IvParameterSpec iv = Iv_generator();
            System.out.println();
            //대칭키 출력 .. 인코딩 
            System.out.println("[client] AES 256 KEY : "+new String(Base64.getEncoder().encode(secretKey.getEncoded())));
            System.out.println();
            System.out.println("[client] iv: "+new String(Base64.getEncoder().encode(iv.getIV())));
            System.out.println();
            
            //대칭키 암호화 , iv 암호화. 
            byte[] secretKey_encrypted = RSA_Encrypt(secretKey.getEncoded(), publicKey);
            byte[] iv_encrypted = RSA_Encrypt(iv.getIV(), publicKey);
            //암호화된 대칭키, iv 전달. 
            System.out.println("[client] Encrypted AES Key: "+new String(Base64.getEncoder().encode(secretKey_encrypted)));
            System.out.println();
            System.out.println("[client] Encrypted iv: "+ new String(Base64.getEncoder().encode(iv_encrypted)));
            System.out.println();
            // 대칭키교환 
            objectOutputStream.writeObject(secretKey_encrypted);
            objectOutputStream.flush();
            objectOutputStream.writeObject(iv_encrypted);
            objectOutputStream.flush();

            //데이터 교환 파트 시작 

            // read,write 쓰레드 생성
            Client t1 = new Client("read",secretKey,iv,socket,dataOutputStream,dataInputStream);
            Client t2 = new Client("write",secretKey,iv,socket,dataOutputStream,dataInputStream);
            // 쓰레드 시작
            t1.start();
            t2.start();
            //t1이 먼저 종료되게 설정 -> t1종료 시 socket 닫아줌 -> 모든 쓰레드 종료
            //만약 서버에서 socket이 close 되어있어도 상관x 
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
    //시간 정보 얻기위한 함수
    public static String get_date(){
        LocalDateTime date= LocalDateTime.now();
        String s= date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm:ss"));

        return "["+s+"]";
    }
    //대칭키 생성 함수 
    public static SecretKey secretKey_generator() throws NoSuchAlgorithmException{
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        SecureRandom rand = new SecureRandom();
        keyGen.init(256,rand);
        SecretKey secretKey = keyGen.generateKey();
        return secretKey;
    }
    //iv 생성 함수 
    public static IvParameterSpec Iv_generator(){
        SecureRandom rand = new SecureRandom();
        byte[] iv = new byte[16];
        rand.nextBytes(iv);
        IvParameterSpec ivParams = new IvParameterSpec(iv);
        return ivParams;

    }
    //aes256 암호화
    public static String AES256_Encrypt(SecretKey secretKey,IvParameterSpec iv,String in) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException{
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE,secretKey,iv);
        
        byte[] encrypted = c.doFinal(in.getBytes("UTF-8"));
        String out = new String(Base64.getEncoder().encode(encrypted),"UTF-8");
        return out;
    } 
    //aes256 복호화
    public static String AES256_Decrypt(SecretKey secretKey,IvParameterSpec iv,String encrypted) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException{
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");// 자바에서 PKCS5 == PKCS7
        cipher.init(Cipher.DECRYPT_MODE,secretKey,iv);
        byte[] decrypted = Base64.getDecoder().decode(encrypted);
        return new String(cipher.doFinal(decrypted),"UTF-8");
        
    }
    //rsa 암호화 
    public static byte[] RSA_Encrypt(byte[] plaintext,PublicKey publicKey) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException{
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE,publicKey);
        byte[] encrypted = cipher.doFinal(plaintext);

        return encrypted; 
    }

}
