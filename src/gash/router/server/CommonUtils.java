package gash.router.server;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.container.RoutingConf;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;
import gash.router.server.edges.EdgeMonitor;
import pipe.common.Common.Header;
import pipe.election.Election.LeaderStatus;
import pipe.election.Election.LeaderStatus.Action;
import pipe.election.Election.LeaderStatus.LeaderQuery;
import pipe.election.Election.LeaderStatus.LeaderState;
import pipe.election.Election.LeaderStatus.NodeState;
import pipe.monitor.Monitor.ClusterMonitor;
import pipe.work.Work.WorkMessage;

public class CommonUtils {
	protected static Logger logger = LoggerFactory.getLogger("utils");
	final static int RETRY_HB = 3;//retry attempts if heartbeats failed to be recieved
	public  static boolean FOUNDNODES = false;
	public final static int SECRET_KEY  = 11111;
	public final static String SUBNET = "192.168.1.";
	public final static int PORT = 5000;
	public static final String DNS_HOST = "192.168.1.51";// ip of dns server
	public final static int DNS_PORT = 4000; //port of the dns server
	public final static int COMMAND_PORT = 3000; //listening on work port
	
	public static RoutingConf conf ;
	
	/**
	 util to get the hostInfo by ID.
	 * @param state
	 * @param nodeId
	 * @return hostIP associated with id
	 */
	public static String getHostInfoById(ServerState state, int nodeId){
		Map<Integer, EdgeInfo> map = state.getEmon().getOutBoundEdges().getEdgesMap();
		System.out.println("node is is"+nodeId);
		for(EdgeInfo ei : map.values()){
			if(ei.getRef() == nodeId){
				return ei.getHost();
			}
		}
		logger.error("getHostInfoById: failed to return the host from id");
		return null;
	}
	
	
	public static WorkMessage createLeaderElectionMessage(ServerState state, int nodeId, LeaderQuery query, 
			Action action, NodeState nState, LeaderState ls, int destId) {
		logger.info("Utils: sending leader election to..1 : "+destId);
		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(nodeId);
		hb.setDestination(-1);
		hb.setTime(System.currentTimeMillis());
		
		WorkMessage.Builder wb = WorkMessage.newBuilder();
		wb.setHeader(hb);
		wb.setSecret(CommonUtils.SECRET_KEY);
		LeaderStatus.Builder lb = LeaderStatus.newBuilder();
		lb.setState(ls);
		lb.setNodeState(nState);

		lb.setQuery(query);
		lb.setAction(action);
		lb.setMsgId(nodeId);
		lb.setOriginNodeId(nodeId);
		lb.setTerm(state.getTerm());
		lb.setPrevTerm(state.getPrevTerm());
		lb.setDestId(destId);
		wb.setLeader(lb);
		return wb.build();
	
	}
	
	/**
	 * 
	 * @param host
	 * @return true if the host is reachable, else false.
	 */
	public static boolean isReachableByPing(String host) {
	    try{
	            String cmd = "";
	            if(System.getProperty("os.name").startsWith("Windows")) {   
	                    // For Windows
	                    cmd = "ping -n 1 " + host;
	            } else {
	                    // For Linux and OSX
	                    cmd = "ping -c 1 " + host;
	            }
	            Process myProcess = Runtime.getRuntime().exec(cmd);
	            myProcess.waitFor();
	            if(myProcess.exitValue() == 0) {
	                    return true;
	            } else {
	                    return false;
	            }
	    } catch( Exception e ) {
	            e.printStackTrace();
	            return false;
	    }
	} 
	
	public static ArrayList<String> pingAll(){
		ArrayList<String> hostList  = new ArrayList<String>();
			   //30-34 chosen as our team addrress range( as agreed across teams).Hard coding it. 
			   for (int i=30;i<36;i++){
		       String host = SUBNET + String.valueOf(i);
		       if(isReachableByPing(host)){	   
		    	  hostList.add(host);
		    	  logger.info(host+"is reachable");}
		       else
		    	  logger.info(host+String.valueOf(i)+"is not reachable");
		}
		return hostList;
	}
	
	public static final  long UUIDGen(){
		    //generate random UUIDs
		UUID id = UUID.randomUUID();  
		System.out.println(id.getMostSignificantBits());
		id.toString().replace('-','1');
		return id.getMostSignificantBits();
		
	}
		  
	 public static Timestamp getTimeStamp(){
		 java.util.Date date= new java.util.Date();
		 return new Timestamp(date.getTime());
	}
	 public static ClusterMonitor sendDummyMessage(ServerState state, EdgeMonitor emon,int tick) {
			//Build the message to be sent to monitor server
		ClusterMonitor.Builder cm = ClusterMonitor.newBuilder();
		cm.setClusterId(3);
		emon.getOutBoundEdges();
		cm.setNumNodes(EdgeList.getTotalNodes());
		cm.addEnqueued(state.getTasks().numEnqueued());
		cm.addProcessId(state.getConf().getNodeId());
		cm.addProcessed( state.getTasks().numProcessed());	
		cm.addStolen(state.getTasks().numBalanced());
		cm.setTick(tick++);
		return cm.build();		
	}

	
	//}
	
//	 public static RoutingConf modifyConf(RoutingConf conf){
//		 ArrayList<RoutingEntry> route = new ArrayList<RoutingEntry>();
//		 RoutingEntry e1 = new RoutingEntry(); 
//		 for (int i=51;i<60;i++){
//	 
//	       String host="192.168.1" + ".";
//	       if(isReachableByPing(host+String.valueOf(i)))
//	    	   System.out.println(host+String.valueOf(i)+"is reachable");
//	       e1.setHost();
//			e1.setId(i);
//			e1.setPort(var_port);
//			route.add(e1);
//	       else
//		       System.out.println(host+String.valueOf(i)+"is not reachable");
//	       
//
//	}
//	 }
//	

}
