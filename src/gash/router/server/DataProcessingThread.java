package gash.router.server;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.logging.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import gash.router.server.dbhandlers.DbHandler;
import gash.router.server.dbhandlers.MongoHandler;
import gash.router.server.dbhandlers.RedisHandler;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.tasks.TaskList;
import io.netty.channel.Channel;
import pipe.common.Common.Failure;
import pipe.work.Work.Task;
import pipe.work.Work.WorkMessage;
import routing.Pipe;
import routing.Pipe.CommandMessage;
/**
 * @author Sarika Nitin Kale
 *
 * 
 */
public class DataProcessingThread implements Runnable {
	protected static Logger logger = LoggerFactory.getLogger("Data processing");
	private TaskList taskList;
	private boolean forever = true;
	private static int i = 0;
	private static int counter = 0;
	private static int msgId = 0;
	private ServerState state;
	private static boolean rollbackChunks = false;

	public DataProcessingThread(TaskList taskList, ServerState state) {
		this.taskList = taskList;
		this.state = state;
	}

	@Override
	public void run() {
		logger.info("DATA PROCESSING");
		while (true) {
			if (!forever && taskList.numEnqueued() == 0) {
				break;
			}
			if (taskList != null && taskList.numEnqueued() != 0) {

				pullTasksFromQueue(taskList);
			}
		}
	}

	public void pullTasksFromQueue(TaskList taskList) {
		
		Task task = taskList.dequeue();
		CommandMessage msg = task.getMessage();
		if ((msg.getAction().equals(Pipe.Action.READ))) {
			processData(msg);
		} else {
			if ((msg.getAction().equals(Pipe.Action.UPLOAD)) && msg.getChunk().getNumberOfChunks() == 1) {
				processData(msg);
			} else if ((!ChunkTracker.checkMissing(msg.getMessageId())) && (msg.getAction().equals(Pipe.Action.UPLOAD))
					&& msg.getChunk().getNumberOfChunks() != 1) {
				processData(msg);
			} else {
					rollback(msg);
			}
		}
	}


	public void rollback(CommandMessage msg) {
		// get all message chunks from the db of same msg id and delete them
		DbHandler handler = null;
		MongoHandler m = null;
		try {
			handler = new RedisHandler();
			m = new MongoHandler();
			logger.error("All chunks not received....ROLLBACK");
			handler.removeData(msg.getFilename());
			m.removeData(msg.getFilename());
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			handler.closeConnection();
			m.closeConnection();
		}
	}

	public synchronized void processData(CommandMessage msg) {
		try {
			if (msg != null) {
				MongoHandler m = new MongoHandler();
				DbHandler handler = new RedisHandler();
				if (msg.hasUploadData() && msg.getAction().equals(Pipe.Action.UPLOAD)) {
					uploadData(msg, m, handler);
				} else {
					if (msg.getAction().equals(Pipe.Action.READ)) {
						readData(msg, m, handler);
					} else {
						System.out.println("No Message To serve");
						return;
					}
				}
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendToLeader(Map<String, String> dataMap, CommandMessage msg) {
		//Sending Read message from slave to the leader
		for (Map.Entry<String, String> entry : dataMap.entrySet()) {
			System.out.println("Key " + dataMap.get(entry.getKey()));
			System.out.println("Value " + dataMap.get(entry.getValue()));
			Channel ch = null;
			for (EdgeInfo ei : state.getEmon().getOutBoundEdges().getEdgesMap().values()) {
				if (ei.getRef() == state.getClusterInfo().getLeaderID()) {
					if (ei.getChannel() == null) {
						ch = state.getEmon().newChannel(ei.getHost(), ei.getPort(), false);
					} else
						ch = ei.getChannel();
				}
			}
			if(dataMap.isEmpty()){
				entry.setValue("No Data Found");
			}
			WorkMessage wm = state.getEmon().createTaskMessage(msg, entry, dataMap.size());
			ch.writeAndFlush(wm);
		}

	}

	public void uploadData(CommandMessage msg, MongoHandler m, DbHandler handler) {
		String key = msg.getFilename();
		boolean successRedis = false;
		boolean successMongo = false;
		try {
			// Redis insertion
			successRedis = handler.insertData(key, msg.getChunk().getChunkId(), msg.getUploadData().toString());
			if (successRedis == true) {
				// Mongo Insertion
				successMongo = m.insertData(key, msg.getChunk().getChunkId(), msg.getUploadData().toString());
				if (successMongo == true) {
					// if the node is a leader, it replicates it to other nodes
					if (state.isLeader) {
						MessageServer.StartWorkCommunication.getInstance().dataReplicate(msg);
					}
				}
				//sending Reply back to the client
				CommandMessage.Builder rb = CommandMessage.newBuilder(msg);
				rb.setAction(Pipe.Action.UPLOAD);
				rb.setUploadData(ByteString.copyFromUtf8("Data Uploaded Successfully"));
				state.getChannelMap().get(msg.getHeader().getNodeId()).writeAndFlush(rb.build());
			}else{
				Failure.Builder eb = Failure.newBuilder();
				eb.setId(state.getEmon().getDestId());
				eb.setRefId(msg.getHeader().getNodeId());
				eb.setMessage("Unable to upload data");
				CommandMessage.Builder rb = CommandMessage.newBuilder(msg);
				rb.setErr(eb);
				state.getChannelMap().get(msg.getHeader().getNodeId()).writeAndFlush(rb.build());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			handler.closeConnection();
			m.closeConnection();
		}
	}

	public void readData(CommandMessage msg, MongoHandler m, DbHandler handler) {
		String key = msg.getFilename();
		Map<String, String> dataMap;
		try {
			// Jedis Read
			dataMap = handler.retrieveData(key);
			// if data is not present in Redis, find it in Mongo
			if (dataMap.isEmpty()) {
				// Mongo Read
				Map<String, String> dataMongo = m.retrieveData(key);
				sendToLeader(dataMongo, msg);
			} else {
				sendToLeader(dataMap, msg);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			handler.closeConnection();
			m.closeConnection();
		}

	}
}
