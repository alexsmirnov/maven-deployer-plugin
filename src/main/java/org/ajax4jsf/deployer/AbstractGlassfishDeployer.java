/**
 * 
 */
package org.ajax4jsf.deployer;

import java.io.File;
import java.io.IOException;


/**
 * @author asmirnov
 * 
 */
public abstract class AbstractGlassfishDeployer extends AbstractDeployerMojo {

	/**
	 * The targetPort Glassfish admin is running on
	 * 
	 * @parameter expression="${targetPort}" default-value="4848"
	 */
	protected int targetPort;
	
	/**
	 * 
	 * @parameter expression="${name}"
	 */
	protected String name;
	
	/**
	 * 
	 * @parameter expression="${contextRoot}"
	 */
	protected String contextRoot;

	public int getTargetPort() {
		return targetPort;
	}

	protected String getApplicationName(File file) {
		String fileName = file.getName();
		fileName = fileName.substring(0, fileName.indexOf("."));
		String name = null != this.name ? this.name : fileName;
		return name;
	}
	
	protected String getContextRoot(File file) {
		String fileName = file.getName();
		fileName = fileName.substring(0, fileName.indexOf("."));
		String contextRoot = null != this.contextRoot ? this.contextRoot : fileName;
		return contextRoot;
	}
}
