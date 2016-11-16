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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.container.RoutingConf;
import gash.router.container.RoutingConf.RoutingEntry;
import gash.router.server.datareplication.DataReplication;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.queuemanagement.ReadHashing;
import gash.router.server.tasks.NoOpBalancer;
import gash.router.server.tasks.TaskList;
import gash.router.server.tasks.WrkBalancer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import pipe.election.Election.LeaderStatus;
import pipe.work.Work.Task;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;

public class MessageServer {
	protected static Logger logger = LoggerFactory.getLogger("server");

	protected static HashMap<Integer, ServerBootstrap> bootstrap = new HashMap<Integer, ServerBootstrap>();

	// public static final String sPort = "port";
	// public static final String sPoolSize = "pool.size";

	public RoutingConf conf;
	protected boolean background = false;
	private static AtomicReference<MessageServer> instance = new AtomicReference<MessageServer>();
	
	public static MessageServer getInstance() {
		// init the netty connection
		return instance.get();
	}
	
	public static MessageServer initFileMessageServer(File cfg){
		//init the instance and start the netty connection
		instance.compareAndSet(null, new MessageServer(cfg));
		return instance.get();
	}
	
	public static MessageServer initRoutingMessageServer(RoutingConf conf){
		//init the instance and start the netty connection
		instance.compareAndSet(null, new MessageServer(conf));
		return instance.get();
	}

	/**
	 * initialize the server with a configuration of it's resources
	 * 
	 * @param cfg
	 */
	
	public MessageServer(File cfg) {
		init(cfg);
	}

	public MessageServer(RoutingConf conf) {
		this.conf = conf;
		CommonUtils.conf = conf;
	}

	public void release() {
	}

	public void startServer() {
		StartWorkCommunication comm = StartWorkCommunication.initStartWorkComm(conf);
		System.out.println("Work starting");

		// We always start the worker in the background
		Thread cthread = new Thread(comm);
		cthread.start();

		if (!conf.isInternalNode()) {
			StartCommandCommunication comm2 = new StartCommandCommunication(conf);
			System.out.println("Command starting");

			if (background) {
				Thread cthread2 = new Thread(comm2);
				cthread2.start();
			} else
				comm2.run();
		}
	}

	/**
	 * static because we need to get a handle to the factory from the shutdown
	 * resource
	 */
	public static void shutdown() {
		logger.info("Server shutdown");
		System.exit(0);
	}

	private void init(File cfg) {
		if (!cfg.exists())
			throw new RuntimeException(cfg.getAbsolutePath() + " not found");
		// resource initialization - how message are processed
		BufferedInputStream br = null;
		try {
			byte[] raw = new byte[(int) cfg.length()];
			br = new BufferedInputStream(new FileInputStream(cfg));
			br.read(raw);
			conf = JsonUtil.decode(new String(raw), RoutingConf.class);
			if (!verifyConf(conf))
				throw new RuntimeException("verification of configuration failed");
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		ArrayList<RoutingEntry> routeEntry = new ArrayList<RoutingEntry>();
		if (conf.getRouting().isEmpty()){
		ArrayList<String> hostList = CommonUtils.pingAll();
		for(String host: hostList){	
		RoutingEntry tempRe = new RoutingEntry();
		tempRe.setHost(host);
		System.out.println(host.split(".").length+" size is"+hostList.size());
		int id = Integer.valueOf(host.split("\\.")[3]); 
		tempRe.setId(id);
		tempRe.setPort(CommonUtils.PORT);
		System.out.println(tempRe.getHost()+"-  routing  ");
		routeEntry.add(tempRe);
		}
//			conf.setRouting(routeEntry);
		conf.setRouting(routeEntry);
		}
	
	}

	private boolean verifyConf(RoutingConf conf) {
		return (conf != null);
	}

	/**
	 * initialize netty communication
	 * 
	 * @param port
	 *            The port to listen to
	 */
	private static class StartCommandCommunication implements Runnable {
		RoutingConf conf;

		public StartCommandCommunication(RoutingConf conf) {
			this.conf = conf;
		}

		public void run() {
			// construct boss and worker threads (num threads = number of cores)

			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();

			try {
				ServerBootstrap b = new ServerBootstrap();
				bootstrap.put(conf.getCommandPort(), b);

				b.group(bossGroup, workerGroup);
				b.channel(NioServerSocketChannel.class);
				b.option(ChannelOption.SO_BACKLOG, 100);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);
				// b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR);

				boolean compressComm = false;
				b.childHandler(new CommandInit(conf, compressComm));

				// Start the server.
				logger.info("Starting command server (" + conf.getNodeId() + "), listening on port = "
						+ conf.getCommandPort());
				ChannelFuture f = b.bind(conf.getCommandPort()).syncUninterruptibly();

				logger.info(f.channel().localAddress() + " -> open: " + f.channel().isOpen() + ", write: "
						+ f.channel().isWritable() + ", act: " + f.channel().isActive());

				// block until the server socket is closed.
				f.channel().closeFuture().sync();

			} catch (Exception ex) {
				// on bind().sync()
				logger.error("Failed to setup handler.", ex);
			} finally {
				// Shut down all event loops to terminate all threads.
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();
			}
		}
		
		
	
	}

