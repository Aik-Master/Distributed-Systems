package verkehrschaos;

/**
 * verkehrschaos/ELocationNotInUseHolder.java . Generated by the IDL-to-Java compiler (portable), version "3.2" from verkehrschaos.idl Samstag, 4. April 2015 19:41 Uhr MESZ
 */

public final class ELocationNotInUseHolder implements org.omg.CORBA.portable.Streamable{
    public verkehrschaos.ELocationNotInUse value = null;

    public ELocationNotInUseHolder(){
    }

    public ELocationNotInUseHolder(verkehrschaos.ELocationNotInUse initialValue){
        value = initialValue;
    }

    public void _read(org.omg.CORBA.portable.InputStream i){
        value = verkehrschaos.ELocationNotInUseHelper.read(i);
    }

    public void _write(org.omg.CORBA.portable.OutputStream o){
        verkehrschaos.ELocationNotInUseHelper.write(o, value);
    }

    public org.omg.CORBA.TypeCode _type(){
        return verkehrschaos.ELocationNotInUseHelper.type();
    }

}
