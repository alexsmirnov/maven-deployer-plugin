/**
 * 
 */
package org.ajax4jsf.deployer;

import java.io.File;
import java.util.Map;

/**
 * @author asmirnov
 * @goal redeploy-glassfish
 * @aggregator true
 * 
 */
public class ReDeployGlassfishMojo extends DeployGlassfishMojo {

	@Override
	protected Map<String, String> getParameters(File file) {
		Map<String, String> parameters = super.getParameters(file);
		parameters.put("force", "true");
		return parameters;
	}
}
