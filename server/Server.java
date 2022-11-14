package server;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class Server {

    static private ArrayList<Room> roomList; // current room list
    static private ServerSocketChannel chatServerChannel; // Socket for connecting new socket to client's chatSocket
    static private ServerSocketChannel fileServerChannel; // Socket for connecting new socket to client's fileSocket

    static private Selector selector;

    public static void main(String[] args) {
        try {
            // port setting
            int port1 = Integer.parseInt(args[0]);
            int port2 = Integer.parseInt(args[1]);

            // create folder for file transmission
            File folder = new File("server/file");
            if(!folder.exists()) folder.mkdirs();


            selector = Selector.open();

            // create sockets
            chatServerChannel = ServerSocketChannel.open();
            chatServerChannel.configureBlocking(false);
            chatServerChannel.bind(new InetSocketAddress(port1));
            chatServerChannel.register(selector, SelectionKey.OP_ACCEPT);

            fileServerChannel = ServerSocketChannel.open();
            fileServerChannel.configureBlocking(false);
            fileServerChannel.bind(new InetSocketAddress(port2));
            fileServerChannel.register(selector, SelectionKey.OP_ACCEPT);

            // create waiting room
            roomList = new ArrayList<Room>();
            roomList.add(new Room("Waiting Room"));

            System.out.println("The server was successfully opened");

            // accept client and add to waiting room
            while(true) {

                int keyCount = selector.select();
                if(keyCount == 0) continue;
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                SocketChannel chatSocketChannel = null;
                SocketChannel fileSocketChannel = null;

                while(iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();

                    if(selectionKey.isAcceptable()) {
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel)selectionKey.channel();
                        if(serverSocketChannel == chatServerChannel)
                            chatSocketChannel = serverSocketChannel.accept();
                        else fileSocketChannel = serverSocketChannel.accept();

                        if(chatSocketChannel != null && fileSocketChannel != null) {

                            Member member = new Member(chatSocketChannel, fileSocketChannel);
                            goWaitingRoom(member);
                            System.out.println("Connection complete");

                            chatSocketChannel = null;
                            fileSocketChannel = null;
                        }
                    }

                    if(selectionKey.isReadable()) {
                        SocketChannel socketChannel = (SocketChannel)selectionKey.channel();
                        Member member = socketChannelToMember(socketChannel);

                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int length = socketChannel.read(buffer);
                        String message = buffer.array().toString().substring(0,length);

                        workMessage(member,message);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

    }
//-----------------------------------------------------------------------------------

    private static Member socketChannelToMember(SocketChannel socketChannel) {
        for(int i=1; i<roomList.size(); i++) {
            Member member = roomList.get(i).getMemberBySocketChannel(socketChannel);
            if(member != null) {
                return member;
            }
        }
        return null;
    }

    private static void workMessage(Member member, String message) throws Exception {
        String[] split = message.split(" ");

        if(split[0].equalsIgnoreCase("#CREATE"))
            createRoom(member,split[1],split[2]);
        else if(split[0].equalsIgnoreCase("#JOIN"))
            joinRoom(member,split[1],split[2]);
        else if(split[0].equalsIgnoreCase("#EXIT"))
            exitRoom(member);
        else if(split[0].equalsIgnoreCase("#STATUS"))
            statusRoom(member);
        else if(split[0].equalsIgnoreCase("#PUT"))
            putFile(member,split[1]);
        else if(split[0].equalsIgnoreCase("#GET"))
            getFile(member,split[1]);
        else chatRoom(member,message);
    }

    // methods using deal with client's message
    public static void createRoom(Member member, String chatName, String nickName) throws Exception {
        for(int i=1; i<roomList.size(); i++) {
            if(roomList.get(i).getChatName().equals(chatName)) {
                member.sendMessage(("Duplicate chat room name exists."));
                return;
            }
        }
        roomList.add(new Room(chatName));
        goChatRoom(member, chatName, nickName);
        System.out.println("\"" + chatName + "\"" + " has been opened.");
        member.sendMessage("***** You've entered \"" + chatName + "\" *****");
    }

    public static void joinRoom(Member member, String chatName, String nickName) throws Exception {
        if(goChatRoom(member,chatName,nickName) == 1)
            member.sendMessage("***** You've entered \"" + chatName + "\" *****");
        else member.sendMessage("This chat room does not exist.");
    }

    public static void exitRoom(Member member) throws Exception {
        if(goWaitingRoom(member) == 1) member.sendMessage("Go to the waiting room.");
        else member.sendMessage("already in the waiting room.");
    }

    public static void chatRoom(Member member, String message) throws Exception {
        for(int i=1; i<roomList.size(); i++) {
            if(roomList.get(i).hasMember(member)) {
                roomList.get(i).sendToMembers("FROM " + member.getNickName() + ": "+ message);
                return;
            }
        }
        member.sendMessage("Please join the chat room first.");
    }

    public static void statusRoom(Member member) throws Exception {
        for(int i=0; i<roomList.size(); i++) {
            if(roomList.get(i).hasMember(member)) {
                member.sendMessage("chatName: \"" + roomList.get(i).getChatName() + "\" user: " + roomList.get(i).printMember());
                return;
            }
        }
        member.sendMessage("Please join the chat room first.");
    }

    private static int goWaitingRoom(Member member) {
        if(roomList.get(0).hasMember(member) == false) {
            for(int i=0; i<roomList.size(); i++)
                roomList.get(i).removeMember(member);
            member.setNickName("nameless");
            roomList.get(0).addMember(member);
            return 1;
        }
        return 0;
    }

    private static int goChatRoom(Member member, String chatName, String nickName) throws Exception {
        goWaitingRoom(member);
        for(int i=1; i<roomList.size(); i++) {
            if(roomList.get(i).getChatName().equals(chatName)) {
                roomList.get(0).removeMember(member);
                member.setNickName(nickName);
                roomList.get(i).addMember(member);
                return 1;
            }
        }
        return 0;
    }

    public static void putFile(Member member, String fileName) throws Exception{
        String chatName = null;
        for(int i=0; i<roomList.size(); i++) {
            if(roomList.get(i).hasMember(member)) {
                chatName = roomList.get(i).getChatName();
                break;
            }
        }
        member.receiveFile( "server/file/" + chatName + "/" + fileName);
    }

    public static void getFile(Member member, String fileName) throws Exception{
        String chatName = null;
        for(int i=0; i<roomList.size(); i++) {
            if(roomList.get(i).hasMember(member)) {
                chatName = roomList.get(i).getChatName();
                break;
            }
        }
        member.sendFile( "server/file/" + chatName + "/" + fileName);
    }

}
