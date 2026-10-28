package sa.urtv.screensaver;
import android.content.*;
import java.util.*;
/** For tests/saver_cards_test.py: the screensaver's own save of the home cards (SaverReceiver.set, the code the phone page reaches through ConfigProvider) on an in-memory store. */
public final class SaverCardsRun {
  static class SP implements SharedPreferences {
    Map<String,Object> m = new HashMap<>();
    public Map<String,?> getAll(){return m;}
    public String getString(String k,String d){return m.containsKey(k)?(String)m.get(k):d;}
    public Set<String> getStringSet(String k,Set<String> d){return d;}
    public int getInt(String k,int d){return m.containsKey(k)?(Integer)m.get(k):d;}
    public long getLong(String k,long d){return m.containsKey(k)?(Long)m.get(k):d;}
    public float getFloat(String k,float d){return d;}
    public boolean getBoolean(String k,boolean d){return m.containsKey(k)?(Boolean)m.get(k):d;}
    public boolean contains(String k){return m.containsKey(k);}
    public Editor edit(){ return new Editor(){
      Map<String,Object> t=new HashMap<>(); List<String> rm=new ArrayList<>();
      public Editor putString(String k,String v){t.put(k,v);return this;}
      public Editor putStringSet(String k,Set<String> v){return this;}
      public Editor putInt(String k,int v){t.put(k,v);return this;}
      public Editor putLong(String k,long v){t.put(k,v);return this;}
      public Editor putFloat(String k,float v){return this;}
      public Editor putBoolean(String k,boolean v){t.put(k,v);return this;}
      public Editor remove(String k){rm.add(k);return this;}
      public Editor clear(){return this;}
      public boolean commit(){apply();return true;}
      public void apply(){ m.putAll(t); for(String r:rm)m.remove(r);} }; }
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
  }
  public static void main(String[] a) throws Exception {
    final SP sp = new SP();
    Context c = new ContextWrapper(null){
      public SharedPreferences getSharedPreferences(String n,int m){return sp;}
      public void sendBroadcast(Intent i){}
      public String getPackageName(){return "sa.urtv.screensaver";}
    };
    // the owner's sequence: a card, then «بدون» (the phone page sends "none" for an empty list), then three cards, then the empty string
    String[][] steps = {{"nextprayer", "nextprayer"}, {"none", "none"}, {"verse,hourly,prayers", "verse,hourly,prayers"}, {"", "none"}, {"nextprayer", "nextprayer"}, {"none", "none"}};
    for (String[] st : steps) {
      Intent i = new Intent(); i.putExtra("home_panel", st[0]);
      // describe() (the answer's text) needs the system; the save is made before it, so an exception from there is expected here
      try { SaverReceiver.set(c, new Prefs(c), i); } catch (NullPointerException e) { }
      String got = new Prefs(c).homePanel();
      System.out.println((got.equals(st[1]) ? "PASS " : "FAIL ") + "home_panel '" + st[0] + "' is stored as " + st[1] + " (got " + got + ")");
    }
  }
}
