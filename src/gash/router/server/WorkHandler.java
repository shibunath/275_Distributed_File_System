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
package gash.router.server;

import java.util.HashMap;
import java.util.Map;

import javax.rmi.CORBA.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.container.RoutingConf;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.tasks.NoOpBalancer;
import gash.router.server.tasks.TaskList;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import pipe.common.Common.Failure;
import pipe.common.Common.Header;
import pipe.election.Election;
import pipe.election.Election.LeaderStatus;
import pipe.election.Election.LeaderStatus.Action;
import pipe.election.Election.LeaderStatus.LeaderQuery;
import pipe.election.Election.LeaderStatus.LeaderState;
import pipe.election.Election.LeaderStatus.NodeState;
import pipe.work.Work.Heartbeat;
import pipe.work.Work.Task;
import pipe.work.Work.TaskAction;
import pipe.work.Work.WorkMessage;
import pipe.work.Work.WorkState;
import pipe.work.Work.WorkStealer;
import routing.Pipe;

/**
 * The message handler processes json messages that are delimited by a 'newline'
 * 
 * TODO replace println with logging!
 * 
 * @author gash l
 */
public class WorkHandler extends SimpleChannelInboundHandler<WorkMessage> {
	protected static Logger logger = LoggerFactory.getLogger("work");
	protected ServerState state;
	protected boolean debug = true;
	protected EdgeMonitor emon;
	public static int[] ctr = new int[255];

	public WorkHandler(ServerState state) {
		if (state != null) {
			this.state = state;
			emon = new EdgeMonitor(state);
		}
	}

