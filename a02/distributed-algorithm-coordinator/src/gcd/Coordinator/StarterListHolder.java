package gcd.Coordinator;


/**
* gcd/Coordinator/StarterListHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from distributed-algorithm.idl
* Dienstag, 19. Mai 2015 08:38 Uhr MESZ
*/

public final class StarterListHolder implements org.omg.CORBA.portable.Streamable
{
  public gcd.Starter.Starter value[] = null;

  public StarterListHolder ()
  {
  }

  public StarterListHolder (gcd.Starter.Starter[] initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = gcd.Coordinator.StarterListHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    gcd.Coordinator.StarterListHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return gcd.Coordinator.StarterListHelper.type ();
  }

}
