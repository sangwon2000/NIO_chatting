package server;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Member {

    private SocketChannel chatSocketChannel; // used for sending message to client's chatSocket
    private SocketChannel fileSocketChannel; // used for file transmission
    private String nickName; // current nickname in room

    public Member(SocketChannel chatSocketChannel, SocketChannel fileSocketChannel) {
        this.chatSocketChannel = chatSocketChannel;
        this.fileSocketChannel = fileSocketChannel;
        this.nickName = "nameless";
    }

    // return if this member has this Socket Channel
    public boolean hasSocket(SocketChannel socketChannel) {
        return (chatSocketChannel == socketChannel || fileSocketChannel == socketChannel);
    }

//------------------------------------------------------------------------------------------
    // getter and setter for nickName
    public String getNickName() {
        return nickName;
    }
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }
//------------------------------------------------------------------------------------------

    // send message to client's chatSocket
    public void sendMessage(String message) throws Exception {
        message += "\n";
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        chatSocketChannel.write(buffer);
    }

    // receive file from client's fileSocket
    public void receiveFile(String path) throws Exception {
        File file = new File(path);
        if(file.exists()) file.delete();
        file.createNewFile();

        FileOutputStream fos = new FileOutputStream(file);

        System.out.println("----- receive start -----");
        while(true) {
            Thread.sleep(100);
            ByteBuffer buffer = ByteBuffer.allocate(65536);
            int length = fileSocketChannel.read(buffer);
            fos.write(buffer.array(),0,length);
            System.out.print("#");
            if(length < 65536) break;
        }
        System.out.println("\n----- receive complete -----");

    }

    // send file to client's fileSocket
    public void sendFile(String path) throws Exception {
        File file = new File(path);
        if(file.exists()) {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

            String[] split = path.split("/");
            sendMessage("#PUT " + split[3]);

            System.out.println("----- send start -----");
            while(bis.available() > 0) {
                byte[] buffer = new byte[65536];
                int length = bis.read(buffer);
                fileSocketChannel.write(ByteBuffer.wrap(buffer,0,length));
                System.out.print("#");
            }
            System.out.println("\n----- send complete -----");

        }
        else sendMessage("File not uploaded to this chat room.");
    }
}
