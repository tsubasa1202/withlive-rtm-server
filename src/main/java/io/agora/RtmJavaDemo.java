package io.agora.mainClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmMessage;

import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmChannel;
import io.agora.rtm.RtmChannelListener;
import io.agora.rtm.RtmChannelMember;
import io.agora.rtm.RtmStatusCode;
import io.agora.rtm.RtmChannelAttribute;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

class APPID {
    public static final String APP_ID = "30c649a7e7c041c7bd628f673246715e";
}

class ChannelListener implements RtmChannelListener {
    private String channel_;

    public ChannelListener(String channel) {
            channel_ = channel;
    }

    @Override
    public void onMemberCountUpdated(int memberCount) {
    }

    @Override
    public void onAttributesUpdated(List<RtmChannelAttribute> attribute) {
    }

    @Override
    public void onMessageReceived(
            final RtmMessage message, final RtmChannelMember fromMember) {
        String account = fromMember.getUserId();
        String msg = message.getText();
        System.out.println("Receive message from channel: " + channel_ +
        " member: " + account + " message: " + msg);

        RestTemplate restTemplate = new RestTemplate();
        Comment commnet = new Comment(channel_, account, msg);
        System.out.println(commnet);


        String url = "https://withlive-backend-staging.appspot.com/v1/comment/save?user_id={user_id}&channel={channel}&msg={msg}";

        
        try{
            System.out.println("before_getForObject");
            Comment res1 = restTemplate.getForObject(url, Comment.class, account, channel_, msg);
            System.out.println("after_getForObject");
            // ResponseEntity<Comment> res2 = restTemplate.exchange(url, HttpMethod.POST, null, Comment.class, commnet);
            // Comment re3 = restTemplate.postForObject("https://withlive-backend-staging.appspot.com/v1/comment", commnet, Comment.class);
            // ResponseEntity<Comment> responseEntity = restTemplate.postForEntity(url, commnet, Comment.class);
            System.out.println(res1);
            // System.out.println(res2);
        }catch(RestClientException e){
            System.out.println(e);
        }
        
    }

    @Override
    public void onMemberJoined(RtmChannelMember member) {
        String account = member.getUserId();
        System.out.println("member " + account + " joined the channel "
                          + channel_);
    }

    @Override
    public void onMemberLeft(RtmChannelMember member) {
        String account = member.getUserId();
        System.out.println("member " + account + " lefted the channel "
                         + channel_);
    }
}

 class Comment {
    private String channel;
    private String uid;
    private String msg;
   
    Comment(String channel, String uid, String msg){
        this.channel = channel;
        this.uid = uid;
        this.msg = msg;
    }
}

public class RtmJavaDemo {
    private RtmClient mRtmClient;
    private RtmChannel mRtmChannel;
    private boolean loginStatus = false;
    private Scanner scn;

    public void init() {
        try {
            mRtmClient = RtmClient.createInstance(APPID.APP_ID,
                            new RtmClientListener() {
                @Override
                public void onConnectionStateChanged(int state, int reason) {
                    System.out.println("on connection state changed to "
                        + state + " reason: " + reason);
                }

                @Override
                public void onMessageReceived(RtmMessage rtmMessage, String peerId) {
                    String msg = rtmMessage.getText();
                    System.out.println("Receive message: " + msg 
                                + " from " + peerId);
                }

                @Override
                public void onTokenExpired() {
                }

		@Override
		public void onPeersOnlineStatusChanged(Map<String, Integer> peersStatus) {
		}
            });
        } catch (Exception e) {
            System.out.println("Rtm sdk init fatal error!");
            throw new RuntimeException("Need to check rtm sdk init process");
        }
        scn = new Scanner(System.in);
    }

    public boolean login() {
        System.out.println("Please enter userID (literal \"null\" or starting " +
            "with space is not allowed, no more than 64 charaters!):");
        String userId = "admin-user";
        if (userId.equals("") ||
            userId.startsWith(" ") ||
            userId.equals("null")) {
            System.out.println("Invalid userID detected!");
            return false;
        }
        mRtmClient.login(null, userId, new ResultCallback<Void>() {
            //@Override
            public void onSuccess(Void responseInfo) {
                loginStatus = true;
                System.out.println("login success!");
            }
            //@Override
            public void onFailure(ErrorInfo errorInfo) {
                loginStatus = false;
                System.out.println("login failure!");
            }
        });
        return true;
    }

