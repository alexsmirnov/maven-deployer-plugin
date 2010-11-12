/**
 * 
 */
package org.ajax4jsf.deployer;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * @author asmirnov
 * 
 */
class Information {
	public static void main(String[] aa)

	{
		try {

			String hostName = InetAddress.getLocalHost().getHostName();
			System.out.println(hostName);
			System.out.println(InetAddress.getLocalHost().getHostAddress());
			InetAddress[] allByName = InetAddress.getAllByName(hostName);
			for (InetAddress inetAddress : allByName) {
				printAddress(inetAddress);
			}
			printAddress(InetAddress.getByAddress(new byte[]{10,(byte) 206,(byte) 169,74}));
			Enumeration<NetworkInterface> interfaces = NetworkInterface
					.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = (NetworkInterface) interfaces
						.nextElement();
				System.out.println("Interface: "+networkInterface.getDisplayName());
				Enumeration<InetAddress> addresses = networkInterface
						.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress inetAddress = (InetAddress) addresses
							.nextElement();
						printAddress(inetAddress);
				}
			}

		} catch (Exception e) {
		}

	}

	private static void printAddress(InetAddress inetAddress)
			throws IOException {
		System.out.println("address:"+inetAddress.getHostAddress());
		System.out.println("local:"+inetAddress.isSiteLocalAddress());
		System.out.println("loopback:"+inetAddress.isLoopbackAddress());
		System.out.println("link local:"+inetAddress.isLinkLocalAddress());
		System.out.println("multicast:"+inetAddress.isMulticastAddress());
		System.out.println("reachable:"+inetAddress.isReachable(10));
		System.out.println("inet4:"+(inetAddress instanceof Inet4Address));
	}

}
