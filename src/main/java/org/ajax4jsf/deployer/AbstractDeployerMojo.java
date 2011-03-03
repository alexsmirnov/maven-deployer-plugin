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
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.authentication.AuthenticationInfo;

/**
 * Created by IntelliJ IDEA. User: jeffgenender Date: Oct 1, 2005 Time: 1:36:05
 * PM To change this template use File | Settings | File Templates.
 */
public abstract class AbstractDeployerMojo extends AbstractMojo {

	/**
	 * The default username to use when authenticating with Tomcat manager.
	 * @parameter expression="${user}" default-value="admin"
	 */
	protected String user = "admin";

	/**
	 * The default password to use when authenticating with Tomcat manager.
	 * @parameter expression="${password}" default-value=""
	 */
	protected String password = "";

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
	protected String toAuthorization() throws MojoExecutionException {
		String userName;
		String password;
	
		if (targetServer == null) {
			// no targetServer set, use defaults
			getLog()
			        .info(
			                "No targetServer specified for authentication - using defaults");
			userName = user;
			password = this.password;
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
				userName = user;
			}
	
			// derive password
			password = info.getPassword();
			if (password == null) {
				getLog().info(
				        "No targetServer password specified - using default");
				password = this.password;
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

}
