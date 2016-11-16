package gash.router.dns.server;

import java.util.HashMap;

import global.Global.GlobalCommandMessage;
import io.netty.channel.Channel;

public class IPList {
	private static String hostIP = "192.168.1.52" ;
	private final static int hostPort = 5000;
	private final static int clientfacingPort = 4000; 
	private static Channel leaderChannel; 
	private static OutgoingMsgHandle handle;
	
	private static HashMap<Integer, MessageObject> channelMap; 

	public static HashMap<Integer, MessageObject> getChannelMap() {
			return channelMap;
		}
	public static void setInitialchannelMap(){
		channelMap =new HashMap<Integer, MessageObject>();	
	}
	public static void putChannelMap(int id, MessageObject mo ){
		channelMap.put(id, mo);
	}
	

//		public static void setChannelMap(HashMap<Integer, Channel> channelMap) {
//			ServerState.channelMap = channelMap;
//		}
	public static String getHostIP() {
		return hostIP;
	}

	public static void setHostIP(String hostIP) {
		IPList.hostIP = hostIP;
	}

	public static int getHostport() {
		return hostPort;
	}

	public static int getClientfacingport() {
		return clientfacingPort;
	}
	
	public static Channel getLeaderChannel() {
		return leaderChannel;

	}
	public static void setLeaderChannel(Channel channel){
		leaderChannel = channel;
	}
	
	public static void setHandle( OutgoingMsgHandle omh){
		handle = omh;
	}
	public static  OutgoingMsgHandle getHandle(){
		return handle;
	}
	
	
}
 class MessageObject{
	Channel channel;
	String messageId;
	GlobalCommandMessage message;
	
}
