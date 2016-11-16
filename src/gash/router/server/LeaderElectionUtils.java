package gash.router.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.client.IPList;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;
import gash.router.server.edges.EdgeMonitor;
import pipe.common.Common.Header;
import pipe.election.Election.LeaderStatus;
import pipe.election.Election.LeaderStatus.Action;
import pipe.election.Election.LeaderStatus.LeaderQuery;
import pipe.election.Election.LeaderStatus.LeaderState;
import pipe.election.Election.LeaderStatus.NodeState;
import pipe.work.Work.WorkMessage;

public class LeaderElectionUtils {
	Map<Integer, EdgeInfo> map;
	final public static int minMumberOfNodes = 3;
	ClusterInfo clusterInfo;
	private static int i = 0;
	public LeaderElectionUtils(ServerState state){
		this.clusterInfo = state.getClusterInfo();
	}
	protected static Logger logger = LoggerFactory.getLogger("leader election");

	public void onLeaderElectionMsgRecieved(WorkMessage msg, ServerState state) {
		EdgeMonitor emon = state.getEmon();
		logger.info(CommonUtils.getTimeStamp()+"LEADER MESSAGE RECIEVED from --->" + msg.getHeader().getNodeId());
		LeaderStatus ls = msg.getLeader();
		emon.setDestId(ls.getDestId());
		emon.setLeaderState(ls.getState());

		//leader has been made and i wasn't leader.
		if(ifLeaderAlreadyAlive(state, ls)){
			updateLeaderInfo(msg, state);
			return;
		}
		else if(ls.getState().equals(LeaderState.LEADERALIVE) && ls.getQuery().equals(LeaderQuery.THELEADERIS) && 
				state.getClusterInfo().getLeaderID() > 0){
			leaderSelected(msg, state, emon);
			return;
		}
		//when a node gets the vote.
		else if(state.getConf().getNodeId()== ls.getDestId() && ls.getAction().equals(LeaderStatus.Action.SENDVOTE)){   
			voteRecieved(msg, state, emon, ls);
		}
		else if(ls.getAction().equals(LeaderStatus.Action.REQUESTVOTE) && clusterInfo.getLeaderID()<0){
			voteRequested(state, emon, ls);
		}
		else if(ls.getAction().equals(LeaderStatus.Action.REQUESTVOTE) && clusterInfo.getLeaderID()>0){
			System.out.println("Dude,already set leader..dnt request.");
		}
	}

	private boolean ifLeaderAlreadyAlive(ServerState state, LeaderStatus ls) {
		return ls.getState().equals(LeaderState.LEADERALIVE) && ls.getQuery().equals(LeaderQuery.THELEADERIS) && 
				state.getClusterInfo().getLeaderID() < 0;
	}
	
	private void updateLeaderInfo(WorkMessage msg, ServerState state) {
		int leaderId = msg.getLeader().getLeaderId();
		System.out.println("Leader is alive"+leaderId);
		clusterInfo.setLeaderID(leaderId);
		//DNSServer.UpdateLeaderDNSEntry(CommonUtils.getHostInfoById(state, leaderId),state);
		createLeaderFileInfo(CommonUtils.SUBNET+String.valueOf(leaderId));
		if(leaderId == state.getConf().getNodeId()){
			state.setLeader(true);
		}
		return;
	}
	

	private void voteRequested(ServerState state, EdgeMonitor emon, LeaderStatus ls) {
		emon.setLeaderState(LeaderState.LEADERELECTING);
		System.out.println("ls.getDestId() was"+ls.getDestId());
		
		System.out.println(CommonUtils.getTimeStamp()+"OOPS.SOMEONE ELSE REQUESTED MY VOTE, i will become follower then");
		state.nodeState = NodeState.FOLLOWER_STATE;
		WorkMessage wm = CommonUtils.createLeaderElectionMessage(state, state.getConf().getNodeId(),LeaderQuery.WHOISTHELEADER,
		Action.SENDVOTE,NodeState.FOLLOWER_STATE, LeaderState.LEADERELECTING, ls.getOriginNodeId());
		System.out.println("voting now..");

		for (EdgeInfo ei : emon.getOutBoundEdges().getEdgesMap().values()) {
			//send only to destination channel
			logger.info("LET: writing to channel of "+ei.getRef());
			if (ei.getRef() == ls.getOriginNodeId()) {
				if(ei.getChannel()!=null){
					System.out.println("LET: writing to channel of "+ei.getRef()+" channel is"+ei.getChannel());
					ei.getChannel().writeAndFlush(wm);
				}
			}
		}
	}

	private void voteRecieved(WorkMessage msg, ServerState state, EdgeMonitor emon, LeaderStatus ls) {
		logger.info(" :"+ls.getTerm());
		//increment the vote for the particular term.
		int vote = state.getTermVotesMap(ls.getTerm());
		vote++;
		logger.info(CommonUtils.getTimeStamp()+"Recieved vote!Vote Count**"+vote);
		state.setTermVotesMap(ls.getTerm(), vote);
		emon.setLeaderState(LeaderState.LEADERELECTING);
		//if voted by more than half of the nodes, announce the leader.
		if(vote >= EdgeList.getTotalNodes()/2){
			leaderSelected(msg, state, emon);
		}
	}

