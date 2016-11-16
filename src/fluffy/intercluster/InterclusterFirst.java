//package fluffy.intercluster;
//
//import gash.router.server.CommonUtils;
//import gash.router.server.ServerDNSInit;
//import global.Global.GlobalCommandMessage;
//import io.netty.bootstrap.Bootstrap;
//import io.netty.channel.Channel;
//import io.netty.channel.ChannelFuture;
//import io.netty.channel.ChannelOption;
//import io.netty.channel.EventLoopGroup;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.nio.NioSocketChannel;
//import pipe.common.Common.Header;
//import storage.Storage.Action;
//import storage.Storage.Query;
//
//public class InterclusterFirst {
//
//	public static void main(String[] args) {
//		UpdateLeaderDNSEntry();
//		// TODO Auto-generated method stub
////		Bootstrap b = new Bootstrap();
////		b.group(new NioEventLoopGroup()).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
////		.handler(new ChannelInitializer<SocketChannel>() {
////			@Override
////			protected void initChannel(SocketChannel arg0) throws Exception {
////				// TODO Auto-generated method stub
////			}
////		}).connect("192.168.1.51", 4000).addListener(new ChannelFutureListener() {
////			@Override
////			public void operationComplete(ChannelFuture future) throws Exception {
////				if (!future.isSuccess()) {
////					return;
////				}
////				try {
////					Header.Builder hb = Header.newBuilder();
////					hb.setNodeId(9);
////					hb.setDestination(-1);
////					hb.setTime(-1);
////					Query.Builder qb=Query.newBuilder();
////					qb.setAction(Action.GET);
////					qb.setKey("raft.pdf");
////					GlobalCommandMessage.Builder gm=GlobalCommandMessage.newBuilder();
////					gm.setHeader(hb);
////					gm.setQuery(qb);					
////					Channel c = future.channel();					
////					c.writeAndFlush(gm.build());
////					System.out.println("channel formed");
////					InetSocketAddress addr = (InetSocketAddress) c.remoteAddress();
////					System.out.println("inet socket");
////					String address = addr.getAddress().getHostAddress();
////					System.out.println("Successfully connected to " + addr.getAddress().getHostAddress());
////					
////					}
////				 catch (RuntimeException e) {
////					System.out.println("jjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjj");
////					e.printStackTrace();
////				}
////			}
////
////	
////});
//	}
//		
//		public static void UpdateLeaderDNSEntry() {
//			Channel channel;
//			EventLoopGroup group; 
//			ChannelFuture future;
//			
//		try {
//			
//			group = new NioEventLoopGroup();
//			//WorkInit si = new WorkInit(null, false);
//			ServerDNSInit mi = new ServerDNSInit(false);
//			Bootstrap b = new Bootstrap();
//			b.group(group).channel(NioSocketChannel.class).handler(mi);
//			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000);
//			b.option(ChannelOption.TCP_NODELAY, true);
//			b.option(ChannelOption.SO_KEEPALIVE, true);
//
//			System.out.println("Channel  ---- This is initing the constructor ");
//
//			// Make the connection attempt.b.bind
//
//			future = b.connect("127.0.0.1", CommonUtils.DNS_PORT).syncUninterruptibly(); //tmp 
//			channel = future.channel();
//			GlobalCommandMessage.Builder cfm =  GlobalCommandMessage.newBuilder();
//			Header.Builder hb = Header.newBuilder();
//			hb.setNodeId(34); //tmp
//			hb.setDestination(-1);
//			hb.setTime(1);
//			cfm.setHeader(hb);
//			Query.Builder qb =  Query.newBuilder();
//			qb.setAction(Action.GET);
//			qb.setPort(5000);
//			qb.setIp("127.0.0.1"); //tmp
//			
//			cfm.setQuery(qb);
//		//	cfm.setQuery(valMapAction.SERVERIS);
//			channel.writeAndFlush(cfm.build()); 
//	}catch(Exception e){
//		
//	}
//	
//	}
//}
