package br.com.meshlivre;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
/** Fixed bounded binary format; no Java object deserialization. */
public final class Packet {
 public static final int MAX_HOPS=8, MAX_FRAME=8192;
 public final String id, sender, name, text;
 public final long created;
 public final int hops;
 public Packet(String id,String sender,String name,String text,long created,int hops) {
  UUID.fromString(id); UUID.fromString(sender);
  if(name.length()<1 || name.length()>32 || text.trim().isEmpty() || text.length()>1000 || hops<0 || hops>MAX_HOPS) throw new IllegalArgumentException("Invalid packet");
  this.id=id; this.sender=sender; this.name=name; this.text=text; this.created=created; this.hops=hops;
 }
 public Packet next(){return new Packet(id,sender,name,text,created,hops+1);}
 public byte[] encode() throws IOException {
  ByteArrayOutputStream b=new ByteArrayOutputStream(); DataOutputStream d=new DataOutputStream(b);
  d.writeInt(0x4d4c0001); d.writeUTF(id);d.writeUTF(sender);d.writeUTF(name);d.writeUTF(text);d.writeLong(created);d.writeInt(hops);return b.toByteArray();
 }
 public static Packet decode(byte[] b) throws IOException {
  if(b.length>MAX_FRAME)throw new IOException("Oversized frame");
  try {DataInputStream d=new DataInputStream(new ByteArrayInputStream(b));
   if(d.readInt()!=0x4d4c0001)throw new IOException("Unsupported protocol");
   Packet p=new Packet(d.readUTF(),d.readUTF(),d.readUTF(),d.readUTF(),d.readLong(),d.readInt());
   if(d.available()!=0)throw new IOException("Trailing data"); return p;
  }catch(IllegalArgumentException e){throw new IOException("Invalid fields",e);}
 }
}
