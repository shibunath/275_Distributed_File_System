package gash.router.server.tasks;

import gash.router.server.ServerState;
public class WrkBalancer extends Thread {
	ServerState state;
	
	public WrkBalancer(ServerState s){ 
		this.state=s;
	}
	
	public void run() {
		do {
			try {
				System.out.println("********########Setting Steal NodeId***********");
				setStealNodeId(state);
				sleep(300000);
			} catch (Exception ignored) {
			}
		} while (true);
	}
	
	public boolean StealWrkCheck(){
		if((!state.isLeader) && (isQueueLessThanThreshold()) ){ 
			return true;
		} else {
			return false;
		}
	}
	public boolean isQueueLessThanThreshold() {
		if (state.getTasks().numEnqueued() == 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public void setStealNodeId(ServerState state){
		if(StealWrkCheck() && (CalCpuLoad.checkCpuLoadOptions()<0.3)){
			this.state.setCheckStealNode(true);  
			this.state.setStealNodeId(state.getConf().getNodeId()); 
		} else {
			this.state.setCheckStealNode(false);
		}
	}
}