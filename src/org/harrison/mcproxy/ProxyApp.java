package org.harrison.mcproxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyApp
{
	public static void main(String[] args) throws IOException, InterruptedException
	{
		String dest = args[0];
		
		ServerSocket ss = new ServerSocket(25565);
		while (true)
		{
			Socket toClientSock = ss.accept();
			System.out.println("Got connection " + toClientSock.getRemoteSocketAddress() );
			
			Socket toServerSock = new Socket(dest, 25565);
			System.out.println("Made connection to server " + toServerSock.getRemoteSocketAddress() );
			
			ProxyConnection pc = new ProxyConnection(toClientSock, toServerSock);
			pc.start();
		}
		
	}
}
