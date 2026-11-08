package br.com.meshlivre;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.*;
import android.text.InputFilter;
import android.view.*;
import android.widget.*;
import java.util.*;
@SuppressLint("MissingPermission")
public final class MainActivity extends Activity implements MeshRadio.Events {
 private BluetoothAdapter adapter;private MeshRadio radio;private String node;
 private TextView state,count;private EditText nickname,input;private LinearLayout messages,devices;private ScrollView scroll;
 private boolean registered,running;private final Set<String> shown=new HashSet<>();
 private final BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){
  if(BluetoothDevice.ACTION_FOUND.equals(i.getAction())){BluetoothDevice d=i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);if(d!=null)addDevice(d);}
  else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(i.getAction()))status("Busca concluída. Toque no aparelho para conectar.");
  else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(i.getAction())&&i.getIntExtra(BluetoothAdapter.EXTRA_STATE,0)==BluetoothAdapter.STATE_OFF)stopNetwork();
 }};
 @Override public void onCreate(Bundle b){super.onCreate(b);
  BluetoothManager manager=getSystemService(BluetoothManager.class);adapter=manager==null?null:manager.getAdapter();
  android.content.SharedPreferences prefs=getSharedPreferences("identity",0);node=prefs.getString("node",null);
  if(node==null){node=UUID.randomUUID().toString();prefs.edit().putString("node",node).apply();}
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(12),dp(18),dp(10));root.setBackgroundColor(Color.rgb(16,24,39));setContentView(root);
  root.setOnApplyWindowInsetsListener((v,insets)->{root.setPadding(dp(18),dp(12)+insets.getSystemWindowInsetTop(),dp(18),dp(10)+insets.getSystemWindowInsetBottom());return insets;});
  root.addView(label("MESH LIVRE",26,Color.rgb(85,230,193)));root.addView(label("ALPHA 0.1  •  Chat coletivo sem internet",13,Color.LTGRAY));
  count=label("0 vizinhos conectados",16,Color.WHITE);root.addView(count);
  nickname=edit("Seu apelido",32);nickname.setText(prefs.getString("name","Pessoa-"+node.substring(0,4)));root.addView(nickname);
  LinearLayout actions=new LinearLayout(this);root.addView(actions);button(actions,"Iniciar",()->startNetwork());button(actions,"Parar",()->stopNetwork());
  LinearLayout discover=new LinearLayout(this);root.addView(discover);button(discover,"Ficar visível",()->visible());button(discover,"Buscar",()->search());
  state=label("Inicie a rede nos aparelhos. Mantenha o app aberto.",13,Color.LTGRAY);root.addView(state);
  ScrollView deviceScroll=new ScrollView(this);devices=new LinearLayout(this);devices.setOrientation(LinearLayout.VERTICAL);deviceScroll.addView(devices);root.addView(deviceScroll,new LinearLayout.LayoutParams(-1,dp(105)));
  root.addView(label("Sala pública • participantes podem ler e retransmitir",12,Color.rgb(245,193,105)));
  scroll=new ScrollView(this);messages=new LinearLayout(this);messages.setOrientation(LinearLayout.VERTICAL);scroll.addView(messages);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  input=edit("Escreva uma mensagem…",1000);input.setSingleLine(false);input.setMaxLines(3);root.addView(input);
  button(root,"Enviar à rede",()->sendMessage());
  if(adapter!=null)radio=new MeshRadio(adapter,this);else status("Este aparelho não oferece Bluetooth compatível.");
  IntentFilter filter=new IntentFilter(BluetoothDevice.ACTION_FOUND);filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
  if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,filter,Context.RECEIVER_EXPORTED);else registerReceiver(receiver,filter);registered=true;
 }
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
 private TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(4),0,dp(4));return t;}
 private EditText edit(String hint,int max){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.GRAY);e.setTextSize(16);e.setSingleLine();e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(max)});return e;}
 private void button(LinearLayout parent,String text,Runnable action){Button b=new Button(this);b.setText(text);b.setAllCaps(false);parent.addView(b);b.setOnClickListener(v->action.run());}
 private boolean permissions(){String[] perms=Build.VERSION.SDK_INT>=31?new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_ADVERTISE}:new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
  for(String s:perms)if(checkSelfPermission(s)!=PackageManager.PERMISSION_GRANTED){requestPermissions(perms,10);return false;}return true;}
 private boolean ready(){if(adapter==null||!permissions())return false;if(!adapter.isEnabled()){startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),11);return false;}return true;}
 private void startNetwork(){if(!ready())return;radio.start();running=true;getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
 private void stopNetwork(){if(radio!=null)radio.stop();running=false;getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);if(adapter!=null&&checkSelfPermission(Build.VERSION.SDK_INT>=31?Manifest.permission.BLUETOOTH_SCAN:Manifest.permission.BLUETOOTH_ADMIN)==PackageManager.PERMISSION_GRANTED)try{adapter.cancelDiscovery();}catch(Exception ignored){}status("Rede parada. Toque em Iniciar para retomar.");}
 private void visible(){if(!ready())return;startNetwork();Intent i=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);startActivity(i);}
 private void search(){if(!ready())return;startNetwork();shown.clear();devices.removeAllViews();for(BluetoothDevice d:adapter.getBondedDevices())addDevice(d);adapter.cancelDiscovery();if(adapter.startDiscovery())status("Buscando… deixe o outro aparelho visível.");else status("Busca não iniciou. No Android 8–11, ative também a Localização.");}
 private void addDevice(BluetoothDevice d){String address=d.getAddress();if(!shown.add(address))return;String name=d.getName();button(devices,(name==null?"Aparelho":name)+" • "+address,()->{if(!ready())return;if(!running)startNetwork();status("Conectando… aceite o pareamento nos dois aparelhos.");radio.connect(d);});}
 private void sendMessage(){String text=input.getText().toString().trim(),name=nickname.getText().toString().trim();if(!running){status("Inicie a rede antes de enviar.");return;}if(text.isEmpty()||name.isEmpty()){status("Preencha apelido e mensagem.");return;}getSharedPreferences("identity",0).edit().putString("name",name).apply();radio.send(node,name,text);input.setText("");}
 @Override public void status(String s){runOnUiThread(()->{if(!isDestroyed())state.setText(s);});}
 @Override public void peers(int n){runOnUiThread(()->{if(!isDestroyed())count.setText(n+" vizinho(s) conectado(s)");});}
 @Override public void message(Packet p){runOnUiThread(()->{if(isDestroyed())return;boolean mine=p.sender.equals(node);String detail=mine?"Você • enviada à fila local, sem confirmação":p.name+" • "+p.hops+" salto(s) • ID "+p.sender.substring(0,6);
  TextView t=label(detail+"\n"+p.text,16,mine?Color.rgb(85,230,193):Color.WHITE);t.setPadding(dp(10),dp(10),dp(10),dp(10));messages.addView(t);while(messages.getChildCount()>200)messages.removeViewAt(0);scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));});}
 @Override public void onRequestPermissionsResult(int request,String[] p,int[] results){super.onRequestPermissionsResult(request,p,results);if(request==10){boolean granted=results.length>0;for(int r:results)granted&=r==PackageManager.PERMISSION_GRANTED;if(granted)startNetwork();else status("Permissão negada. Autorize Dispositivos próximos (ou Localização) nas configurações do app.");}}
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==11&&result==RESULT_OK)startNetwork();}
 @Override protected void onDestroy(){if(registered)unregisterReceiver(receiver);if(radio!=null)radio.destroy();super.onDestroy();}
}
