package verkehrschaostruckcompany;

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

import verkehrschaos.ELocationInUse;
import verkehrschaos.ELocationNotFound;
import verkehrschaos.Streets;
import verkehrschaos.StreetsHelper;
import verkehrschaos.TruckCompany;
import verkehrschaos.TruckCompanyHelper;

public class Main{

    public static void main(String[] args){

        String name = args[0];
        String verkehrsChaosName = args[1];
        String location = args[2];

        try{
            System.out.println("TruckCompany gestarte mit Name:" + name + ", ChaosName:" + verkehrsChaosName + ", Location:" + location);
            ORB orb = ORB.init(args, null);
            POA rootPoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootPoa.the_POAManager().activate();

            CountDownLatch cdl = new CountDownLatch(1);

            TruckCompanyImpl truckCompany = new TruckCompanyImpl(name, cdl);
            org.omg.CORBA.Object ref = rootPoa.servant_to_reference(truckCompany);
            TruckCompany href = TruckCompanyHelper.narrow(ref);

            truckCompany.setCorbaTruckComapny(href);

            NamingContextExt ns = NamingContextExtHelper.narrow(orb.resolve_initial_references("NameService"));

            NameComponent path[] = ns.to_name(name);
            ns.rebind(path, href);

            Streets streets = StreetsHelper.narrow(ns.resolve_str(verkehrsChaosName));
            streets.claim(href, location);

            System.out.println("Spedition(" + name + ") steht hier: " + location);

            cdl.await();
            streets.free(location);
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
        } catch(ELocationNotFound e){
            e.printStackTrace();
        } catch(ELocationInUse e){
            e.printStackTrace();
        } catch(InterruptedException e){
            e.printStackTrace();
        }

    }

}
