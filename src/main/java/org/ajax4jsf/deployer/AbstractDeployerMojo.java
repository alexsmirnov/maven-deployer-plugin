/*
 * Copyright 2005 Jeff Genender.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ajax4jsf.deployer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.authentication.AuthenticationInfo;

/**
 * Created by IntelliJ IDEA. User: jeffgenender Date: Oct 1, 2005 Time: 1:36:05
 * PM To change this template use File | Settings | File Templates.
 */
public abstract class AbstractDeployerMojo extends AbstractMojo {

	public static final class DeployerHTTPD extends NanoHTTPD {

		private static final String MIME_JAR = "application/java-archive";

		private boolean serviced = true;

		public boolean isServiced() {
			return serviced;
		}

		private final String url;

		private final File send;

		private Log log;

		public DeployerHTTPD(int port, String url, File send, Log log)
		        throws IOException {
			super(port);
			this.url = url;
			this.send = send;
			this.log = log;
		}

		public Response serve(String uri, String method, Properties header,
		        Properties parms) {
			getLog().info(
			        "Request from server to URL " + uri + " by Method "
			                + method);
			Response response;
			if (url.equals(uri.trim())) {
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

	/**
	 * The default username to use when authenticating with Tomcat manager.
	 */
	private static final String DEFAULT_USERNAME = "admin";

	/**
	 * The default password to use when authenticating with Tomcat manager.
	 */
	private static final String DEFAULT_PASSWORD = "";

	/**
	 * The project whose project files to create.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	protected MavenProject project;

	/**
	 * The currently executed project (can be a reactor project).
	 * 
	 * @parameter expression="${executedProject}"
	 * @readonly
	 */
	protected MavenProject executedProject;

	/**
	 * If the executed project is a reactor project, this will contains the full
	 * list of projects in the reactor.
	 * 
	 * @parameter expression="${reactorProjects}"
	 * @readonly
	 */
	protected List<MavenProject> reactorProjects;

	/**
	 * The project packaging.
	 * 
	 * @parameter expression="${project.packaging}"
	 * @readonly
	 */
	protected String packaging;
	/**
	 * The targetPort jboss is running on
	 * 
	 * @parameter expression="${targetPort}" default-value="8080"
	 */
	protected int targetPort;

	/**
	 * The host jboss is running on
	 * 
	 * @parameter expression="${targetHostName}" default-value="localhost"
	 * @required
	 */
	protected String targetHostName;

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
	 * @parameter expression="${localHostName}"
	 */
	protected String localHostName;

	/**
	 * The name of the file or directory to deploy or undeploy.
	 * 
	 * @parameter expression="${deployFileName}"
	 */
	protected String deployFileName;

	/**
	 * The name of the file or directory to deploy or undeploy.
	 * 
	 * @parameter expression="${buildDirectory}"
	 *            default-value="${project.build.directory}"
	 * @required
	 */
	protected File buildDirectory;
	/**
	 * The Maven Wagon manager to use when obtaining targetServer authentication
	 * details.
	 * 
	 * @parameter expression =
	 *            "${component.org.apache.maven.artifact.manager.WagonManager}"
	 * @required
	 * @readonly
	 */
	private WagonManager wagonManager;

	/**
	 * @parameter expression ="${http}" default-value="true"
	 * 
	 */
	private boolean remote = true;
	/**
	 * @parameter expression ="${secure}" default-value="true"
	 * 
	 */
	private boolean secure = false;
	/**
	 * The targetServer id to use when authenticating with Tomcat manager, or
	 * <code>null</code> to use defaults.
	 * 
	 * @parameter
	 */
	private String targetServer;

	private File deploymentFile;

	protected void doURL(String url) throws MojoExecutionException {
		try {

			getLog().info("Calling url " + url);

			HttpURLConnection connection = (HttpURLConnection) new URL(url)
			        .openConnection();
			connection.setInstanceFollowRedirects(false);
			connection.setRequestProperty("Authorization", toAuthorization());

			BufferedReader reader = new BufferedReader(new InputStreamReader(
			        connection.getInputStream()));
			reader.readLine();
			reader.close();
		} catch (Exception e) {
			throw new MojoExecutionException("Mojo error occurred: "
			        + e.getMessage(), e);
		}
	}

	/**
	 * Gets the HTTP Basic Authorization header value for the supplied username
	 * and password.
	 * 
	 * @return the HTTP Basic Authorization header value
	 * @throws MojoExecutionException
	 */
	private String toAuthorization() throws MojoExecutionException {
		String userName;
		String password;

		if (targetServer == null) {
			// no targetServer set, use defaults
			getLog()
			        .info(
			                "No targetServer specified for authentication - using defaults");
			userName = DEFAULT_USERNAME;
			password = DEFAULT_PASSWORD;
		} else {
			// obtain authenication details for specified targetServer from
			// wagon
			AuthenticationInfo info = wagonManager
			        .getAuthenticationInfo(targetServer);
			if (info == null) {
				throw new MojoExecutionException(
				        "Server not defined in settings.xml: " + targetServer);
			}

			// derive username
			userName = info.getUserName();
			if (userName == null) {
				getLog().info(
				        "No targetServer username specified - using default");
				userName = DEFAULT_USERNAME;
			}

			// derive password
			password = info.getPassword();
			if (password == null) {
				getLog().info(
				        "No targetServer password specified - using default");
				password = DEFAULT_PASSWORD;
			}
		}

		StringBuffer buffer = new StringBuffer();
		buffer.append(userName).append(':');
		if (password != null) {
			buffer.append(password);
		}
		return "Basic "
		        + new String(Base64.encodeBase64(buffer.toString().getBytes()));
	}

	/**
	 * @param fixedFile
	 * @return
	 * @throws MojoExecutionException
	 */
	protected String getDeploymentURL() throws MojoExecutionException {
		StringBuilder url = new StringBuilder();
		url.append("http://").append(targetHostName).append(":").append(targetPort).append(getUrl());
		if(remote){
			url.append("http://").append(getLocalHostName()).append(":").append(localPort).append("/").append(getDeployFileName());
		} else {
			try {
				url.append(deploymentFile.getAbsoluteFile().toURI().toURL().toString());
			} catch (MalformedURLException e) {
				throw new MojoExecutionException("Error creating deployment url",e);
			}
		}
		return url.toString();
//		"http://" + targetHostName + ":" + targetPort + getUrl()
//		        + "http://" + getLocalHostName() + ":" + localPort + "/"
//		        + getDeployFileName();
	}

	protected abstract String getUrl();

	/**
	 * @return
	 * @throws MojoExecutionException
	 */
	protected String getDeployFileName() throws MojoExecutionException {
		return deployFileName;
	}

	protected void calculateDeploymentFile() throws MojoExecutionException {
		if (null == deployFileName) {
			MavenProject deploymentProject = null;
			if ("pom".equalsIgnoreCase(packaging)) {
				getLog().debug("Check subprojects for deployable file");
				int highLevel = 0;
				// Look for the ear ( preffered ) subproject
				for (MavenProject subproject : reactorProjects) {
					if ("ear".equalsIgnoreCase(subproject.getPackaging())) {
						deploymentProject = subproject;
						highLevel = 4;
						getLog().debug(
						        "Found EAR subproject:"
						                + subproject.getArtifactId());
					} else if ("war"
					        .equalsIgnoreCase(subproject.getPackaging())
					        && highLevel <= 3) {
						deploymentProject = subproject;
						highLevel = 3;
						getLog().debug(
						        "Found WAR subproject:"
						                + subproject.getArtifactId());
					} else if ("sar"
					        .equalsIgnoreCase(subproject.getPackaging())
					        && highLevel <= 2) {
						deploymentProject = subproject;
						highLevel = 2;
						getLog().debug(
						        "Found SAR subproject:"
						                + subproject.getArtifactId());
					} else if ("jar"
					        .equalsIgnoreCase(subproject.getPackaging())
					        && highLevel <= 1) {
						deploymentProject = subproject;
						highLevel = 1;
						getLog().debug(
						        "Found jar subproject:"
						                + subproject.getArtifactId());
					}
				}
				if (null == deploymentProject) {
					throw new MojoExecutionException(
					        "No deployable project found");
				}
				buildDirectory = new File(deploymentProject.getBuild()
				        .getDirectory());
			} else {
				deploymentProject = project;
			}
			Artifact artifact = deploymentProject.getArtifact();
			getLog().debug(
			        "Look build file name for artifact: "
			                + artifact.getArtifactId());
			deploymentFile = artifact.getFile();
			if (null == deploymentFile) {
				String fileName = deploymentProject.getBuild().getFinalName();
				if (null != fileName) {
					deploymentFile = new File(buildDirectory, fileName + "."
					        + artifact.getType());
				}
			}
			if (deploymentFile.exists() && !deploymentFile.isDirectory()) {
				deployFileName = deploymentFile.getName();
			} else {
				throw new MojoExecutionException("No file for target artifact:"
				        + deploymentProject.toString());
			}

		}
	}

	protected String getLocalHostName() throws MojoExecutionException {
		if (null == localHostName) {
			// Detect public interface.
			try {
				// Deployment to local host from the local address
				if (InetAddress.getByName(targetHostName).isLoopbackAddress()) {
					localHostName = InetAddress.getLocalHost().getHostAddress();
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
						localHostName = publicAddresses.get(0).getHostAddress();
					} else {
						// Look for direct interface for target host.
						// Non-local address is preffered.
						for (InetAddress inetAddress : publicAddresses) {
							if (!inetAddress.isSiteLocalAddress()) {
								localHostName = inetAddress.getHostAddress();
							}
						}
						// If all addresses are local, return first.
						if (null == localHostName) {
							localHostName = publicAddresses.get(0)
							        .getHostAddress();

						}
					}
				}
			} catch (Exception e) {
				throw new MojoExecutionException("Error detect hostname", e);
			}
		}
		getLog().debug("local server host name: " + localHostName);
		return localHostName;
	}

	public void execute() throws MojoExecutionException {

		// Fix the ejb packaging to a jar
		calculateDeploymentFile();
		String fixedFile = getDeployFileName();

		final String requestUrl = "/" + fixedFile;
		final File fileToSend = getDeployFile();
		if (fileToSend.exists()) {
			getLog().info("Deploying " + fixedFile + " to JBoss.");
			try {
				String url = getDeploymentURL();
				if (remote) {
					NanoHTTPD httpd = new DeployerHTTPD(localPort, requestUrl,
					        fileToSend, getLog());
					httpd.start();
					doURL(url);
					Thread.sleep(100L);
					httpd.stop();
				} else {
					doURL(url);
				}
			} catch (IOException e) {
				throw new MojoExecutionException("Local targetPort in use", e);
			} catch (InterruptedException e) {
				throw new MojoExecutionException("Send file interrupted", e);
			}

		} else {
			getLog().warn("File " + fixedFile + " do not exist!");
		}
	}

	protected File getDeployFile() throws MojoExecutionException {
		return new File(buildDirectory, deployFileName);
	}

}
