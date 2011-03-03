/**
 * 
 */
package org.ajax4jsf.deployer;

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


}
