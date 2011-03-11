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
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.binary.Base64;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.authentication.AuthenticationInfo;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;

/**
 * Created by IntelliJ IDEA. User: jeffgenender Date: Oct 1, 2005 Time: 1:36:05
 * PM To change this template use File | Settings | File Templates.
 */
public abstract class AbstractDeployerMojo extends AbstractMojo {

	private static final String ADMIN = "admin";

	/**
	 * The default username to use when authenticating with Tomcat manager.
	 * @parameter expression="${user}"
	 */
	protected String username;

	/**
	 * The default password to use when authenticating with Tomcat manager.
	 * @parameter expression="${password}" default-value=""
	 */
	protected String password;

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
	 * The host jboss is running on
	 * 
	 * @parameter expression="${targetHost}" default-value="localhost"
	 * @required
	 */
	protected String targetHost;

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
	protected WagonManager wagonManager;

	/**
	 * @parameter expression ="${secure}" default-value="false"
	 * 
	 */
	private boolean secure = false;
	/**
	 * The targetServer id to use when authenticating with Tomcat manager, or
	 * <code>null</code> to use defaults.
	 * 
	 * @parameter
	 */
	protected String targetServer;

	
	/**
	 * @parameter expression="${deploymentFile}"
	 */
	protected File deploymentFile;

	/**
	 * Jersey REST client
	 */
	private  Client client;
	public void execute() throws MojoExecutionException, MojoFailureException {
		init();
		performCommand(getDeploymentFile());
	}

	protected abstract void performCommand(File file) throws MojoExecutionException;

	private void init() throws MojoExecutionException {
		SSLContext ctx = createSSLContext();
		ClientConfig config=new DefaultClientConfig();
		config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(getHostNameVerifier(),ctx));
//		config.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS,Boolean.TRUE);
		client = Client.create(config);
//		client.setFollowRedirects(true);
		client.addFilter(getAuthorization());
	}

	private HostnameVerifier getHostNameVerifier() {
		return new HostnameVerifier() {
			
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
	}

	protected void doPostRequest(Map<String, String> parameters) throws MojoExecutionException {
		final MultivaluedMapImpl mapImpl = prepareParameters(parameters);
		doPost(new RequestInvoker() {
			
			public ClientResponse perform(Builder resource) {
				return resource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, mapImpl);
			}
		});
	}

	protected void doDeleteRequest(final Map<String, String> parameters) throws MojoExecutionException {
		final MultivaluedMapImpl mapImpl = prepareParameters(parameters);
		doPost(new RequestInvoker() {
			
			public ClientResponse perform(Builder resource) {
//				for (Map.Entry<String, String> entry : parameters.entrySet()) {
//					resource.
//				}
				return resource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).delete(ClientResponse.class);
			}
		});
	}

	private MultivaluedMapImpl prepareParameters(Map<String, String> parameters) {
		final MultivaluedMapImpl mapImpl = new MultivaluedMapImpl();
		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			mapImpl.add(entry.getKey(), entry.getValue());
		}
		return mapImpl;
	}

	protected void doPostFileRequest(String fileParameter, File file,Map<String, String> parameters) throws MojoExecutionException {
		final FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
		formDataMultiPart.bodyPart(new FileDataBodyPart(fileParameter, file));
		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			formDataMultiPart.field(entry.getKey(), entry.getValue());
		}
		doPost(new RequestInvoker() {
			
			public ClientResponse perform(Builder resource) {
				return resource.type(MediaType.MULTIPART_FORM_DATA_TYPE).post(ClientResponse.class, formDataMultiPart);
			}
		});
	}

	private void doPost(RequestInvoker invoker)
			throws MojoExecutionException {
		try {
			getLog().info("Calling target server ");
			Builder resourceBuilder = getResource().accept(MediaType.APPLICATION_JSON_TYPE,MediaType.TEXT_HTML_TYPE);
			ClientResponse response = invoker.perform(resourceBuilder);
			if(response.getStatus()!=200){
				throw new MojoExecutionException("Error processing request "+response.getClientResponseStatus().toString());
			}
		} catch (Exception e) {
			throw new MojoExecutionException("Mojo error occurred: "
			        + e.getMessage(), e);
		}
	}
	
	private WebResource getResource() throws MalformedURLException, URISyntaxException{
		return client.resource(getTargetURI());
	}

	private URI getTargetURI() throws MalformedURLException, URISyntaxException{
		URI target = new URL(secure?"https":"http",targetHost,getTargetPort(),getServicePath()).toURI();
		return target;
	}
	
	protected abstract String getServicePath();

	protected abstract int getTargetPort();

	protected File getDeploymentFile() throws MojoExecutionException {
		if (null == deploymentFile) {
			MavenProject deploymentProject = null;
			if ("pom".equalsIgnoreCase(packaging)) {
				getLog().debug("Check subprojects for deployable file");
				int highLevel = 0;
				// Look for the ear ( preferred ) subproject
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
			if (!deploymentFile.exists() || deploymentFile.isDirectory()) {
				throw new MojoExecutionException("No file for target artifact:"
				        + deploymentProject.toString());
			}

		}
		return deploymentFile;
	}

	/**
	 * Gets the HTTP Basic Authorization header value for the supplied username
	 * and password.
	 * 
	 * @return the HTTP Basic Authorization header value
	 * @throws MojoExecutionException
	 */
	protected ClientFilter getAuthorization() throws MojoExecutionException {
		ClientFilter authFilter;
	
		if( null != this.username){
			authFilter = new HTTPBasicAuthFilter(this.username,this.password);
		} else if (targetServer != null) {
			authFilter = getServerAuthentication(targetServer);
		} else if (targetHost != null) {
			authFilter = getServerAuthentication(targetHost);
		} else {
			// no targetServer set, use defaults
			getLog()
			        .info(
			                "No targetServer specified for authentication - using defaults");
			authFilter = new HTTPBasicAuthFilter(ADMIN,this.password);
		}
		return authFilter;
	}

	private ClientFilter getServerAuthentication(String serverId) throws MojoExecutionException {
		ClientFilter authFilter;
		// obtain authenication details for specified targetServer from
		// wagon
		String userName;
		String password;
		AuthenticationInfo info = wagonManager
		        .getAuthenticationInfo(serverId);
		if (info == null) {
			throw new MojoExecutionException(
			        "Server not defined in settings.xml: " + targetServer);
		}

		// derive username
		userName = info.getUserName();
		if (userName == null) {
			getLog().info(
			        "No targetServer username specified - using default");
			userName = ADMIN;
		}

		// derive password
		password = info.getPassword();
		if (password == null) {
			getLog().info(
			        "No targetServer password specified - using default");
			password = this.password;
		}
		authFilter = new HTTPBasicAuthFilter(userName, password);
		return authFilter;
	}

	public static SSLContext createSSLContext() throws MojoExecutionException {
		        // Create a trust manager that does not validate certificate chains
		        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
		            public X509Certificate[] getAcceptedIssuers() {
		                return new X509Certificate[]{};
		            }

		            public void checkClientTrusted(X509Certificate[] certs, String authType) {
		                return;
		            }

		            public void checkServerTrusted(X509Certificate[] certs, String authType) {
		                return;
		            }
		        }};

		        // Install the all-trusting trust manager
		        try {
		            SSLContext sc = SSLContext.getInstance("SSL");
		            sc.init(null, trustAllCerts, new SecureRandom());
		            return sc;
		        } catch (Exception e) {
		        	throw new MojoExecutionException("Error on SSL initialization", e);
		        }
		    }

}
