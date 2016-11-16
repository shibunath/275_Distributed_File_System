/**
 * Copyright 2016 Gash.
 *
 * This file and intellectual content is protected under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package gash.router.server.edges;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import gash.router.client.CommInit;
import gash.router.server.CommonUtils;
import gash.router.container.RoutingConf.RoutingEntry;
import gash.router.server.ClusterInfo;
import gash.router.server.LeaderElectionUtils;
import gash.router.server.ServerState;
import gash.router.server.WorkHandler;
import gash.router.server.WorkInit;
import gash.router.server.tasks.CalCpuLoad;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import pipe.common.Common.Header;
import pipe.election.Election.LeaderStatus;
import pipe.election.Election.LeaderStatus.LeaderState;
import pipe.work.Work.Heartbeat;
import pipe.work.Work.Task;
import pipe.work.Work.TaskAction;
import pipe.work.Work.WorkMessage;
import pipe.work.Work.WorkState;
import routing.Pipe;
import routing.Pipe.Chunk;
import routing.Pipe.CommandMessage;
import routing.Pipe.ReadResponse;

public class EdgeMonitor implements EdgeListener, Runnable {
	protected static Logger logger = LoggerFactory.getLogger("edge monitor");

	public EdgeList outboundEdges;
	private EdgeList inboundEdges;
	public static long DT = 2000;
	public ServerState state;
	private boolean forever = true;
	public LeaderState ls = LeaderState.LEADERUNKNOWN;
	public int destId;
	private LeaderElectionUtils leaderElec;
	private int enqueuedTask;
	private int processedTask;
	private static int taskSeqId=0;
	InetAddress[] localaddr;

	public EdgeList getOutBoundEdges() {
		return outboundEdges;
	}

	public synchronized void setLeaderState(LeaderState lstate) {
		this.ls = lstate;
	}

	public synchronized LeaderState getLeaderState() {
		return ls;
	}

	public LeaderElectionUtils geLeaderElectionThread() {
		return leaderElec;
	}

	public synchronized void setDestId(int s_destId) {
		this.destId = s_destId;
	}

	public synchronized int getDestId() {
		return destId;
	}

	

	public EdgeMonitor(ServerState state) {

		if (state == null)
			throw new RuntimeException("state is null");
		this.outboundEdges = new EdgeList();
		this.inboundEdges = new EdgeList();
		this.state = state;
		this.state.setEmon(this);
		
		leaderElec = new LeaderElectionUtils(state);

		if (state.getConf().getRouting() != null) {
			for (RoutingEntry e : state.getConf().getRouting()) {
				outboundEdges.addNode(e.getId(), e.getHost(), e.getPort());
			}
		}

		// cannot go below 2 sec
		if (state.getConf().getHeartbeatDt() > EdgeMonitor.DT)
			EdgeMonitor.DT = state.getConf().getHeartbeatDt();

	}

	public void createInboundIfNew(int ref, String host, int port) {
		inboundEdges.createIfNew(ref, host, port);
	}

	public void createOutboundIfNew(int ref, Channel channel) {
		// state.getConf().updateRouting(Utils.SUBNET+ref, Utils.PORT, ref);
		System.out.println("In outbound " + channel);
		outboundEdges.createIfNew(ref, CommonUtils.SUBNET + ref, CommonUtils.PORT);
		outboundEdges.getNode(ref).setChannel(channel);
	}

	private WorkMessage createHB(EdgeInfo ei) {
		WorkState.Builder sb = WorkState.newBuilder();
		sb.setEnqueued(getEnqueuedTasks());
		sb.setCpuLoad(CalCpuLoad.checkCpuLoadOptions());
		sb.setSteal(state.isCheckStealNode());

		Heartbeat.Builder bb = Heartbeat.newBuilder();
		bb.setState(sb);
		bb.setCapacity(1024);
		bb.setTotalNodes(EdgeList.getTotalNodes());

		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(state.getConf().getNodeId());
		hb.setDestination(-1);
		hb.setTime(System.currentTimeMillis());

		WorkMessage.Builder wb = WorkMessage.newBuilder(); 
		wb.setHeader(hb);
		wb.setBeat(bb);
		wb.setSecret(CommonUtils.SECRET_KEY);
		return wb.build();
	}

	

	public void shutdown() {
		forever = false;
	}

	@Override
	public void run() {
		ClusterInfo clusterInfo = state.getClusterInfo();
		TimerTask timerTask = new MonitorHBTask();
		// running timer task as daemon thread
		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(timerTask, 0, 6 * 1000);
		while (forever) {
			logger.info("WITHIN EDGE MONITOR +++++" + this.outboundEdges.getEdgesMap().size());
			try {
				for (EdgeInfo ei : this.outboundEdges.getEdgesMap().values()) {

					if (ei.getChannel() == null) {
						ei.setChannel(newChannel(ei.getHost(), ei.getPort(), true));
						ei.setActive(true);
						this.outboundEdges.getEdgesMap().put(ei.getRef(), ei);
						state.setEmon(this);
						sendHBToLeader(clusterInfo, ei);
					} else if (ei.isActive() && ei.getChannel() != null) {
						sendHBToLeader(clusterInfo, ei);
						if (EdgeList.getTotalNodes() >= LeaderElectionUtils.minMumberOfNodes
								&& getLeaderState().equals(LeaderStatus.LeaderState.LEADERUNKNOWN)
								&& state.getClusterInfo().getLeaderID() < 0) {
							leaderElec.triggerLeaderElec(ei, state, destId);
						} else if (EdgeList.getTotalNodes() >= LeaderElectionUtils.minMumberOfNodes
								&& getLeaderState().equals(LeaderStatus.LeaderState.LEADERDEAD)) {
						}
					}
				}
				Thread.sleep(DT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void monitorHB() {
		// if slave node, monitor the leader
				if (!state.isLeader) {
					if (state.getClusterInfo().getLeaderID() > -1
							&& WorkHandler.ctr[state.getClusterInfo().getLeaderID()] > 0) {
						logger.info(WorkHandler.ctr[state.getClusterInfo().getLeaderID()]
								+ "Recieved heartbeat from leader >>>"
								+ state.getClusterInfo().getLeaderID());
						WorkHandler.ctr[state.getClusterInfo().getLeaderID()]--;
					} else if (EdgeList.getTotalNodes() > LeaderElectionUtils.minMumberOfNodes
							&& !state.getEmon().getLeaderState().equals(LeaderState.LEADERELECTING)) {
						state.getClusterInfo().setLeaderID(-1);
						state.getEmon().setLeaderState(LeaderState.LEADERDEAD);
						state.setPrevTerm(state.getTerm());
						state.setTerm(state.getTerm() + 1);
						logger.error("PANIC!!NO HEARTBEAT FROM LEADER");
						
						leaderElec.onLeaderDead(this);
					}
				}
				// if master node, monitor all the nodes and update ClusterInfo
				else {
					int nodesAliveCount = 0;
					for (EdgeInfo ei : this.outboundEdges.getEdgesMap().values()) {
						int id = ei.getRef();
						if (id != state.getConf().getNodeId()) {
							if (id > -1 && WorkHandler.ctr[id] > 0) {
								logger.info(WorkHandler.ctr[id]
										+ "Recieved heartbeat from slaves >>>" + id);
								WorkHandler.ctr[id]--;
								nodesAliveCount++;
								state.getClusterInfo().setAliveNodeInfo(id);
							} else {
								logger.error("Time Out of Slave>>>>>");
								state.getClusterInfo().getAliveNodeInfo().remove(id);
							}
						}
					}
					state.getClusterInfo().setNodesAliveCount(nodesAliveCount);
				}

	}

	private void sendHBToLeader(ClusterInfo clusterInfo, EdgeInfo ei) {
		System.out.println("sending HB..........................................................." + state.isLeader());
		// if (state.isLeader()) {
		WorkMessage wm = createHB(ei);
		Channel chan = ei.getChannel();
		if (ei.getChannel() != null) {
			System.out.println("to child nodes..");
			chan.writeAndFlush(wm);
		} else
			System.out.println("to child nodes..");
		// } else
		if (ei.getRef() == clusterInfo.getLeaderID()) {
			System.out.println("will try to send HB to leader" + clusterInfo.getLeaderID());
			WorkMessage wm1 = createHB(ei);
			ei.getChannel().writeAndFlush(wm1);
		}
	
	}

	public Channel newChannel(String host, int port, boolean addingNode) {
		ChannelFuture channel;
		Channel chan = null;
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			System.out.println("Creating new channel for " + host);
			WorkInit si = new WorkInit(state, false);
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class).handler(si);
			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
			b.option(ChannelOption.TCP_NODELAY, true);
			b.option(ChannelOption.SO_KEEPALIVE, true);

			// Make the connection attempt.
			channel = b.connect(host, port).syncUninterruptibly();
			chan = channel.channel();
			System.out.println("LOGGERRRRR" + chan);

			if (chan.isOpen() && addingNode) {
				outboundEdges.setTotalNodes(EdgeList.getTotalNodes() + 1);
				state.getClusterInfo().setNumberOfNodes(EdgeList.getTotalNodes() + 1);
				System.out.println(EdgeList.getTotalNodes() + "$$$$$$$$$$$$$$");
			}

			System.out.println(channel.channel().localAddress() + " -> open: " + channel.channel().isOpen()
					+ ", write: " + channel.channel().isWritable() + ", reg: " + channel.channel().isRegistered());
			return chan;

		} catch (Throwable ex) {
			logger.error("failed to initialize the client connection");
		}
		return chan;
	}

	

	@Override
	public synchronized void onAdd(EdgeInfo ei) {
		// TODO check connection
	}

	@Override
	public synchronized void onRemove(EdgeInfo ei) {
		// TODO ?
	}

	public class MonitorHBTask extends TimerTask {
		@Override
		public void run() {

			monitorHB();
		}
	}

	public int getEnqueuedTasks() {
		enqueuedTask = state.getTasks().numEnqueued();
		return enqueuedTask;
	}

	public int getprocessesTask() {
		processedTask = state.getTasks().numProcessed();
		return processedTask;
	}

	public boolean isQueueLessThanThreshold() {

		if (state.getTasks().numEnqueued() == 0 || state.getTasks().numEnqueued() <= 5) {
			return true;
		} else
			return false;
	}



//Sending tasks messages while data replication to othe nodes
	public synchronized void sendTaskMessages(WorkMessage wm) {
		
		for (EdgeInfo ei : this.outboundEdges.getEdgesMap().values()) {
			try {
				if (ei.getChannel() == null) {
					try {
						ei.setChannel(newChannel(ei.getHost(), ei.getPort(), false));
						ei.setActive(true);

					} catch (Exception e) { 
						logger.info("trying to connect to node " + ei.getRef());
					}

				}
				
				if (ei.isActive() && ei.getChannel() != null) {
					System.out.println(ei.getHost());
					System.out.println("HI");
					ei.getChannel().writeAndFlush(wm);

				} else {
					logger.info("trying to connect to node " + ei.getRef());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}
	

	//For creating a Read Response
	public CommandMessage createReadMessage(CommandMessage msg, Entry<String, String> entry, int size) {
		CommandMessage.Builder cmsg = CommandMessage.newBuilder(msg);
		Header.Builder header = Header.newBuilder();
		header.setNodeId(msg.getHeader().getNodeId());
		header.setDestination(msg.getHeader().getNodeId());
		header.setTime(System.currentTimeMillis());
		cmsg.setHeader(header);

		cmsg.setAction(Pipe.Action.READ);
		cmsg.setFilename(msg.getFilename());
		ReadResponse.Builder rd = ReadResponse.newBuilder();
		rd.setReadData(ByteString.copyFrom(entry.getValue().getBytes()));
		cmsg.setReadResponse(rd);
		Chunk.Builder chunk = Chunk.newBuilder();
		chunk.setChunkId(Integer.parseInt(entry.getKey()));
		chunk.setChunkSize(entry.getValue().length());
		chunk.setNumberOfChunks(size);
		cmsg.setChunk(chunk);
		return cmsg.build();
	}

	//For creating task messages to send for reading to the leader by the node obtained through round robin approach
	public WorkMessage createTaskMessage(CommandMessage cm, Entry<String, String> entry, int size) {
		CommandMessage cmsg = createReadMessage(cm, entry, size);

		Task.Builder tm = Task.newBuilder();
		tm.setSeqId((int)CommonUtils.UUIDGen());
		tm.setSeriesId(taskSeqId++);
		tm.setMessage(cmsg);
		tm.setTaskAction(TaskAction.RESPONSE);
		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(state.getConf().getNodeId());
		hb.setDestination(-1);
		hb.setTime(System.currentTimeMillis());
		
		WorkMessage.Builder wb = WorkMessage.newBuilder();
		wb.setHeader(hb);
		wb.setTask(tm);
		//checking secret key
		wb.setSecret(CommonUtils.SECRET_KEY);
		return wb.build();

	}
	//For creating task messages to send while data replication
	public WorkMessage createTaskMessage(CommandMessage cm) {
		Task.Builder tm = Task.newBuilder();
		tm.setSeqId((int)CommonUtils.UUIDGen());
		tm.setSeriesId(taskSeqId++);
		tm.setMessage(cm);

		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(state.getConf().getNodeId());
		hb.setDestination(-1);
		hb.setTime(System.currentTimeMillis());

		WorkMessage.Builder wb = WorkMessage.newBuilder();
		wb.setHeader(hb);
		wb.setTask(tm);
		//checking secret key
		wb.setSecret(CommonUtils.SECRET_KEY);
		return wb.build();

	}
}
