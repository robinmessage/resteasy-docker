package uk.ac.cam.cl.dtg.teaching.docker.api;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerWsApiImpl implements DockerWsApi {

	private static final Logger LOG = LoggerFactory.getLogger(DockerWsApiImpl.class);
	
	private String hostname;
	private int port;
	private WebSocketClient client;

	public DockerWsApiImpl(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
		client = new WebSocketClient();
        try {
			client.start();
		} catch (Exception e) {
			LOG.error("Failed to start WebSocketClient",e);
			throw new Error(e);
		}
	}
	
	
	public void attach(final String containerId, 
			final boolean logs, 
			final boolean stream, 
			final boolean stdout, 
			final boolean stderr, 
			final boolean stdin,
			final WebSocketListener listener) {
        String url = String.format("ws://%s:%d/containers/%s/attach/ws?logs=%s&stream=%s&stdout=%s&stderr=%s&stdin=%s",
        		hostname,
        		port,
        		containerId,
        		logs?"1":"0",
				stream?"1":"0",
				stdout?"1":"0",
				stderr?"1":"0",
				stdin?"1":"0");
		try {
			URI uri = new URI(url);
			ClientUpgradeRequest req = new ClientUpgradeRequest();
			req.setHeader("origin", "http://localhost:2375"); // this can be anything - but it has to be there....
			client.connect(listener,uri,req);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Failed to connect to web socket",e);
		} catch (IOException e) {
			throw new RuntimeException("Failed to connect to web socket",e);
		}
	}
	
	/* For a while it looked like there was a race condition in attaching which required a retry - can't reproduce now
	@Override
	public void attach(final String containerId, 
			final boolean logs, 
			final boolean stream, 
			final boolean stdout, 
			final boolean stderr, 
			final boolean stdin,
			final WebSocketListener listener) {
		attachDirectListener(containerId, logs, stream, stdout, stderr, stdin, 
				new WebSocketListener() {
			boolean waitingForClose = false;
			
			@Override
			public void onWebSocketError(Throwable cause) {
				listener.onWebSocketError(cause);
			}
			
			@Override
			public void onWebSocketConnect(Session session) {
				listener.onWebSocketConnect(session);
			}
			
			@Override
			public void onWebSocketClose(int statusCode, String reason) {
				if (waitingForClose) {
					try {
						System.out.println("Retry");
						attachDirectListener(containerId, logs, stream, stdout, stderr, stdin, listener);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} 
				else {
					listener.onWebSocketClose(statusCode, reason);
				}
			}
			
			@Override
			public void onWebSocketText(String message) {
				if (message.startsWith("no such id: "+containerId)) {
					System.out.println("No ID error");
					waitingForClose = true;
				}
				else {
					listener.onWebSocketText(message);
				}
			}
			
			@Override
			public void onWebSocketBinary(byte[] payload, int offset, int len) {
				listener.onWebSocketBinary(payload, offset, len);
			}	
			
		});
	}
	*/
}