    public void logout() {
        loginStatus = false;
        mRtmClient.logout(null);
    }

    public void p2pChat(String dst) {
        String msg;
        while(true) {
            System.out.println("please input message you want to send,"+
                " or input \'quit\' " + " to leave p2pChat");
            msg = scn.nextLine();
            if (msg.equals("quit")) {
                return;
            } else {
                sendPeerMessage(dst, msg);
            }
        }
    }

    public void groupChat(String channel) {
        String msg;
        mRtmChannel = mRtmClient.createChannel(channel,
                            new ChannelListener(channel));
        if (mRtmChannel == null) {
            System.out.println("channel created failed!");
            return;
        }
        mRtmChannel.join(new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void responseInfo) {
                System.out.println("join channel success!");
                String channel = "test-live";
                groupChat(channel);
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                System.out.println("join channel failure! errorCode = "
                                    + errorInfo.getErrorCode());
            }
        });
        while(true) {
            System.out.println("please input message you want to send,"+
                " or input \'quit\' " + " to leave groupChat, " + 
                "or input \'members\' to list members");
            msg = scn.nextLine();
            if (msg.equals("quit")) {
                mRtmChannel.leave(null);
                mRtmChannel.release();
                mRtmChannel = null;
                return;
            } else if (msg.equals("members")) {
                getChannelMemberList();
            } else {
                sendChannelMessage(msg);
            }
        }
    }

    public void sendPeerMessage(String dst, String message) {
        RtmMessage msg = mRtmClient.createMessage();
        msg.setText(message);

        mRtmClient.sendMessageToPeer(dst, msg, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                final int errorCode = errorInfo.getErrorCode();
                System.out.println("Send Message to peer failed, errorCode = "
                                + errorCode);
            }
        });
    }

    public void getChannelMemberList() {
        mRtmChannel.getMembers(new ResultCallback<List<RtmChannelMember>>() {
            @Override
            public void onSuccess(final List<RtmChannelMember> responseInfo) {
                for (int i = 0; i < responseInfo.size(); i++) {
                    System.out.println("memberlist[" + i + "]" + ": "
                                        + responseInfo.get(i).getUserId());
                }
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                System.out.println("failed to get channel members, errCode = "
                                    + errorInfo.getErrorCode());
            }
        });
    }

    public void sendChannelMessage(String msg) {
        RtmMessage message = mRtmClient.createMessage();
        message.setText(msg);

        mRtmChannel.sendMessage(message, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                final int errorCode = errorInfo.getErrorCode();
                System.out.println("Send Message to channel failed, erroCode = "
                                + errorCode);
            }
        });
    }

    public static void main(String[] args) {
        RtmJavaDemo client_ = new RtmJavaDemo();
        client_.init();
        while(true) {
            System.out.println("ループします");
            if (!client_.loginStatus) {
                if (!client_.login())
                    continue;
            }
            System.out.println("1: peer to peer chat\n"
                             + "2: group chat\n"
                             + "3: logout");
            System.out.println("please input your choice:");
            Scanner scn = new Scanner(System.in);
            int choice = 2;
            /*
            if (scn.hasNextInt()) {
                choice = scn.nextInt();
            } else {
                System.out.println("your input is not an int type");
                continue;
            }
            */
            if (choice == 1) {
                System.out.println("please input your destination user ID:");
                scn.nextLine();
                String dst = scn.nextLine();
                System.out.println("input destination ID:" + dst);
                client_.p2pChat(dst);
            } else if (choice == 2) {
                System.out.println("please input your channel ID:");
                // scn.nextLine();
                
            } else if (choice == 3) {
                client_.logout();
                System.out.println("quit the demo? yes/no");
                scn.nextLine();
                if (scn.hasNextLine()) {
                    String quit = scn.nextLine();
                    if (quit.equals("yes")) {
                        break;
                    }
                }
            } else {
                continue;
            }
        }
        System.out.println("leaving demo...");
        System.exit(0);
    }
}

