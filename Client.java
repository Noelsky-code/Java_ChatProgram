

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
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
import java.text.SimpleDateFormat;


public class Client {
    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException{
        
        /*
        //AES256 테스트 -> 제대로 수행됨 
        System.out.println();
        
        SecretKey secretKey = secretKey_generator();
        IvParameterSpec iv = Iv_generator();
        
        System.out.println(iv.getIV());
        System.out.println(secretKey.getEncoded());
        String in = "hansol";
        String enc = AES256_Encrypt(secretKey,iv,in);
        System.out.println(enc);
        String dec = AES256_Decrypt(secretKey,iv,enc);
        System.out.println(dec);
        */





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
        objectOutputStream = new ObjectOutputStream(outputStream);
    
        inputStream = socket.getInputStream();
        dataInputStream = new DataInputStream(inputStream);
        objectInputStream = new ObjectInputStream(inputStream);

        //공개키 받기  
        PublicKey publicKey = (PublicKey) objectInputStream.readObject();
        // 공개키 출력 .. 어떻게? 
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
        objectOutputStream.writeObject(secretKey_encrypted);
        objectOutputStream.flush();
        objectOutputStream.writeObject(iv_encrypted);
        objectOutputStream.flush();

        // 키교환 완료         



        //데이터 교환 
        while(true){
            System.out.printf(">");
            //client 메시지 암호화 후 전달 
            String outMessage = scanner.nextLine();
            String outMessage_encrpyted = AES256_Encrypt(secretKey, iv, outMessage);
            dataOutputStream.writeUTF(outMessage_encrpyted);
            dataOutputStream.flush();
                
            //server 메시지 복호화 후 출력
            String serverMessage_encrypted = dataInputStream.readUTF();
            String serverMessage = AES256_Decrypt(secretKey, iv, serverMessage_encrypted);
            

            System.out.println("[client] Received: \""+serverMessage+"\" "+get_date());
            System.out.println("[client] Encrypted: \""+serverMessage_encrypted+"\"");    
            if(serverMessage.equals("exit")){// 클라이언트가 exit를 보내 서버를 종료 시키고 나서 .. 
                socket.close();
                //System.out.println("Connection closed");
                break;
            }
        }

    }
    public static String get_date(){
        LocalDateTime date= LocalDateTime.now();
        //SimpleDateFormat simpl = new SimpleDateFormat("[yyyy/mm/dd/  hh:mm:ss]");
        String s= date.format(DateTimeFormatter.ofPattern("yyyy/mm/dd hh:mm:ss"));
        //String s= simpl.format(date);
        return "["+s+"]";
    }
    public static SecretKey secretKey_generator() throws NoSuchAlgorithmException{
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        SecureRandom rand = new SecureRandom();
        keyGen.init(256,rand);
        SecretKey secretKey = keyGen.generateKey();
        return secretKey;
    }
    public static IvParameterSpec Iv_generator(){
        SecureRandom rand = new SecureRandom();
        byte[] iv = new byte[16];
        rand.nextBytes(iv);
        IvParameterSpec ivParams = new IvParameterSpec(iv);
        return ivParams;

    }
    public static String AES256_Encrypt(SecretKey secretKey,IvParameterSpec iv,String in) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException{
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE,secretKey,iv);
        
        byte[] encrypted = c.doFinal(in.getBytes("UTF-8"));
        String out = new String(Base64.getEncoder().encode(encrypted));
        return out;
    } 
    public static String AES256_Decrypt(SecretKey secretKey,IvParameterSpec iv,String encrypted) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException{
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");// 자바에서 PKCS5 == PKCS7
        cipher.init(Cipher.DECRYPT_MODE,secretKey,iv);
        byte[] decrypted = Base64.getDecoder().decode(encrypted);
        return new String(cipher.doFinal(decrypted),"UTF-8");
        
    }
    public static byte[] RSA_Encrypt(byte[] plaintext,PublicKey publicKey) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException{
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE,publicKey);
        byte[] encrypted = cipher.doFinal(plaintext);

        return encrypted; 
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