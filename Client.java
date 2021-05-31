

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
import java.util.Date;
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
        String enc = AES256_Encode(secretKey,iv,in);
        System.out.println(enc);
        String dec = AES256_Decode(secretKey,iv,enc);
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
        objectOutputStream = new ObjectOutputStream(outputStream);
    
        inputStream = socket.getInputStream();
        dataInputStream = new DataInputStream(inputStream);
        objectInputStream = new ObjectInputStream(inputStream);

        //공개키 받기  
        PublicKey publicKey = (PublicKey) objectInputStream.readObject();
        System.out.println("Received Public Key: "+publicKey);

        //대칭키 , iv 생성
        SecretKey secretKey = secretKey_generator();
        IvParameterSpec iv = Iv_generator();
        




        while(true){
            System.out.println("[client] Write Message: ");
                
            String outMessage = scanner.nextLine();
            dataOutputStream.writeUTF(outMessage);// 입력받은 메시지 전송
            dataOutputStream.flush();
                
            String receiveString = dataInputStream.readUTF();

            System.out.println("[client] Received: \""+receiveString+"\" "+get_date());
                
            if(receiveString.equals("exit")){// 클라이언트가 exit를 보내 서버를 종료 시키고 나서 .. 
                socket.close();
                break;
            }
        }

    }
    static String get_date(){
        Date date= new Date();
        SimpleDateFormat simpl = new SimpleDateFormat("[yyyy/mm/dd/  hh:mm:ss]");
        String s= simpl.format(date);
        return s;
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
    public static String AES256_Encode(SecretKey secretKey,IvParameterSpec iv,String in) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException{
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE,secretKey,iv);
        
        byte[] encrypted = c.doFinal(in.getBytes("UTF-8"));
        String out = new String(Base64.getEncoder().encode(encrypted));
        return out;
    } 
    public static String AES256_Decode(SecretKey secretKey,IvParameterSpec iv,String encrypted) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException{
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE,secretKey,iv);
        byte[] decrypted = Base64.getDecoder().decode(encrypted);
        return new String(cipher.doFinal(decrypted),"UTF-8");
        
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