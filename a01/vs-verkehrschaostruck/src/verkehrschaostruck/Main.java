package verkehrschaostruck;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import verkehrschaos.Truck;
import verkehrschaos.TruckCompany;
import verkehrschaos.TruckCompanyHelper;
import verkehrschaos.TruckHelper;

public class Main{

    public static void main(String[] args){

        String name = args[0];
        String truckCompanyName = args[1];

        try{
            System.out.println("Truck gestarte mit Name:" + name + ", TruckCompany:" + truckCompanyName);

            ORB orb = ORB.init(args, null);
            POA rootPoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootPoa.the_POAManager().activate();

            CountDownLatch cdl = new CountDownLatch(1);

            TruckImpl truck = new TruckImpl(name, cdl);
            org.omg.CORBA.Object ref = rootPoa.servant_to_reference(truck);
            Truck href = TruckHelper.narrow(ref);
            truck.setCorbaTruck(href);

            NamingContextExt ns = NamingContextExtHelper.narrow(orb.resolve_initial_references("NameService"));

            NameComponent path[] = ns.to_name(name);
            ns.rebind(path, href);

            TruckCompany company = TruckCompanyHelper.narrow(ns.resolve_str(truckCompanyName));
            truck.setCompany(company);
            company.addTruck(href);

            System.out.println("neuer Truck(" + name + ") ist bei der Spedition " + truckCompanyName + " angemeldet");

            cdl.await();
            orb.shutdown(true);
            Thread.sleep(500);

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
        }

    }

}
