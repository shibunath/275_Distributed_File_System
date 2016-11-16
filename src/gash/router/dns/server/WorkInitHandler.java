package gash.router.dns.server;

import java.net.InetAddress;

import com.google.protobuf.ByteString;

import ClientFacingMapping.Mapping.ClientFacingMessage;
import ClientFacingMapping.Mapping.MapAction;
import gash.router.server.CommonUtils;
import global.Global.GlobalCommandMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import pipe.common.Common.Header;
import routing.Pipe.CommandMessage;
import routing.Pipe.ReadResponse;
import storage.Storage;
// for WITHIN CLUSTER
public class WorkInitHandler extends SimpleChannelInboundHandler<CommandMessage>{
	@Override
	protected void channelRead0(ChannelHandlerContext ctx,  CommandMessage msg) throws Exception {
		// TODO Auto-generated method stub
		handleMessage(msg, ctx.channel());
		
	}
	public void handleMessage(CommandMessage msg, Channel channel) {
		try {
			
		    if(msg.hasReadResponse()){
		    	
		    } }catch(Exception e){
		    	
		    }
			
			
			
		
}
}
