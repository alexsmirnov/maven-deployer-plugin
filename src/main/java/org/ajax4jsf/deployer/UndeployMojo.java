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

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Undeploys a directory or file from JBoss via JMX
 * 
 * @author <a href="mailto:jgenender@apache.org">Jeff Genender</a>
 * @goal undeploy
 * @aggregator true
 * @description Maven 2 JBoss plugin
 */
public class UndeployMojo extends AbstractDeployerMojo {

	private static final String DEFAULT_PATH = "/jmx-console/HtmlAdaptor?action=invokeOpByName&name=jboss.system:service%3DMainDeployer&methodName=undeploy&argType=java.net.URL&arg0=";
	/**
	 * The undeployment URL
	 * 
	 * @parameter expression="${undeployUrlPath}"
	 */
	protected String undeployUrlPath;

	public void execute() throws MojoExecutionException {

		calculateDeploymentFile();
		// Fix the ejb packaging to a jar
		String fixedFile = null;
		fixedFile = getDeployFileName();

		getLog().info("Undeploying " + fixedFile + " from JBoss.");
		String url = getDeploymentURL();
		doURL(url);
	}

	protected String getUrl() {
		if (null != undeployUrlPath) {
			return undeployUrlPath;

		} else {
			return DEFAULT_PATH;
		}
	}
}
