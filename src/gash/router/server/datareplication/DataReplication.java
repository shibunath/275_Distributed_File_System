package gash.router.server.datareplication;

import gash.router.server.ServerState;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;
/**
 * @author Sarika Nitin Kale
 *
 * 
 */
public class DataReplication implements Runnable{
	ServerState state;
	CommandMessage cm;
	
	public DataReplication(ServerState state, CommandMessage cm){
		this.state=state;
		this.cm=cm;
	}
	
	public synchronized void replicateDataToEveryNode() {
		sendDataToEveryNodes(state.getEmon().createTaskMessage(cm));
	}

	public synchronized void sendDataToEveryNodes(WorkMessage wm){
		state.getEmon().sendTaskMessages(wm);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		replicateDataToEveryNode();
		
	}
}
