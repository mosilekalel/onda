import br.com.meshlivre.*;
import java.util.*;
public class ProtocolTest {
 static void check(boolean v){if(!v)throw new AssertionError();}
 static Packet packet(int hops){return new Packet(UUID.randomUUID().toString(),UUID.randomUUID().toString(),"João","Olá 🌎",System.currentTimeMillis(),hops);}
 public static void main(String[] args)throws Exception{
  Packet p=packet(0),copy=Packet.decode(p.encode());check(copy.text.equals(p.text)&&copy.id.equals(p.id));
  Router a=new Router(),b=new Router(),c=new Router();check(a.accept(p));check(b.accept(p.next()));check(c.accept(p.next().next()));check(!a.accept(p.next().next().next()));check(!b.accept(p));
  check(b.replay().get(0).hops==2);Router end=new Router();check(end.accept(packet(8)));check(end.replay().isEmpty());
  try{Packet.decode(new byte[8193]);throw new AssertionError();}catch(java.io.IOException expected){}
  try{Packet.decode(new byte[]{1,2,3});throw new AssertionError();}catch(java.io.IOException expected){}
  try{packet(9);throw new AssertionError();}catch(IllegalArgumentException expected){}
  byte[] trailing=Arrays.copyOf(p.encode(),p.encode().length+1);try{Packet.decode(trailing);throw new AssertionError();}catch(java.io.IOException expected){}
  Router bounded=new Router();for(int i=0;i<600;i++)bounded.accept(packet(0));check(bounded.replay().size()==500);
  System.out.println("PASS: codec Unicode, A-B-C, loop, dedup, TTL, bounds, invalid frames, replay.");
 }
}
