package br.com.meshlivre;
import java.util.*;
/** Bounded in-memory history and replay; clock-independent local expiry. */
public final class Router {
 private final LinkedHashMap<String,Entry> seen=new LinkedHashMap<>();
 private static final long AGE=10L*60*1000*1000*1000;
 private static class Entry { final Packet p; final long at=System.nanoTime(); Entry(Packet p){this.p=p;} }
 private void prune(){long now=System.nanoTime();seen.values().removeIf(e->now-e.at>AGE);}
 public synchronized boolean accept(Packet p){prune();if(seen.containsKey(p.id))return false;seen.put(p.id,new Entry(p));while(seen.size()>500)seen.remove(seen.keySet().iterator().next());return true;}
 public synchronized List<Packet> replay(){prune();List<Packet> r=new ArrayList<>();for(Entry e:seen.values())if(e.p.hops<Packet.MAX_HOPS)r.add(e.p.next());return r;}
}
