package org.harrison.mcproxy;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ProxyConnection
{
	private Socket clientSocket;
	private Socket serverSocket;
	private InputStream server_is;
	private OutputStream server_os;
	private InputStream client_is;
	private OutputStream client_os;
	
	private Thread toServerThread;
	private Thread toClientThread;
	
	private String proxyDesc;
	
	public ProxyConnection(Socket clientSocket, Socket serverSocket) throws IOException
	{
		this.clientSocket = clientSocket;
		this.serverSocket = serverSocket;
		
		this.client_is = clientSocket.getInputStream();
		this.client_os = clientSocket.getOutputStream();
		
		this.server_is = serverSocket.getInputStream();
		this.server_os = serverSocket.getOutputStream();
		
		proxyDesc = clientSocket.getInetAddress().getHostAddress() + "-" + clientSocket.getPort() + " to " +serverSocket.getInetAddress().getHostAddress() + "-" + serverSocket.getPort();
		
		toServerThread = new Thread( createStreamCopyProc(client_is, server_os, "client_to_server"));
		toServerThread.setName("Proxy " + proxyDesc + " TO SERVER");
		
		toClientThread = new Thread( createStreamCopyProc(server_is, client_os, "server_to_client"));
		toClientThread.setName("Proxy " + proxyDesc + " TO CLIENT");
	}
	
	public String getDesc()
	{
		return proxyDesc;
	}
	
	void threadExiting() throws IOException
	{
		if ( clientSocket != null && !clientSocket.isClosed() ) clientSocket.close();
		if ( serverSocket != null && !serverSocket.isClosed() ) serverSocket.close();
		//if ( toServerThread != null && !toServerThread.isInterrupted() ) toServerThread.interrupt();
		//if ( toClientThread != null && !toClientThread.isInterrupted() ) toClientThread.interrupt();
	}
	
	public void start() throws InterruptedException
	{
		toServerThread.start();
		toClientThread.start();
	}
	
	public void stop() throws InterruptedException
	{
		toServerThread.join();
		toClientThread.join();

		System.out.println("ProxyConnection done");
	}
	
	void dumpPacket(byte[] buffer, int length, long packetNum, String label) throws IOException
	{
		try
		(
			FileOutputStream fos = new FileOutputStream(label + "_" + packetNum + ".bin"); 
		)
		{
			fos.write(buffer, 0, length);
		}
	}
	
	public Runnable createStreamCopyProc(final InputStream is, final OutputStream os, final String label)
	{
		Runnable proc = new Runnable()
		{
			private byte[] buffer = new byte[32 * 1024];
			private StatCounter bytesCopiedStat = new StatCounter("bps", "pps");
			private long snapshotNum = 0;
			
			@Override
			public void run()
			{
				try
				{
					int bytesRead;
					
					while ( !Thread.currentThread().isInterrupted() && (bytesRead = is.read(buffer)) > 0 )
					{
						os.write(buffer, 0, bytesRead);
						bytesCopiedStat.bump(bytesRead);
						
						dumpPacket(buffer, bytesRead, bytesCopiedStat.getTotal().getCount(), proxyDesc + "_" + label);
						displayStats();
					}
				}
				catch (IOException e)
				{
					//System.out.println("IOException in copystream proc: " + e);
				}
				finally
				{
					System.out.println("Exiting copystream proc " + label + ": " + bytesCopiedStat.toString() );
					try { threadExiting(); } catch ( IOException ioe ) { }
				}
			}
			
			void displayStats()
			{
				if ( snapshotNum != bytesCopiedStat.getSnapshotNum() )
				{
					System.out.println(label + ": " + bytesCopiedStat.toString() );
					snapshotNum = bytesCopiedStat.getSnapshotNum();
				}
			}
			
		};
		return proc;
	}
	
}
