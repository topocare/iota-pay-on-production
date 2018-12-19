package de.topocare.topocareXiota.iotaMachineWallet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import jota.IotaAPI;
import jota.pow.pearldiver.PearlDiverLocalPoW;

/**
 * Imports settings from wallet.properties and ros.properties
 * 
 * @author Stefan Kuenne [info@topocare.de]
 *
 */
public class PropertiesImport {
	
	public String outputAddress;
	public String returnAddress;
	
	public int receivingAddress_first, receivingAddress_last, searchKeyIndexFirst, searchKeyIndexLast;
	
	public String rosHost;
	
	public void importWallet() {
		try (InputStream in = getClass().getResourceAsStream("/wallet.properties");) {
			Properties props = new Properties();
			props.load(in);
			
			//IotaConfig
			IotaConfig.seed = props.getProperty("seed");
			IotaConfig.depth = Integer.parseInt(props.getProperty("depth"));
			IotaConfig.maxDepth = Integer.parseInt(props.getProperty("maxDepth"));
			IotaConfig.minWeightMagnitude = Integer.parseInt(props.getProperty("minWeightMagnitude"));
			IotaConfig.security = Integer.parseInt(props.getProperty("security"));
			
			
			String host = props.getProperty("host");
			String port = props.getProperty("port");
			
			IotaConfig.api = new IotaAPI.Builder().protocol("https").host(host).port(port)
					.localPoW(new PearlDiverLocalPoW()).build();
			
			IotaConfig.productionUnitSize = Integer.parseInt(props.getProperty("productionUnitSize"));
			IotaConfig.productionPoolLowerBorder = Integer.parseInt(props.getProperty("productionPoolLowerBorder"));
			IotaConfig.productionPoolUpperBorder = Integer.parseInt(props.getProperty("productionPoolUpperBorder"));
			IotaConfig.promoteOrReattachAfterMinutes = Integer.parseInt(props.getProperty("promoteOrReattachAfterMinutes"));
			
			//for wallet constructor
			outputAddress = props.getProperty("outputAddress");
			returnAddress = props.getProperty("returnAddress");
			receivingAddress_first =Integer.parseInt(props.getProperty("receivingAddress_first"));
			receivingAddress_last =Integer.parseInt(props.getProperty("receivingAddress_last"));
			searchKeyIndexFirst =Integer.parseInt(props.getProperty("searchKeyIndexFirst"));
			searchKeyIndexLast = Integer.parseInt(props.getProperty("searchKeyIndexLast"));
			
		} catch (IOException e) {
			throw new RuntimeException("wallet.properties not found");
		}
	}
	
	public void importRos() {
		try (InputStream in = getClass().getResourceAsStream("/ros.properties");) {
			Properties props = new Properties();
			props.load(in);
			
			rosHost = props.getProperty("host");
			
			
			
		} catch (IOException e) {
			throw new RuntimeException("wallet.properties not found");
		}
	}
}
