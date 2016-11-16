//package gash.router.dns.server;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.util.Scanner;
//
//import com.google.protobuf.ByteString;
//
//import ClientFacingMapping.Mapping.MapAction;
//import gash.router.server.CommandInit;
//import gash.router.server.CommonUtils;
//import global.Global.GlobalCommandMessage;
//import io.netty.bootstrap.Bootstrap;
//import io.netty.channel.Channel;
//import io.netty.channel.ChannelFuture;
//import io.netty.channel.ChannelFutureListener;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.ChannelOption;
//import io.netty.channel.EventLoopGroup;
//import io.netty.channel.SimpleChannelInboundHandler;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.nio.NioSocketChannel;
//import pipe.common.Common.Header;
//import routing.Pipe.CommandMessage;
//import routing.Pipe.ReadResponse;
//import storage.Storage;
//// for INTER CLUSTER
//// handling incoming requests... we need identifier for msg from leaderr or from other cluster
//public class IPRequestHandler  extends SimpleChannelInboundHandler<GlobalCommandMessage> {
//
//	private ChannelFuture channel; // do not use directly call
//	// connect()!
//
//	private EventLoopGroup group;
//
//	@Override
//	protected void channelRead0(ChannelHandlerContext ctx, GlobalCommandMessage msg) throws Exception {
//		// TODO Auto-generated method stub
//		System.out.println("in handleee"+msg);
//		handleMessage(msg, ctx.channel());
//		System.out.println("in handleee "+msg); 
//		
//	}
//	public void handleMessage(GlobalCommandMessage msg, Channel channel) {
//		try {
//			
//			if(msg.hasQuery()){ // handling all queries
//			if (msg.getQuery().getAction().equals(MapAction.WHICHSERVER)){
//				//ClientFacingMessage.Builder cfm =  ClientFacingMessage.newBuilder();
////				GlobalCommandMessage.Builder gcm = GlobalCommandMessage.newBuilder();
////				Header.Builder hb = Header.newBuilder();
////				hb.setNodeId(msg.getHeader().getNodeId());
////				hb.setDestination(-1);
////				hb.setTime(-1);
////				gcm.setHeader(value)
////				gcm.setMapaction(MapAction.SERVERIS);
////				gcm.setIp(IPList.getHostIP());
////				gcm.setPort(IPList.getHostport());
////				channel.writeAndFlush(cfm.build());	
////				System.out.println("Inside which");
//			}		
//			else if(msg.getQuery().getAction().equals(MapAction.SERVERIS)){
//				System.out.println("HEllo" + msg.getQuery().getIp());				
//			}
//			else if(msg.getQuery().getAction().equals(Storage.Action.GET)){
//				System.out.println("In query  "+msg.getQuery().getKey());
//				
//				read(msg, channel);
//				
//			}else if(msg.getQuery().getAction().equals(Storage.Action.STORE)){
//				
//				
//			}else if(msg.getQuery().getAction().equals(Storage.Action.DELETE) ||
//					msg.getQuery().getAction().equals(Storage.Action.UPDATE)){
//				//not supported
//			}} else if(msg.hasResponse()){ //handling all responses
//			switch( msg.getResponse().getAction()){	
//			//	case  :
//					
//				
//			}
//				
//			}
//	}catch(Exception e){
//		
//	}	
//
//	}
//
//void read(GlobalCommandMessage msg, Channel channel){  //reading in our netowrk
//	try {
//		//ByteString byteStr = ByteString.copyFrom(imageConversion.imageConvert(filename));
//		Header.Builder hb = Header.newBuilder();
//		hb.setNodeId(54);
//		hb.setDestination(54);
//		hb.setTime(System.currentTimeMillis());
//
//		CommandMessage.Builder rb = CommandMessage.newBuilder();
//		rb.setHeader(hb);
//		rb.setAction(routing.Pipe.Action.READ);
//		
//		ReadResponse.Builder read = ReadResponse.newBuilder();
//		read.setReadData(ByteString.copyFromUtf8(msg.getQuery().getKey()));
//		rb.setReadResponse(read);
//		rb.setFilename(msg.getQuery().getKey());
//		rb.setTotalSize(msg.getQuery().getKey().length());
//		rb.setPort(CommonUtils.PORT);
//		//rb.setHost("192.168.1.52");
//		rb.setIntercluster(true);
//		
//	/*	Chunk.Builder chunk = Chunk.newBuilder();
//		chunk.setChunkId(1);
//		chunk.setChunkSize(byteStr.size());
//		chunk.setNumberOfChunks(byteStr.size() / (30 * 1048576));
//		rb.setChunk(chunk);*/
//		rb.setHost("192.168.1.52");
//		int id = (int) CommonUtils.UUIDGen();
//		rb.setMessageId(1234);
//		createConnectionToLeader(getLeaderFileInfo(), CommonUtils.PORT);
//		channel.writeAndFlush(rb.build());
//				//C.getInstance().enqueue(rb.build());}
//		
//		
//	} catch (Exception e) {
//		// TODO Auto-generated catch block
//		System.out.println("jjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjj");
//		e.printStackTrace();
//	}
//	
//	
//}
//
//private void createConnectionToLeader(String host, int port){
//	System.out.println("--> initializing connection to " + host + ":" + port);
//	
//
//	// the queue to support client-side surging
//	//outbound = new LinkedBlockingDeque<CommandMessage>();
//
//	group = new NioEventLoopGroup();
//	try {
//		CommandInit si = new CommandInit(null, false);
//		Bootstrap b = new Bootstrap();
//		b.group(group).channel(NioSocketChannel.class).handler(si);
//		b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
//		b.option(ChannelOption.TCP_NODELAY, true);
//		b.option(ChannelOption.SO_KEEPALIVE, true);
//
//		// Make the connection attempt.
//		channel = b.connect(host, port).syncUninterruptibly();
//
//		// want to monitor the connection to the server s.t. if we loose the
//		// connection, we can try to re-establish it.
//		ClClosedListener ccl = new ClClosedListener(this);
//		channel.channel().closeFuture().addListener(ccl);
//
//		System.out.println(channel.channel().localAddress() + " -> open: " + channel.channel().isOpen()
//				+ ", write: " + channel.channel().isWritable() + ", reg: " + channel.channel().isRegistered());
//
//	} catch (Throwable ex) {
//	//	logger.error("failed to initialize the client connection", ex);
//		ex.printStackTrace();
//	}
//}
//private String  getLeaderFileInfo(){
//	String content = null;
//	try {
//		String FilePath=System.getProperty("user.dir").replace("\\", "/");
//		content = new Scanner(new File(FilePath+"/leaderinfo.txt")).useDelimiter("\\Z").next();
//		System.out.println(content+"the contents...............................................");
//	} catch (FileNotFoundException e) {
//		// TODO Auto-generated catch block
//		e.printStackTrace();
//	}
//	return content;
//}
//
//public static class ClClosedListener implements ChannelFutureListener {
//	IPRequestHandler ipReq = null;
//
//	public ClClosedListener(IPRequestHandler ipReq) {
//		this.ipReq = ipReq;
//	}
//
//	@Override
//	public void operationComplete(ChannelFuture future) throws Exception {
//		// we lost the connection or have shutdown.
//		System.out.println("--> client lost connection to the server");
//		System.out.flush();
//
//		// @TODO if lost, try to re-establish the connection
//	}
//}
//}
//
//
//
//
//