	public static void createLeaderFileInfo(String ip){
		try {
			String FilePath=System.getProperty("user.dir").replace("\\", "/");
			File file = new File(FilePath+"/leaderinfo.txt");

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(ip);
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void leaderSelected(WorkMessage msg, ServerState state, EdgeMonitor emon) {
	//	MonitorClient mc = new MonitorClient("192.168.1.2", 5000);
	//	ClusterMonitor msgCluster = CommonUtils.sendDummyMessage(state,state.getEmon(),i);
	//	mc.write(msgCluster);

		logger.info("LEADER ELECTED!!"+state.getConf().getNodeId());
		publishLeaderInfoToSlaves(state, emon);
		
	}

	private void publishLeaderInfoToSlaves(ServerState state, EdgeMonitor emon) {
		emon.setLeaderState(LeaderState.LEADERALIVE);
		state.setLeader(true);
		state.setPrevTerm(state.getTerm());
		state.setTerm(state.getTerm()+1);
		int nodeId = state.getConf().getNodeId();
		clusterInfo.setLeaderID(nodeId);

		IPList.setHostIP(CommonUtils.SUBNET+String.valueOf(nodeId));
		createLeaderFileInfo(CommonUtils.SUBNET+String.valueOf(nodeId));

		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(state.getConf().getNodeId());
		hb.setDestination(-1);
		hb.setTime(System.currentTimeMillis());
		
		WorkMessage.Builder wb = WorkMessage.newBuilder();
		LeaderStatus.Builder lb = createLeaderMessage(state, nodeId, hb, wb);
		
		for (EdgeInfo ei : emon.getOutBoundEdges().getEdgesMap().values()) {
			//announce to all OTHER NODES
			if (ei.getRef()!= nodeId) {
				logger.info("Announcing about Elected Leader %%%%%%%%%%%"+ei.getRef()+" channel is"+ei.getChannel());
				wb.setLeader(lb);
				if(ei.getChannel() != null)
				ei.getChannel().writeAndFlush(wb.build());
				
				}	
		}
	}

	public LeaderStatus.Builder createLeaderMessage(ServerState state, int nodeId, Header.Builder hb,
		WorkMessage.Builder wb) {
		wb.setHeader(hb);
		wb.setSecret(CommonUtils.SECRET_KEY);
		LeaderStatus.Builder lb = LeaderStatus.newBuilder();
		lb.setState(LeaderState.LEADERALIVE);

		lb.setQuery(LeaderQuery.THELEADERIS);
		lb.setOriginNodeId(nodeId);
		lb.setLeaderId(nodeId);
		lb.setMsgId(nodeId);
		lb.setTerm(state.getTerm());
		lb.setPrevTerm(state.getPrevTerm());
		return lb;
	}
	
	/**
	 * Waits for the timeout period of 2secs,if no LeaderMessage is recieved, promotes as candidate.
	 * @param ei
	 * @param state
	 * @param destId
	 */
	public void triggerLeaderElec(EdgeInfo ei, ServerState state, int destId) {
		WorkMessage wm;
		Long startTime;
		//if leader already present, exit.
		if(state.getClusterInfo().getLeaderID() >0)
			return;
		startTime = System.currentTimeMillis();
		while(true){
			if(state.getEmon().getLeaderState().equals(LeaderState.LEADERALIVE)){
				logger.info("Leader is Alive");
				break;
			}
			
			long timeOut = state.getConf().getPriority() == 0 ? EdgeMonitor.DT * 1 : EdgeMonitor.DT * state.getConf().getPriority();
			
			if(System.currentTimeMillis() - startTime >= timeOut){				state.nodeState = NodeState.CANDIDATE_STATE;
				logger.info(CommonUtils.getTimeStamp()+"New Candidate"+ei.getRef());
				break;
			}
			
		}
		//since candidate, request the votes.
		if(state.nodeState.equals(NodeState.CANDIDATE_STATE)){
			System.out.println(CommonUtils.getTimeStamp()+"PLease vote for me...:"+state.getConf().getNodeId());
			wm = CommonUtils.createLeaderElectionMessage(state, state.getConf().getNodeId(),LeaderQuery.WHOISTHELEADER,
				Action.REQUESTVOTE,NodeState.CANDIDATE_STATE, LeaderState.LEADERELECTING, state.getConf().getNodeId());
			if(ei.getChannel() != null)
				ei.getChannel().writeAndFlush(wm);
		}
	}

	public void onLeaderDead(EdgeMonitor edgeMonitor) {
		for (EdgeInfo ei : edgeMonitor.outboundEdges.getEdgesMap().values()) {
			if (EdgeList.getTotalNodes() >= LeaderElectionUtils.minMumberOfNodes) {
				triggerLeaderElec(ei, edgeMonitor.state, edgeMonitor.destId);
			}
		}
	}	

}
