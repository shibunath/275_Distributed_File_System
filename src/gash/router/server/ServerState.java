package gash.router.server;

import java.util.HashMap;
import java.util.Map;

import gash.router.container.RoutingConf;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.tasks.TaskList;
import io.netty.channel.Channel;
import pipe.election.Election.LeaderStatus;

public class ServerState {
	private RoutingConf conf;
	private EdgeMonitor emon;
	public LeaderStatus.NodeState nodeState;
	public boolean isLeader;
	private Map<Integer, Integer> termVotesMap = new HashMap<Integer, Integer>();
	ClusterInfo clusterInfo;
	private TaskList tasks;
	private int term;
	private int prevTerm;
	private static HashMap<Integer, Channel> channelMap=new HashMap<Integer, Channel>();
	private Channel readCh;
	private boolean checkStealNode;
	private int StealNodeId;
	
	public boolean isCheckStealNode() {
		return checkStealNode;
	}

	public void setCheckStealNode(boolean checkStealNode) {
		this.checkStealNode = checkStealNode;
	}

	public int getStealNodeId() {
		return StealNodeId;
	}

	public void setStealNodeId(int stealNodeId) {
		StealNodeId = stealNodeId;
	}
	
	
	public RoutingConf getConf() {
		return conf;
	}
	
	public Channel getReadCh() {
		return readCh;
	}

	public void setReadCh(Channel ch) {
		this.readCh = readCh;
	}

	public synchronized void setTermVotesMap(int term, int votes){
		this.termVotesMap.put(term, votes);
	}
	
	public synchronized int getTermVotesMap(int term){
		if(this.termVotesMap.containsKey(term))
		return this.termVotesMap.get(term);
		else
			return 1;
	}
	
	public void setConf(RoutingConf conf) {
		this.conf = conf;
	}

	public EdgeMonitor getEmon() {
		return emon;
	}

	public void setEmon(EdgeMonitor emon) {
		this.emon = emon;
	}

	public TaskList getTasks() {
		return tasks;
	}

	public void setTasks(TaskList tasks) {
		this.tasks = tasks;
	}
	
	public void setClusterInfo(ClusterInfo clusterInfo){
		this.clusterInfo = clusterInfo;
	}
	public ClusterInfo getClusterInfo(){
		return clusterInfo;
	}

	public boolean isLeader() {
		return isLeader;
	}


	public void setLeader(boolean isLeader) {
		this.isLeader = isLeader;
	}

	public int getTerm() {
		return term;
	}

	public void setTerm(int term) {
		this.term = term;
	}

	public int getPrevTerm() {
		return prevTerm;
	}

	public void setPrevTerm(int prevTerm) {
		this.prevTerm = prevTerm;
	}

	public static HashMap<Integer, Channel> getChannelMap() {
		return channelMap;
	}

	public static void setChannelMap(HashMap<Integer, Channel> channelMap) {
		ServerState.channelMap = channelMap;
	}

}
