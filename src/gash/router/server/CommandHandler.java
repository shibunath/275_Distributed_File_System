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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.container.RoutingConf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import pipe.common.Common.Failure;
import pipe.work.Work.Task;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;

/**
 * The message handler processes json messages that are delimited by a 'newline'
 * 
 * TODO replace println with logging!
 * 
 * @author gash
 * 
 */
public class CommandHandler extends SimpleChannelInboundHandler<CommandMessage> {
	protected static Logger logger = LoggerFactory.getLogger("cmd");
	private static HashMap<Integer, Channel> channelMap=new HashMap<Integer, Channel>();
	protected RoutingConf conf;

	public CommandHandler(RoutingConf conf) {
		if (conf != null) {
			this.conf = conf;
		}
	}

	/**
	 * override this method to provide processing behavior. This implementation
	 * mimics the routing we see in annotating classes to support a RESTful-like
	 * behavior (e.g., jax-rs).
	 * 
	 * @param msg
	 */
	public void handleMessage(CommandMessage msg, Channel channel) {
		if (msg == null) {
			// TODO add logging
			System.out.println("ERROR: Unexpected content - " + msg);
			return;
		}

		PrintUtil.printCommand(msg);

		try

		{
			if (msg.hasPing()) {
				logger.info("ping from " + msg.getHeader().getNodeId());
			} else {
				if (msg.getAction().equals(routing.Pipe.Action.UPLOAD)) {
					// client msg obtained here
					logger.info((msg.getUploadData()).toString() + "Chunk Id===" + msg.getChunk().getChunkId());
					logger.info(msg.getFilename());
					channelMap.put(msg.getHeader().getNodeId(),channel);
					MessageServer.StartWorkCommunication.getInstance().store(channelMap);
					//Transfer to other servers within the network
					transferToWork(msg);

				} else {
					if (msg.getAction().equals(routing.Pipe.Action.READ)) {
						logger.info("Read from " + msg.getHeader().getNodeId());
						logger.info("$");
						channelMap.put(msg.getHeader().getNodeId(),channel);
						logger.info("$$"+msg.getHeader().getNodeId());
						MessageServer.StartWorkCommunication.getInstance().store(channelMap);
						//Transfer to other servers within the network
						transferToWork(msg);
					} else {
						logger.info("No Action");
					}
				}

			}
		} catch (Exception e){
			Failure.Builder eb = Failure.newBuilder();
			eb.setId(conf.getNodeId());
			eb.setRefId(msg.getHeader().getNodeId());
			eb.setMessage(e.getMessage());
			CommandMessage.Builder rb = CommandMessage.newBuilder(msg);
			rb.setErr(eb);
			channel.write(rb.build());
		}

		System.out.flush();

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
	
	
	private void transferToWork(CommandMessage msg) {
		try {
			CommandMessage.Builder cMsg = CommandMessage.newBuilder(msg);

			Task.Builder task = Task.newBuilder();
			task.setMessage(cMsg);
			task.setSeqId(12121);
			task.setSeriesId(261313);

			WorkMessage.Builder wm = WorkMessage.newBuilder();
			wm.setTask(task);
			wm.setHeader(msg.getHeader());

			if (msg.getAction().equals(routing.Pipe.Action.UPLOAD)) {
				//if not a single chunk	
				if (msg.getChunk().getNumberOfChunks() != 1) {
					//tracking chunks
					if (ChunkTracker.getChunkTracker().containsKey(msg.getMessageId())) {
						//set the message id and its corresponding chunks
						ChunkTracker.set(msg.getMessageId(), msg.getChunk().getChunkId());
					} else {
						//initialize an entry into chunktracker hashmap and set the message id to it
						ChunkTracker.setInitial(msg.getMessageId(), (int) msg.getChunk().getNumberOfChunks());
					}
					//set tasks for uploading into the databases
					if(MessageServer.StartWorkCommunication.getInstance()!=null){
					MessageServer.StartWorkCommunication.getInstance().setTaskFromClient(wm.getTask());
					}
				} else {
					//set tasks for uploading into the databases and if one whole message less than 1 MB is present
					MessageServer.StartWorkCommunication.getInstance().setTaskFromClient(wm.getTask());
				}
			} else if (msg.getAction().equals(routing.Pipe.Action.READ)) {
				//start the round robin process
				MessageServer.StartWorkCommunication.getInstance().roundRobin(wm.getTask());			
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
		System.out.println("HIIIIII");
		handleMessage(msg, ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error("Unexpected exception from downstream.", cause);
		ctx.close();
	}

}