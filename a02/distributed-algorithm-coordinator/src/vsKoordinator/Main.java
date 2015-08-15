package vsKoordinator;

import gcd.Coordinator.Coordinator;
import gcd.Coordinator.CoordinatorHelper;
import monitor.Monitor;
import monitor.MonitorHelper;

import java.util.Properties;

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

public class Main{

    public static void main(String[] args){

        Properties props = new Properties();

        props.put("org.omg.CORBA.ORBInitialPort", args[0]);
        props.put("org.omg.CORBA.ORBInitialHost", args[1]);

        String name = args[2];
        String monitorName = args[3];

        ORB orb = ORB.init(args, props);

        try{

            POA rootPoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootPoa.the_POAManager().activate();

            NamingContextExt ns = NamingContextExtHelper.narrow(orb.resolve_initial_references("NameService"));

            Monitor monitor = MonitorHelper.narrow(ns.resolve_str(monitorName));

            CoordinatorImpl koordiantor = new CoordinatorImpl(name, monitor, orb); // todo: replace null with monitor
            org.omg.CORBA.Object ref = rootPoa.servant_to_reference(koordiantor);
            Coordinator href = CoordinatorHelper.narrow(ref);

            NameComponent path[] = ns.to_name(name);
            ns.rebind(path, href);

            System.out.println("Koordinator laeuft...");

            orb.run();

        } catch(InvalidName e){
            e.printStackTrace();
        } catch(AdapterInactive e){
            e.printStackTrace();
        } catch(ServantNotActive e){
            e.printStackTrace();
        } catch(WrongPolicy e){
            e.printStackTrace();
        } catch(org.omg.CosNaming.NamingContextPackage.InvalidName e){
            e.printStackTrace();
        } catch(NotFound e){
            e.printStackTrace();
        } catch(CannotProceed e){
            e.printStackTrace();
        }

    }

}
