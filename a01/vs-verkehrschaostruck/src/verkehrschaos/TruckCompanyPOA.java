package verkehrschaos;

/**
 * verkehrschaos/TruckCompanyPOA.java . Generated by the IDL-to-Java compiler (portable), version "3.2" from verkehrschaos.idl Samstag, 4. April 2015 19:40 Uhr MESZ
 */

public abstract class TruckCompanyPOA extends org.omg.PortableServer.Servant implements verkehrschaos.TruckCompanyOperations,
        org.omg.CORBA.portable.InvokeHandler{

    // Constructors

    private static java.util.Hashtable _methods = new java.util.Hashtable();
    static{
        _methods.put("getName", new java.lang.Integer(0));
        _methods.put("addTruck", new java.lang.Integer(1));
        _methods.put("removeTruck", new java.lang.Integer(2));
        _methods.put("getTrucks", new java.lang.Integer(3));
        _methods.put("leave", new java.lang.Integer(4));
        _methods.put("advise", new java.lang.Integer(5));
        _methods.put("arrive", new java.lang.Integer(6));
        _methods.put("putOutOfService", new java.lang.Integer(7));
    }

    public org.omg.CORBA.portable.OutputStream _invoke(String $method, org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler $rh){
        org.omg.CORBA.portable.OutputStream out = null;
        java.lang.Integer __method = (java.lang.Integer) _methods.get($method);
        if(__method == null)
            throw new org.omg.CORBA.BAD_OPERATION(0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);

        switch(__method.intValue()){

        /* Gibt den Namen der Spedition. */
            case 0: // verkehrschaos/TruckCompany/getName
            {
                String $result = null;
                $result = this.getName();
                out = $rh.createReply();
                out.write_string($result);
                break;
            }

            /*
             * Fuegt der Spedition einen LKW hinzu. Damit ist die Spedition die dem LKW zugeordnete Spedition: */
            case 1: // verkehrschaos/TruckCompany/addTruck
            {
                verkehrschaos.Truck truck = verkehrschaos.TruckHelper.read(in);
                this.addTruck(truck);
                out = $rh.createReply();
                break;
            }

            /*
             * Entfernt den LKW von der Spedition */
            case 2: // verkehrschaos/TruckCompany/removeTruck
            {
                verkehrschaos.Truck truck = verkehrschaos.TruckHelper.read(in);
                this.removeTruck(truck);
                out = $rh.createReply();
                break;
            }

            /*
             * Liefert eine Liste aller verfuegbaren LKWs. LKWs, die unterwegs sind, sollen auch in der Liste enthalten sein. Rueckgabewert soll die Anzahl der LKWs enthalten. */
            case 3: // verkehrschaos/TruckCompany/getTrucks
            {
                verkehrschaos.TTruckListHolder trucks = new verkehrschaos.TTruckListHolder();
                int $result = (int) 0;
                $result = this.getTrucks(trucks);
                out = $rh.createReply();
                out.write_long($result);
                verkehrschaos.TTruckListHelper.write(out, trucks.value);
                break;
            }

            /*
             * LKW hat Spedition verlassen. Spedition ist nicht mehr fuer den Laster zustaendig, muss aus Liste der Laster entfernt werden. Wird von Streets aufgerufen */
            case 4: // verkehrschaos/TruckCompany/leave
            {
                verkehrschaos.Truck truck = verkehrschaos.TruckHelper.read(in);
                this.leave(truck);
                out = $rh.createReply();
                break;
            }

            /*
             * LKW wird angekuendigt. Eine andere Spedition hat einen Laster auf den Weg zu dieser Spedition gebracht. Spedition soll den LKW sofort durch Aufruf von Truck.setCompany uebernehmen. Wird von Streets aufgerufen */
            case 5: // verkehrschaos/TruckCompany/advise
            {
                verkehrschaos.Truck truck = verkehrschaos.TruckHelper.read(in);
                this.advise(truck);
                out = $rh.createReply();
                break;
            }

            /*
             * LKW ist im Ziel angekommen. Steht nun fuer neue Fahrten zur Verfuegung. Wird von Streets aufgerufen */
            case 6: // verkehrschaos/TruckCompany/arrive
            {
                verkehrschaos.Truck truck = verkehrschaos.TruckHelper.read(in);
                this.arrive(truck);
                out = $rh.createReply();
                break;
            }

            /* Stilllegung der Spedition (TruckCompany Anwendung wird beendet). Wird von der Steueranwendung (Client) aufgerufen. Legt auch alle zugeordneten LKWs still. Beenden der Anwendung durch Aufruf von orb.shutdown(true). Nach orb.shutdown kleine Pause einlegen (0.5 sec) um Exception zu vermeiden. */
            case 7: // verkehrschaos/TruckCompany/putOutOfService
            {
                this.putOutOfService();
                out = $rh.createReply();
                break;
            }

            default:
                throw new org.omg.CORBA.BAD_OPERATION(0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);
        }

        return out;
    } // _invoke

    // Type-specific CORBA::Object operations
    private static String[] __ids = {"IDL:verkehrschaos/TruckCompany:1.0"};

    public String[] _all_interfaces(org.omg.PortableServer.POA poa, byte[] objectId){
        return (String[]) __ids.clone();
    }

    public TruckCompany _this(){
        return TruckCompanyHelper.narrow(super._this_object());
    }

    public TruckCompany _this(org.omg.CORBA.ORB orb){
        return TruckCompanyHelper.narrow(super._this_object(orb));
    }

} // class TruckCompanyPOA
