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
 * @goal deploy-glassfish
 * @aggregator true
 *
 */
public class DeployGlassfishMojo extends AbstractGlassfishDeployer {

	@Override
	protected String getServicePath() {
		return "/management/domain/applications/application/";
	}

	@Override
	protected void performCommand(File file) throws MojoExecutionException {
		Map<String, String> parameters = getParameters(file);
		doPostFileRequest("id", file, parameters);
	}

	protected Map<String, String> getParameters(File file) {
		String name = getApplicationName(file);
		String contextRoot = getContextRoot(file);
		HashMap<String, String> params = new HashMap<String, String>(4);
		params.put("name", name);
		params.put("contextroot", contextRoot);
		return params;
	}

}
