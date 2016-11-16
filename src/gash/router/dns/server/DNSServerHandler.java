package gash.router.dns.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import javax.naming.CommunicationException;

import com.google.protobuf.ByteString;

import gash.router.server.CommandInit;
import gash.router.server.CommonUtils;
import global.Global.GlobalCommandMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import pipe.common.Common.Failure;
import pipe.common.Common.Header;
import routing.Pipe.CommandMessage;
import routing.Pipe.ReadResponse;
import storage.Storage;
import storage.Storage.Action;
import storage.Storage.Query;

public class DNSServerHandler  extends SimpleChannelInboundHandler<GlobalCommandMessage> {
	private ChannelFuture channel;
	private EventLoopGroup group;
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, GlobalCommandMessage msg) throws Exception {

		handleMessage(msg, ctx.channel());

	}
	public void handleMessage(GlobalCommandMessage msg, Channel channel) {
		try {
			//IPList.setHostIP(msg.getIp());
		
			if(msg.hasQuery()){
				Query query = msg.getQuery();
			
				if(query.getAction().equals(Action.WHICHSERVER)){
					String leaderIp = getLeaderFileInfo();
					System.out.println("LeaderInfo is "+getLeaderFileInfo());
					Header.Builder hb = Header.newBuilder();
					//hb.setNodeId(); // tmp
					hb.setDestination(-1);
					hb.setTime(1);
					
					GlobalCommandMessage.Builder gcb =  GlobalCommandMessage.newBuilder();
					gcb.setHeader(hb);
					
					Storage.Response.Builder rqb = Storage.Response.newBuilder();
					rqb.setAction(Action.SERVERIS);
					rqb.setIp(leaderIp);
					gcb.setResponse(rqb);
					System.out.println("in init cfm");
					channel.writeAndFlush(gcb.build());

				}
				//handle read from other Clusters
				else if(msg.getQuery().getAction().equals(Storage.Action.GET)){
					System.out.println("In query  "+msg.getQuery().getKey());
					
					read(msg, channel);
					
			}	
					
			}
			if(msg.hasResponse()){
		

			}
			System.out.println("HostIP ");
			//IPList.setHostPort(msg.getPort());
			//IPList.setRead(true);
		}catch(Exception e){

		}

	}
	private String  getLeaderFileInfo(){
		String content = null;
		try {
			String FilePath=System.getProperty("user.dir").replace("\\", "/");
			content = new Scanner(new File(FilePath+"/leaderinfo.txt")).useDelimiter("\\Z").next();
		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return content;
	}
	
	void read(GlobalCommandMessage msg, Channel channel){  //reading in our netowrk
	try {
		//ByteString byteStr = ByteString.copyFrom(imageConversion.imageConvert(filename));
		String leaderIp = getLeaderFileInfo();
		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(54);
		hb.setDestination(54);
		hb.setTime(System.currentTimeMillis());

		CommandMessage.Builder rb = CommandMessage.newBuilder();
		rb.setHeader(hb);
		rb.setAction(routing.Pipe.Action.READ);
		
		ReadResponse.Builder read = ReadResponse.newBuilder();
		read.setReadData(ByteString.copyFromUtf8(msg.getQuery().getKey()));
		rb.setReadResponse(read);
		rb.setFilename(msg.getQuery().getKey());
		rb.setTotalSize(msg.getQuery().getKey().length());
		rb.setPort(CommonUtils.PORT);
		//rb.setHost("192.168.1.52");
		rb.setIntercluster(true);
		
	/*	Chunk.Builder chunk = Chunk.newBuilder();
		chunk.setChunkId(1);
		chunk.setChunkSize(byteStr.size());
		chunk.setNumberOfChunks(byteStr.size() / (30 * 1048576));
		rb.setChunk(chunk);*/
		rb.setHost(leaderIp);
		int id = (int) CommonUtils.UUIDGen();
		rb.setMessageId(id);
		createConnectionToLeader(getLeaderFileInfo(), CommonUtils.PORT);
		channel.writeAndFlush(rb.build());
				//C.getInstance().enqueue(rb.build());}
		
		
	} catch (Exception e) {
		// TODO Auto-generated catch block
	
		e.printStackTrace();
	}
	
	
}
	
	private void createConnectionToLeader(String host, int port){
		System.out.println("--> initializing connection to " + host + ":" + port);
		

		// the queue to support client-side surging
		//outbound = new LinkedBlockingDeque<CommandMessage>();

		group = new NioEventLoopGroup();
		try {
			WorkInitDNS si = new WorkInitDNS(false);
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class).handler(si);
			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
			b.option(ChannelOption.TCP_NODELAY, true);
			b.option(ChannelOption.SO_KEEPALIVE, true);

			// Make the connection attempt.
			channel = b.connect(host, port).syncUninterruptibly();

			// want to monitor the connection to the server s.t. if we loose the
			// connection, we can try to re-establish it.
			ClClosedListener ccl = new ClClosedListener(this);
			channel.channel().closeFuture().addListener(ccl);

			System.out.println(channel.channel().localAddress() + " -> open: " + channel.channel().isOpen()
					+ ", write: " + channel.channel().isWritable() + ", reg: " + channel.channel().isRegistered());

		} catch (Throwable ex) {
		//	logger.error("failed to initialize the client connection", ex);
			ex.printStackTrace();
		}
	}
	
	public static class ClClosedListener implements ChannelFutureListener {
		DNSServerHandler ipReq = null;

		public ClClosedListener(DNSServerHandler ipReq) {
			this.ipReq = ipReq;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			// we lost the connection or have shutdown.
			System.out.println("--> client lost connection to the server");
			System.out.flush();

			// @TODO if lost, try to re-establish the connection
		}
	}
	
}