	/**
	 * initialize netty communication
	 * 
	 * @param port
	 *            The port to listen to
	 */
	public static class StartWorkCommunication implements Runnable {
		ServerState state;
		
		public StartWorkCommunication(RoutingConf conf) {



			if (conf == null)
				throw new RuntimeException("missing conf");

			state = new ServerState();
			state.setConf(conf);
			state.nodeState = LeaderStatus.NodeState.FOLLOWER_STATE;

			TaskList tasks = new TaskList(new NoOpBalancer());
			state.setTasks(tasks);
			state.setLeader(false);
			state.setTerm(1);
			state.setPrevTerm(0);
			ClusterInfo clusterInfo = new ClusterInfo();
			state.setClusterInfo(clusterInfo);
			
			
			EdgeMonitor emon = new EdgeMonitor(state);
			Thread t = new Thread(emon);
			t.start();
	
			//Starting the data processing thread that listens the tasklist and pulls tasks once it gets enqueued and proceses them
			DataProcessingThread dataProcessor=new DataProcessingThread(state.getTasks(),state);
			Thread th=new Thread(dataProcessor);
			th.start();
			
			WrkBalancer wrkbalance= new WrkBalancer(state);
			Thread wrb=new Thread(wrkbalance);
			wrb.start();
		}

		//enqueueing the task list
		public void setTaskFromClient(Task tsk) {
			TaskList taskList = state.getTasks();
			taskList.addTask(tsk);
			state.setTasks(taskList);

		}
		
		//starting the data replication thread
		public void dataReplicate(CommandMessage cm){
				DataReplication dataReplication=new DataReplication(state,cm);
				Thread th=new Thread(dataReplication);
				th.start();
		}
		
		//Storing client channel to send response
		public void store(HashMap<Integer, Channel> channelMap){		
			state.setChannelMap(channelMap);			
		}
		
		//obtain id through rond robin and delegate read request to this node
		public void roundRobin(Task task){		
			if(state.isLeader()){
				ReadHashing rh=new ReadHashing();
				int nodeId= rh.roundRobin(state);
				if(state.isCheckStealNode() && state.getTasks().numEnqueued()<3 ){
					nodeId=state.getStealNodeId();
					state.getTasks().rebalance();
				}
				WorkMessage wm=state.getEmon().createTaskMessage(task.getMessage());
				EdgeInfo ei=state.getEmon().getOutBoundEdges().getNode(nodeId);
				if(ei.getChannel()==null){
					Channel ch=state.getEmon().newChannel(ei.getHost(), ei.getPort(),false);
					ch.writeAndFlush(wm);
				}else{
					ei.getChannel().writeAndFlush(wm);
				}
				
			}else{
				//if not a leader, process the request
				setTaskFromClient(task);
			}
		}
		
		
		
		public void run() {
			// construct boss and worker threads (num threads = number of cores)

			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();

			try {
				ServerBootstrap b = new ServerBootstrap();
				bootstrap.put(state.getConf().getWorkPort(), b);

				b.group(bossGroup, workerGroup);
				b.channel(NioServerSocketChannel.class);
				b.option(ChannelOption.SO_BACKLOG, 100);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);
				// b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR);
				boolean compressComm = false;
				b.childHandler(new WorkInit(state, compressComm));

				// Start the server.
				System.out.println("Starting work server (" + state.getConf().getNodeId() + "), listening on port = "
						+ state.getConf().getWorkPort());
				ChannelFuture f = b.bind(state.getConf().getWorkPort()).syncUninterruptibly();

				logger.info(f.channel().localAddress() + " -> open: " + f.channel().isOpen() + ", write: "
						+ f.channel().isWritable() + ", act: " + f.channel().isActive());

				// block until the server socket is closed.
				f.channel().closeFuture().sync();

			} catch (Exception ex) {
				// on bind().sync()
				logger.error("Failed to setup handler.", ex);
			} finally {
				// Shut down all event loops to terminate all threads.
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();

				// shutdown monitor
				EdgeMonitor emon = state.getEmon();
				if (emon != null)
					emon.shutdown();
			}
		}
		
		
		
		public void init(){
			
		}
		
		
		private static AtomicReference<StartWorkCommunication> instance = new AtomicReference<StartWorkCommunication>();

		public static StartWorkCommunication getInstance() {
			// init the netty connection
			System.out.println("HIIII INSTANCE *******************"+instance.get());
			return instance.get();
		}
		
		public static StartWorkCommunication initStartWorkComm(RoutingConf conf){
			//init the instance and start the netty connection
			instance.compareAndSet(null, new StartWorkCommunication(conf));
			return instance.get();
		}
	}

	/**
	 * help with processing the configuration information
	 * 
	 * @author gash
	 *
	 */
	public static class JsonUtil {
		private static JsonUtil instance;

		public static void init(File cfg) {

		}

		public static JsonUtil getInstance() {
			if (instance == null)
				throw new RuntimeException("Server has not been initialized");

			return instance;
		}

		public static String encode(Object data) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				return mapper.writeValueAsString(data);
			} catch (Exception ex) {
				return null;
			}
		}

		public static <T> T decode(String data, Class<T> theClass) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				return mapper.readValue(data.getBytes(), theClass);
			} catch (Exception ex) {
				ex.printStackTrace();
				return null;
			}
		}
	}

}
