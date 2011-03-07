package org.ajax4jsf.deployer;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource.Builder;

public interface RequestInvoker {
	
	ClientResponse perform(Builder resource);

}
