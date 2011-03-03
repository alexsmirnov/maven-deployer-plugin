package org.ajax4jsf.deployer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.ajax4jsf.deployer.NanoHTTPD.Response;
import org.apache.maven.plugin.logging.Log;

public final class DeployerHTTPD extends NanoHTTPD {

	private static final String MIME_JAR = "application/java-archive";

	private boolean serviced = true;

	public boolean isServiced() {
		return serviced;
	}

	private final String url;

	private final File send;

	private Log log;

	public DeployerHTTPD(int port, File send, Log log)
	        throws IOException {
		super(port);
		this.url = send.getName();
		this.send = send;
		this.log = log;
	}

	public Response serve(String uri, String method, Properties header,
	        Properties parms) {
		getLog().info(
		        "Request from server to URL " + uri + " by Method "
		                + method);
		Response response;
		if (uri.trim().endsWith(url)) {
			try {
				FileInputStream fis = new FileInputStream(send);
				response = new Response(NanoHTTPD.HTTP_OK, MIME_JAR, fis);

			} catch (FileNotFoundException e) {
				response = new Response(NanoHTTPD.HTTP_NOTFOUND,
				        NanoHTTPD.MIME_PLAINTEXT, "File not found");
			}

		} else {
			response = new Response(NanoHTTPD.HTTP_FORBIDDEN,
			        NanoHTTPD.MIME_PLAINTEXT,
			        "Only project package allowed");

		}
		return response;
	}

	@Override
	public void cleanup(String uri) {
		// if (null != uri && url.equals(uri.trim())) {
		// try {
		// stop();
		// } catch (IOException e) {
		// log("Error server stop" + e.getMessage());
		// }
		// }

	}

	/**
	 * @return the log
	 */
	public Log getLog() {
		return log;
	}

	protected void log(String message) {
		getLog().info(message);
	}

}