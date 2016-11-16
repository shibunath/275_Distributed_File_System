//package gash.router.server;
//
//
//import java.util.HashMap;
//
//import ClientFacingMapping.Mapping.ClientFacingMessage;
//import ClientFacingMapping.Mapping.MapAction;
//import global.Global.GlobalCommandMessage;
//import storage.Storage;
//import storage.Storage.Action;
//import storage.Storage.Query;
//import storage.Storage.Response;
//import io.netty.bootstrap.Bootstrap;
//import io.netty.bootstrap.ServerBootstrap;
//import io.netty.channel.Channel;
//import io.netty.channel.ChannelFuture;
//import io.netty.channel.ChannelOption;
//import io.netty.channel.EventLoopGroup;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.nio.NioSocketChannel;
//import pipe.common.Common.Header;
//
//public class DNSServer {
//	protected static HashMap<Integer, ServerBootstrap> bootstrap = new HashMap<Integer, ServerBootstrap>();
//	private static int DNSSERVERPORT = CommonUtils.DNS_PORT; //port of the command prompt
//	private static Channel channel;
//	private static EventLoopGroup group; 
//	private static ChannelFuture future;
//	public static String leaderHost = null;
//	public static void UpdateLeaderDNSEntry(String host, ServerState state) {
//		try {
//			System.out.println("setting the leader now in DNS ................");
//			leaderHost = host;
//			group = new NioEventLoopGroup();
//			//WorkInit si = new WorkInit(null, false);
//			ServerDNSInit mi = new ServerDNSInit(false);
//			Bootstrap b = new Bootstrap();
//			b.group(group).channel(NioSocketChannel.class).handler(mi);
//			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000);
//			b.option(ChannelOption.TCP_NODELAY, true);
//			b.option(ChannelOption.SO_KEEPALIVE, true);
//
//			System.out.println("Channel  ---- " + host);
//
//			// Make the connection attempt.b.bind
//			System.out.println("LEADER CAME LAUNCHING THE MAPPING SERVER");
//			future = b.connect(CommonUtils.DNS_HOST, CommonUtils.DNS_PORT).syncUninterruptibly();
//			channel = future.channel();
//			GlobalCommandMessage.Builder cfm =  GlobalCommandMessage.newBuilder();
//			Header.Builder hb = Header.newBuilder();
//			hb.setNodeId(state.getConf().getNodeId());
//			hb.setDestination(-1);
//			hb.setTime(-1);
//			cfm.setHeader(hb);
//			Response.Builder rb = Response.newBuilder();
//			rb.setAction(Action.SERVERIS);
//			rb.setIp(host);
//			rb.setPort(CommonUtils.COMMAND_PORT);
//			cfm.setResponse(rb);
//			//Query.Builder qb =  Query.newBuilder();
//			//qb.setAction(Action.SERVERIS);
//			//qb.setPort(CommonUtils.COMMAND_PORT);
//			//qb.setIp(host);
//			//cfm.setQuery(qb);
//		//	cfm.setQuery(valMapAction.SERVERIS);
//			
//			
//			channel.writeAndFlush(cfm.build()); 
//			
//
//			
//	}catch(Exception e){
//		
//	}
//	}
//}
