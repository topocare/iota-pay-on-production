package de.topocare.topocareXiota;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.topocare.topocareXiota.iotaMachineWallet.IotaMachineWalletCore;
import de.topocare.topocareXiota.iotaMachineWallet.PropertiesImport;
import de.topocare.topocareXiota.iotaMachineWallet.tangleMonitoring.IotaLoopTask;
import de.topocare.topocareXiota.ros.RosAdapter;

public class Main {

	/**
	 * @author Stefan Kuenne [info@topocare.de]
	 */
	public static void main(String[] args) {
				
		//import settings
		System.out.println("reading properties-files");
		PropertiesImport propertiesImport = new PropertiesImport();
		propertiesImport.importWallet();
		propertiesImport.importRos();
		
		// define IOTA-wallet
		System.out.println("starting wallet-core");
		IotaMachineWalletCore wallet = new IotaMachineWalletCore(propertiesImport.receivingAddress_first, propertiesImport.receivingAddress_last, propertiesImport.searchKeyIndexFirst, propertiesImport.searchKeyIndexLast, propertiesImport.outputAddress, propertiesImport.returnAddress);
		

		// define RosAdapter connecting the machine
		System.out.println("starting RosAdapter");
		RosAdapter ros = new RosAdapter(propertiesImport.rosHost);

		// define the Mediator. connecting wallet and machine
		System.out.println("starting Mediator");
		Mediator mediator = new Mediator(wallet, ros);

		// defining the repeating wallet task (should be included in wallet later)
		System.out.println("initiating IotaLoopTask");
		IotaLoopTask iotaLoop = new IotaLoopTask(wallet);

		// define the display-task for console-output
		System.out.println("initiating DisplayTask");
		DisplayTask display = new DisplayTask(wallet, mediator);

		// run the tasks
		ScheduledExecutorService exec1 = Executors.newSingleThreadScheduledExecutor();
		ScheduledExecutorService exec2 = Executors.newSingleThreadScheduledExecutor();

		System.out.println("starting IotaLoopTask");
		exec1.scheduleAtFixedRate(iotaLoop, 0, 20, TimeUnit.SECONDS);
		System.out.println("starting DisplayTask");
		exec2.scheduleAtFixedRate(display, 0, 5, TimeUnit.SECONDS);

	}



}
