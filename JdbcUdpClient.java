package jdbcudpclient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JdbcUdpClient {

    private DatagramSocket socket;
    private DatagramPacket packet;
    private final int receivePort = 16000;
    private final int sendPort = 16001;
    private final int bufSize = 1024;
    private int newReceivePort;

    static private final String INET_ADDRESS = "localhost";

    public JdbcUdpClient() {
        try {
            socket = new DatagramSocket();//Socket for any free port
        } catch (SocketException ex) {
            Logger.getLogger(JdbcUdpClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void JdbcUdpClientClose() {
        socket.close();
    }

    private boolean isWorking = true;

    public boolean IsWorking() {
        return isWorking;
    }

    public void PreStart() {
        byte[] buffer = new byte[bufSize];
        //Send request to a known port
        try {
            packet = new DatagramPacket(buffer, buffer.length);
            packet.setAddress(InetAddress.getByName(INET_ADDRESS));
            packet.setPort(receivePort);
            socket.send(packet);

        } catch (UnknownHostException ex) {
            Logger.getLogger(JdbcUdpClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(JdbcUdpClient.class.getName()).log(Level.SEVERE, null, ex);
        }

        packet = new DatagramPacket(buffer, buffer.length);
        //Get a new port
        try {
            socket.receive(packet);
            String packetData = new String(packet.getData());
            String[] newPort = packetData.split("(:|;)");
            this.newReceivePort = Integer.parseInt(newPort[1]);
            System.out.println(packetData);
            //write new port

        } catch (IOException ex) {
            Logger.getLogger(JdbcUdpClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String[] DoQuery(String textQuery) {
        String[] response = new String[0];
        try {
            byte[] inBuffer = textQuery.getBytes();
            packet = new DatagramPacket(inBuffer, inBuffer.length);
            packet.setAddress(InetAddress.getByName(INET_ADDRESS));
            packet.setPort(this.newReceivePort);
            socket.send(packet);

            byte[] outBuffer = new byte[bufSize];
            packet = new DatagramPacket(outBuffer, outBuffer.length);
            socket.receive(packet);
            String responseString = new String(packet.getData());
            response = new String[]{responseString};
        } catch (UnknownHostException ex) {
            Logger.getLogger(JdbcUdpClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(JdbcUdpClient.class.getName()).log(Level.SEVERE, null, ex);
        }

        return response;
    }

    public static boolean isDidit(String digit) {
        return digit.trim().matches("[0-9]*");
    }

    public String[] getHistory(String id) {
        String TAMPLATE_GET_HISTORY = "function=getHistory;argc=1;";
        String textQuery;
        String[] response = new String[0];
        if (isDidit(id)) {
            int code = Integer.parseInt(id);
            
            textQuery = TAMPLATE_GET_HISTORY + code + ";";
        } else {
            
            textQuery = TAMPLATE_GET_HISTORY + id + ";";
        }
        return response = DoQuery(textQuery);
    }

    public boolean login(String user, String password) {
        //Create request
        String textQuery = "function=login;argc=2;" + user + ";" + password + ";";
        //get response
        String[] response = DoQuery(textQuery);
        //processing
        String regex = ".*true.*";
        String responseText = response[0];
        Pattern patern = Pattern.compile(regex);
        Matcher m = patern.matcher(responseText);

        return m.matches();
    }

    
    public boolean logout() {
        
        String textQuery = "function=logout;argc=0;";
        
        String[] response = DoQuery(textQuery);
        
        String regex = ".*true.*";
        String responseText = response[0];
        Pattern patern = Pattern.compile(regex);
        Matcher m = patern.matcher(responseText);

        return m.matches();
    }

    public static void main(String[] args) {
        JdbcUdpClient appClient = new JdbcUdpClient();
        Scanner scan = new Scanner(System.in);
        //Ferst request
        appClient.PreStart();
        //Go to working port
        System.out.println("Login : ");
        String login = scan.next();
        System.out.println("Password : ");
        String password = scan.next();
        if (null != login && null != password) {
            //login in new port
            if (appClient.login(login, password)) {
                while (appClient.IsWorking()) {
                    
                    System.out.println("Enter the employee ID or Last name, or N to exit : ");
                    String id = scan.next();
                    if (("n".equals(id)) || ("N".equals(id))) {
                        appClient.logout();
                        System.out.println("Have a nice day.");
                        appClient.JdbcUdpClientClose();
                        System.exit(0);
                    } else {
                        System.out.println(Arrays.toString(appClient.getHistory(id)));
                    }
                    
                }
            } else {
                appClient.JdbcUdpClientClose();
                System.exit(0);
            }
        }
    }
}
