/**
 * 
 */
package org.ajax4jsf.deployer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @author asmirnov
 * @goal undeploy-glassfish
 * @aggregator true
 *
 */
public class UndeployGlassfishMojo extends AbstractGlassfishDeployer {
	
	private File deploymentFile;

	@Override
	protected String getServicePath() {
		return "/management/domain/applications/application/"+getApplicationName(deploymentFile);
	}

	@Override
	protected void performCommand(File file) throws MojoExecutionException {
		this.deploymentFile = file;
		Map<String, String> parameters = getParameters(file);
		doDeleteRequest(parameters);
	}

	private Map<String, String> getParameters(File file) {
		HashMap<String, String> params = new HashMap<String, String>(4);
		return params;
	}

}
