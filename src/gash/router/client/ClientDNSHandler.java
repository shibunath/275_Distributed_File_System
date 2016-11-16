package gash.router.client;


import global.Global.GlobalCommandMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ClientDNSHandler extends SimpleChannelInboundHandler<GlobalCommandMessage> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, GlobalCommandMessage msg) throws Exception {

		handleMessage(msg, ctx.channel());

	}
	public void handleMessage(GlobalCommandMessage msg, Channel channel) {
		try {
				if(msg.hasResponse()){
					//Set Leader IP, recieved from DNS..
					MessageClient.leaderIp = msg.getResponse().getIp();
				}
			System.out.println("HostIP ");
		}catch(Exception e){
	}

	}
	
	
}

