/**
 * 
 */
package org.ajax4jsf.deployer;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @author asmirnov
 * 
 */
public abstract class AbstractJbossDeployer extends AbstractDeployerMojo {

	/**
	 * The targetPort jboss is running on
	 * 
	 * @parameter expression="${targetPort}" default-value="8080"
	 */
	protected int targetPort;
	/**
	 * The port local server is running on
	 * 
	 * @parameter expression="${localPort}" default-value="9999"
	 * @required
	 */
	protected int localPort;
	/**
	 * The host local server is running on
	 * 
	 * @parameter expression="${localHost}"
	 */
	protected String localHost;
	/**
	 * @parameter expression ="${http}" default-value="true"
	 * 
	 */
	private boolean remote = true;

	/**
	 * @param fixedFile
	 * @return
	 * @throws MojoExecutionException
	 */
	protected String getDeploymentURL(File deploymentFile)
			throws MojoExecutionException {
		URL url;
		try {
			if (remote) {
				url = new URL("http", getLocalHost(), localPort, "/"
						+ deploymentFile.getName());
			} else {
				url = deploymentFile.getAbsoluteFile().toURI().toURL();
			}
		} catch (MalformedURLException e) {
			throw new MojoExecutionException("Error creating deployment url", e);
		}
		return url.toString();
		// "http://" + targetHostName + ":" + targetPort + getUrl()
		// + "http://" + getLocalHostName() + ":" + localPort + "/"
		// + getDeployFileName();
	}

	protected String getLocalHost() throws MojoExecutionException {
		if (null == localHost) {
			// Detect public interface.
			try {
				// Deployment to local host from the local address
				if (InetAddress.getByName(targetHost).isLoopbackAddress()) {
					localHost = InetAddress.getLocalHost().getHostAddress();
				} else {
					Enumeration<NetworkInterface> interfaces = NetworkInterface
							.getNetworkInterfaces();
					List<InetAddress> publicAddresses = new ArrayList<InetAddress>();
					while (interfaces.hasMoreElements()) {
						NetworkInterface networkInterface = (NetworkInterface) interfaces
								.nextElement();
						Enumeration<InetAddress> addresses = networkInterface
								.getInetAddresses();
						while (addresses.hasMoreElements()) {
							InetAddress inetAddress = (InetAddress) addresses
									.nextElement();
							if (inetAddress instanceof Inet4Address
									&& !inetAddress.isLoopbackAddress()
									&& inetAddress.isReachable(10)) {
								publicAddresses.add(inetAddress);
							}
						}
					}
					if (publicAddresses.size() == 0) {
						throw new MojoExecutionException(
								"No public addresses detected");
					} else if (publicAddresses.size() == 1) {
						localHost = publicAddresses.get(0).getHostAddress();
					} else {
						// Look for direct interface for target host.
						// Non-local address is preffered.
						for (InetAddress inetAddress : publicAddresses) {
							if (!inetAddress.isSiteLocalAddress()) {
								localHost = inetAddress.getHostAddress();
							}
						}
						// If all addresses are local, return first.
						if (null == localHost) {
							localHost = publicAddresses.get(0).getHostAddress();

						}
					}
				}
			} catch (Exception e) {
				throw new MojoExecutionException("Error detect hostname", e);
			}
		}
		getLog().debug("local server host name: " + localHost);
		return localHost;
	}

	public void performCommand() throws MojoExecutionException {

		// Fix the ejb packaging to a jar
		final File fileToSend = getDeploymentFile();

		final String requestUrl = fileToSend.getName();
		getLog().info("Deploying " + requestUrl + " to Server.");
		try {
			Map<String, String> parameters = getParameters(fileToSend);
			if (isRequireLocalServer()) {
				NanoHTTPD httpd = new DeployerHTTPD(localPort, fileToSend,
						getLog());
				httpd.start();
				doPostRequest(parameters);
				Thread.sleep(100L);
				httpd.stop();
			} else {
				doPostRequest(parameters);
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Local targetPort in use", e);
		} catch (InterruptedException e) {
			throw new MojoExecutionException("Send file interrupted", e);
		}
	}
	
	@Override
	protected String getServicePath() {
		return "/jmx-console/HtmlAdaptor";
	}

	@Override
	protected int getTargetPort() {
		return targetPort;
	}
	private Map<String, String> getParameters(File file) throws MojoExecutionException {
		HashMap<String, String> parametersMap = new HashMap<String, String>(5);
		// action=invokeOpByName&name=jboss.system:service%3DMainDeployer&methodName=deploy&argType=java.lang.String&arg0=
		parametersMap.put("action", "invokeOpByName");
		parametersMap.put("name", "jboss.system:service=MainDeployer");
		parametersMap.put("methodName", getMethodName());
		parametersMap.put("argType", String.class.getName());
		parametersMap.put("arg0",getDeploymentURL(file));
		return parametersMap;
	}

	protected abstract String getMethodName();

	public boolean isRequireLocalServer() {
		return remote;
	}

}
