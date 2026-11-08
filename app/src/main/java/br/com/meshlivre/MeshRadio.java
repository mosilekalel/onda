package br.com.meshlivre;
import android.annotation.SuppressLint;
import android.bluetooth.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
@SuppressLint("MissingPermission")
public final class MeshRadio {
 public interface Events {void status(String s);void message(Packet p);void peers(int count);}
 private static final UUID SERVICE=UUID.fromString("bf438ab9-95e6-4523-b061-e1f078493508");
 private final BluetoothAdapter adapter; private final Events events; private final Router router=new Router();
 private final ConcurrentHashMap<String,Link> links=new ConcurrentHashMap<>();
 private final Set<BluetoothSocket> pending=ConcurrentHashMap.newKeySet();
 private final Set<String> dialing=ConcurrentHashMap.newKeySet();
 private final ScheduledExecutorService timers=Executors.newScheduledThreadPool(1);
 private volatile boolean active; private volatile BluetoothServerSocket server;
 public MeshRadio(BluetoothAdapter a,Events e){adapter=a;events=e;}
 public synchronized void start(){if(active)return;active=true;new Thread(()->{
  while(active){BluetoothServerSocket listening=null;try {
   listening=adapter.listenUsingRfcommWithServiceRecord("MeshLivre",SERVICE);server=listening;
   if(!active)break;
   BluetoothSocket socket=listening.accept();listening.close();attach(socket);
  }catch(Exception e){if(active){events.status("Escuta interrompida. Pare e inicie a rede novamente.");break;}}
  finally{if(listening!=null)try{listening.close();}catch(IOException ignored){}}}
 },"mesh-listener").start();events.status("Rede iniciada. Conecte os aparelhos pela lista.");}
 public void connect(BluetoothDevice device){String address=device.getAddress();
  if(!active || links.containsKey(address)||!dialing.add(address))return;
  new Thread(()->{BluetoothSocket s=null;ScheduledFuture<?> timeout=null;
   try {adapter.cancelDiscovery();s=device.createRfcommSocketToServiceRecord(SERVICE);pending.add(s);
    BluetoothSocket current=s;timeout=timers.schedule(()->closeSocket(current),30,TimeUnit.SECONDS);
    if(!active)throw new IOException("Stopped");s.connect();timeout.cancel(false);attach(s);
   }catch(Exception e){if(s!=null)closeSocket(s);if(active)events.status("Não conectou. Verifique o app e o pareamento no outro aparelho.");}
   finally{if(timeout!=null)timeout.cancel(false);if(s!=null)pending.remove(s);dialing.remove(address);}
  },"mesh-connect").start();}
 private synchronized void attach(BluetoothSocket s)throws IOException {
  String key=s.getRemoteDevice().getAddress();if(!active||links.containsKey(key)||links.size()>=4){s.close();return;}
  Link l=new Link(s,key);links.put(key,l);events.peers(links.size());events.status("Vizinho conectado: "+key);
  l.start();for(Packet p:router.replay())l.send(p);
 }
 public void send(String node,String name,String text){Packet p=new Packet(UUID.randomUUID().toString(),node,name,text,System.currentTimeMillis(),0);router.accept(p);events.message(p);broadcast(p.next(),null);}
 private void received(Packet p,Link source){if(!router.accept(p))return;events.message(p);if(p.hops<Packet.MAX_HOPS)broadcast(p.next(),source);}
 private void broadcast(Packet p,Link skip){for(Link l:links.values())if(l!=skip)l.send(p);}
 private static void closeSocket(BluetoothSocket s){try{s.close();}catch(IOException ignored){}}
 public synchronized void stop(){active=false;try{if(server!=null)server.close();}catch(IOException ignored){}for(BluetoothSocket s:pending)closeSocket(s);for(Link l:links.values())l.close();events.peers(0);}
 public void destroy(){stop();timers.shutdownNow();}
 private final class Link {
  final BluetoothSocket socket;final String key;final BlockingQueue<Packet> queue=new ArrayBlockingQueue<>(512);
  volatile boolean closed;Thread writer;
  Link(BluetoothSocket s,String k){socket=s;key=k;}
  void send(Packet p){if(!closed&&!queue.offer(p)){events.status("Vizinho lento: conexão encerrada.");close();}}
  void start(){writer=new Thread(()->{try{DataOutputStream out=new DataOutputStream(socket.getOutputStream());while(!closed){Packet p=queue.take();byte[] data=p.encode();out.writeInt(data.length);out.write(data);out.flush();}}catch(Exception e){}finally{close();}},"mesh-write");writer.start();
   new Thread(()->{try{DataInputStream in=new DataInputStream(socket.getInputStream());while(!closed){int n=in.readInt();if(n<1||n>Packet.MAX_FRAME)throw new IOException("Frame size");byte[] b=new byte[n];in.readFully(b);received(Packet.decode(b),this);}}catch(Exception e){}finally{close();}},"mesh-read").start();
  }
  synchronized void close(){if(closed)return;closed=true;closeSocket(socket);if(writer!=null)writer.interrupt();links.remove(key,this);events.peers(links.size());}
 }
}
