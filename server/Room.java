package server;

import java.io.File;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

public class Room {

    private String chatName; // chat room's name
    private ArrayList<Member> memberList; // member list who is in this room

    public Room(String name) throws Exception {
        this.chatName = name;
        this.memberList = new ArrayList<Member>();

        // create folder for file transmission
        File folder = new File("server/file/" + name);
        if(!folder.exists()) folder.mkdirs();
    }

    public Member getMemberBySocketChannel(SocketChannel socketChannel) {
        for(int i=0; i<memberList.size(); i++) {
            if(memberList.get(i).hasSocket(socketChannel)) return memberList.get(i);
        }
        return null;
    }

    // getter from chatName
    public String getChatName() {
        return this.chatName;
    }

    // add/remove member into/from this room
    public void addMember(Member member) {
        memberList.add(member);
    }
    public void removeMember(Member member) {
        memberList.remove(member);
    }

    // check that this member is in this room
    public boolean hasMember(Member member) {
        for(int i=0; i<memberList.size(); i++) {
            if(memberList.get(i) == member) return true;
        }
        return  false;
    }

    // send message to all members in this room
    public void sendToMembers(String message) throws Exception {
        for(int i=0; i<memberList.size(); i++) {
            memberList.get(i).sendMessage(message);
        }
    }

    // return information of this room
    public String printMember() {
        String result = "";
        for(int i=0; i<memberList.size(); i++) {
            result += "\"" + memberList.get(i).getNickName() + "\" ";
        }
        return result;
    }
}