	/**
	 * override this method to provide processing behavior. T
	 * 
	 * @param msg
	 * @throws InterruptedException
	 */
	public void handleMessage(WorkMessage msg, Channel channel) throws InterruptedException {
		if (msg == null) {
			logger.error("ERROR: Unexpected content - " + msg);
			return;
		}

		if (debug)
			// PrintUtil.printWork(msg);

			// TODO How can you implement this without if-else statements?
			try {
				if(msg.getSecret() != CommonUtils.SECRET_KEY){
					logger.error("Invalid secret key.Ignoring the message");
					return;
					}

				if (msg.hasBeat()) {
					Heartbeat hb = msg.getBeat();
					logger.info("heartbeat from " + msg.getHeader().getNodeId());
					// only after leader election
					// acknowledgeHB(msg);
					validateHeartBeats(msg);
				} else if (msg.hasPing()) {
					logger.info("ping from " + msg.getHeader().getNodeId());
					boolean p = msg.getPing();
					WorkMessage.Builder rb = WorkMessage.newBuilder();
					rb.setPing(true);
					channel.write(rb.build());
				} else if (msg.hasErr()) {
					Failure err = msg.getErr();
					logger.error("failure from " + msg.getHeader().getNodeId());
					// PrintUtil.printFailure(err);
				} else if (msg.hasTask()) {
					Task t = msg.getTask();
					
					
					if (t.getMessage().getAction().equals(routing.Pipe.Action.UPLOAD)) {
						//if not a single chunk	
						if (t.getMessage().getChunk().getNumberOfChunks() > 1) {
							//tracking chunks
							if (ChunkTracker.getChunkTracker().containsKey(t.getMessage().getMessageId())) {
								//set the message id and its corresponding chunks
								ChunkTracker.set(t.getMessage().getMessageId(), t.getMessage().getChunk().getChunkId());
							} else {
								//initialize an entry into chunktracker hashmap and set the message id to it
								ChunkTracker.setInitial(t.getMessage().getMessageId(),
										(int) t.getMessage().getChunk().getNumberOfChunks());
							}
							//set tasks for uploading into the databases
							if (MessageServer.StartWorkCommunication.getInstance() != null) {
								MessageServer.StartWorkCommunication.getInstance().setTaskFromClient(t);
							} 
						} else {
							//set tasks for uploading into the databases and if one whole message less than 1 MB is present
							MessageServer.StartWorkCommunication.getInstance().setTaskFromClient(t);
						}
					}
					
					
						if (t.getMessage().getAction().equals(Pipe.Action.READ)) {
							//obtaining leader Id
							for(EdgeInfo ei: state.getEmon().getOutBoundEdges().getEdgesMap().values()){
								if(ei.getRef() == state.getClusterInfo().getLeaderID()){
									state.setReadCh(ei.getChannel());
								}							
							}
							//sending response of read back to the leader
							if (t.getTaskAction().equals(TaskAction.RESPONSE)) {
								if (state.isLeader) {
									logger.info("$"+t.getMessage().getHeader().getNodeId());
									Channel chnl = state.getChannelMap().get(t.getMessage().getHeader().getNodeId());
									chnl.writeAndFlush(t.getMessage());
								} else {
									state.getReadCh().writeAndFlush(t.getMessage());
								}
							} else {
								//if just READ then process it
								MessageServer.StartWorkCommunication.getInstance().setTaskFromClient(msg.getTask());
							}

						} 
					} else if (msg.hasState() && (msg.getState().getSteal())) { 
						if(state.isLeader){
							int wrkStealerNode=msg.getSteal().getNodeId();
							Header.Builder hb = Header.newBuilder();
							hb.setNodeId(state.getConf().getNodeId());
							hb.setDestination(-1);
							hb.setTime(System.currentTimeMillis());

							WorkMessage.Builder wb = WorkMessage.newBuilder();
							wb.setHeader(hb);
							wb.setSecret(CommonUtils.SECRET_KEY);
							
							WorkStealer.Builder ws= WorkStealer.newBuilder();
							ws.setNodeId(wrkStealerNode);
							wb.setSteal(ws);
							channel.writeAndFlush(wb);
						}
				}  else if (msg.hasLeader()) {
						LeaderElectionUtils leaderElectionThread;
						if (emon != null)
							leaderElectionThread = emon.geLeaderElectionThread();
						else
							emon = state.getEmon();
						if (msg.getLeader().getQuery().equals(LeaderQuery.WHOISTHELEADER)
								&& state.getClusterInfo().leaderID >= 0) {
							System.out.println("hey buddy,leader is already set dude...");
							LeaderStatus ls = msg.getLeader();
							Header.Builder hb = Header.newBuilder();
							hb.setNodeId(state.getConf().getNodeId());
							hb.setDestination(-1);
							hb.setTime(System.currentTimeMillis());

							WorkMessage.Builder wb = WorkMessage.newBuilder();
							wb.setHeader(hb);
							wb.setSecret(CommonUtils.SECRET_KEY);
							LeaderStatus.Builder lb = LeaderStatus.newBuilder();
							lb.setState(LeaderState.LEADERALIVE);

							lb.setQuery(LeaderQuery.THELEADERIS);

							lb.setMsgId(state.getConf().getNodeId());
							lb.setOriginNodeId(state.getConf().getNodeId());
							lb.setLeaderId(state.getClusterInfo().getLeaderID());
							lb.setTerm(1);
							lb.setPrevTerm(0);
							wb.setLeader(lb);

							channel.writeAndFlush(wb.build());

				            //new edge discovered, add it to outboundedegs	
							state.getEmon().createOutboundIfNew(msg.getHeader().getNodeId(), channel);
							for (EdgeInfo ei : state.getEmon().getOutBoundEdges().getEdgesMap().values()){
							System.out.println("Inside new eeeedge");
							if(ei.getRef() == msg.getHeader().getNodeId()){
							System.out.println("Inside new edge");
							System.out.println(ei.getHost()+"Nodeee ID");
							ei.setChannel(emon.newChannel(ei.getHost(), ei.getPort(), false));
							ei.setActive(true);

								}

							}

							return;
						}
						ClusterInfo clusterInfo;
						clusterInfo = state.getClusterInfo();

						if (clusterInfo.getLeaderID() < 0) {
							leaderElectionThread = emon.geLeaderElectionThread();
							leaderElectionThread.onLeaderElectionMsgRecieved(msg, state);
						}

					} else
						logger.error("Unknown");
				
			} catch (Exception e) {
				logger.error("exception in message handler");
				e.printStackTrace();
				Failure.Builder eb = Failure.newBuilder();
				eb.setId(state.getConf().getNodeId());
				eb.setRefId(msg.getHeader().getNodeId());
				eb.setMessage(e.getMessage());
				WorkMessage.Builder rb = WorkMessage.newBuilder(msg);
				rb.setErr(eb);
				channel.write(rb.build());
			}

	}

	/**
	 * a message was received from the server. Here we dispatch the message to
	 * the client's thread pool to minimize the time it takes to process other
	 * messages.
	 * 
	 * @param ctx
	 *            The channel the message was received from
	 * @param msg
	 *            The message
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, WorkMessage msg) throws Exception {
		System.out.println("handle message: " + ctx.channel());
		handleMessage(msg, ctx.channel());
	}

	private void validateHeartBeats(WorkMessage wm) {
		int id = wm.getHeader().getNodeId();
		if (state.isLeader) {
			if (id > -1)
				WorkHandler.ctr[id] = WorkHandler.ctr[id] + 2;
		} else {
			if (id > -1 && id == state.getClusterInfo().getLeaderID())
				WorkHandler.ctr[id] = WorkHandler.ctr[id] + 2;
		}
		if (id > -1 && WorkHandler.ctr[id] > CommonUtils.RETRY_HB) {
			WorkHandler.ctr[id] = CommonUtils.RETRY_HB;
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
	}

}