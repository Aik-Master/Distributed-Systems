package starter;

import gcd.Coordinator.*;
import gcd.Starter.*;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

	public static void main(String[] args) {

		Properties props = new Properties();
		props.put("org.omg.CORBA.ORBInitialPort", args[0]);
		props.put("org.omg.CORBA.ORBInitialHost", args[1]);

		String id = args[2];
		String coordinatorName = args[3];

		try{
			System.out.println("starting starter - name: " + id + ", coordinator: " + coordinatorName + " ...");

			ORB orb = ORB.init(args, props);
			POA rootPoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
			rootPoa.the_POAManager().activate();

			CountDownLatch cdl = new CountDownLatch(1);

			NamingContextExt ns = NamingContextExtHelper.narrow(orb.resolve_initial_references("NameService"));

			Coordinator coordinator = CoordinatorHelper.narrow(ns.resolve_str(coordinatorName));

			StarterImpl starterImpl = new StarterImpl(id, coordinator, orb, rootPoa, cdl);
			org.omg.CORBA.Object ref = rootPoa.servant_to_reference(starterImpl);
			Starter starter = StarterHelper.narrow(ref);
			starterImpl.setStarter(starter);

			NameComponent path[] = ns.to_name(id);
			ns.rebind(path, starter);

			coordinator.registerStarter(starter);

			System.out.println("starter(" + id + ") is registered to coordinator(" + coordinatorName + ")");

			cdl.await();
			Thread.sleep(500);
			orb.shutdown(true);

			System.out.println("...starter(" + id + ") says GOOD-BYE!");

		} catch(InvalidName e){
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
		} catch(AdapterInactive e){
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
		} catch(ServantNotActive e){
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
		} catch(WrongPolicy e){
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
		} catch(org.omg.CosNaming.NamingContextPackage.InvalidName e){
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
		} catch(NotFound e){
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
		} catch(CannotProceed e){
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
		} catch(InterruptedException e){
			e.printStackTrace();
		} catch(NullPointerException e){
			e.printStackTrace();
		}
	}
}
