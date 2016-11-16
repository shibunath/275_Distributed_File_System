package gash.router.client;



public class IPList {
	private static String hostIP; // ip of the leader
	private  static int hostPort ; //Port of the leader server
	//private final static int clientfacingPort = 4000; 
	private static boolean isRead;
	public static boolean isRead() {
		return isRead;
	}

	public static void setRead(boolean isRead) {
		IPList.isRead = isRead;
	}

	public static String getHostIP() {
		return hostIP;
	}

	public static void setHostIP(String hostIP) {
		System.out.println("SETTTTTING  IN DNS ENTRY *******************"+ hostIP+" HOST IP $$$$");
		IPList.hostIP = hostIP;
	}

	public static int getHostport() {
		return hostPort;
	}


	public static void setHostPort(int hostPort) {
		IPList.hostPort = hostPort;
	}

	

	
}
