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

/**
 * Deploys a directory or file to JBoss via JMX
 * 
 * @author <a href="mailto:jgenender@apache.org">Jeff Genender</a>
 * @goal redeploy-jboss
 * @aggregator true
 * @description Maven 2 JBoss plugin
 */
public class ReDeployJbossMojo extends AbstractJbossDeployer {

	private static final String DEFAULT_PATH = "/jmx-console/HtmlAdaptor?action=invokeOpByName&name=jboss.system:service%3DMainDeployer&methodName=redeploy&argType=java.lang.String&arg0=";
	/**
	 * The deployment URL
	 * 
	 * @parameter expression="${reDeployUrlPath}"
	 */
	protected String reDeployUrlPath;

	protected String getUrl() {
		if (null != reDeployUrlPath) {
			return reDeployUrlPath;

		} else {
			return DEFAULT_PATH;
		}
	}
}
