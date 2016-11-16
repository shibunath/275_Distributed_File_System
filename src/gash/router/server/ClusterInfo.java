package gash.router.server;

import java.util.HashSet;
import java.util.Set;

import pipe.election.Election.LeaderStatus.LeaderState;

public class ClusterInfo {

	int numberOfNodes;
	int nodesAliveCount;
	int leaderID = -1; //negative number implies no leader set
	Set<Integer> aliveNodeInfo = new HashSet<Integer>();
	LeaderState ls;
	
	public void setNumberOfNodes(int numberOfNodes){
		this.numberOfNodes = numberOfNodes;
	}
	public int getNumberOfNodes(){
		return numberOfNodes;
	}
	public void setNodesAliveCount(int nodesAliveCount){
		this.nodesAliveCount = nodesAliveCount;
	}
	public int getNodesAliveCount(){
		return nodesAliveCount;
	}
	
	public void setAliveNodeInfo(int nodeId){
		this.aliveNodeInfo.add(nodeId);
	}
	public Set<Integer> getAliveNodeInfo(){
		return aliveNodeInfo;
	}
	
	public void setLeaderID(int leaderID){
		this.leaderID = leaderID;
	}
	public int getLeaderID(){
		return leaderID;
	}
	
	public void setLeaderState(LeaderState ls){
		this.ls = ls;
	}
	public LeaderState getLeaderState(){
		return ls;
	}
	
	
}
