

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
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
            
            //대칭키 , iv 전달 받음
            byte[] secretKey_encrypted = (byte[])objectInputStream.readObject();
            byte[] iv_encrypted = (byte[])objectInputStream.readObject();
            System.out.println();
            System.out.println("Received AES Key: "+ new String(Base64.getEncoder().encode(secretKey_encrypted)));
            System.out.println();
            System.out.println("Received iv: "+ new String(Base64.getEncoder().encode(iv_encrypted)));
            System.out.println();
            //대칭키 복호화 , iv 복호화. 
            SecretKey secretKey = new SecretKeySpec(RSA_Decrypt(secretKey_encrypted, privateKey),"AES");
            IvParameterSpec iv = new IvParameterSpec(RSA_Decrypt(iv_encrypted,privateKey));
            //
            System.out.println("Descrypted AES Key: "+ new String(Base64.getEncoder().encode(secretKey.getEncoded())));
            System.out.println();
            System.out.println("Descrpyed iv: "+new String(Base64.getEncoder().encode(iv.getIV())));
            System.out.println();
            // 키교환 완료 



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
    public static byte[] RSA_Decrypt(byte[] encrypted,PrivateKey privateKey) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException{ 
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE,privateKey);
        byte[] decrypted = cipher.doFinal(encrypted);

        return decrypted; 
    }
    public static String AES256_Encrypt(SecretKey secretKey,IvParameterSpec iv,String in) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException{
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding"); // 자바에서 PKCS5 == PKCS7 
        c.init(Cipher.ENCRYPT_MODE,secretKey,iv);
        
        byte[] encrypted = c.doFinal(in.getBytes("UTF-8"));
        String out = new String(Base64.getEncoder().encode(encrypted));
        return out;
    } 
    public static String AES256_Decrypt(SecretKey secretKey,IvParameterSpec iv,String encrypted) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException{
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE,secretKey,iv);
        byte[] decrypted = Base64.getDecoder().decode(encrypted);
        return new String(cipher.doFinal(decrypted),"UTF-8");
        
    }
}
