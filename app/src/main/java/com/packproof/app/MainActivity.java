package com.packproof.app;

import android.app.*;
import android.os.Bundle;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class MainActivity extends Activity {
 private LinearLayout content, bottomBar;
 private TripStore store;
 private final ExecutorService io=Executors.newSingleThreadExecutor();
 private JSONObject current;
 private JSONObject draftData=new JSONObject();
 private int formStepNo=1;
 private EditText destInput;
 private AutoCompleteTextView hotelInput;
 private CheckBox laundryInput, internationalInput, toiletriesInput, towelsInput;
 private TextView passStatus, depValue, retValue, durationText, peopleValue;
 private String draftPass="";

 // Palette: deep forest green, champagne gold, warm ivory
 private final int green=Color.rgb(18,70,57), ink=Color.rgb(27,40,36), muted=Color.rgb(110,118,113);
 private final int FOREST=Color.rgb(16,61,51), FOREST_LIGHT=Color.rgb(34,96,78), GOLD=Color.rgb(184,149,90);
 private final int IVORY=Color.rgb(247,244,238), LINE=Color.rgb(230,224,212), DANGER=Color.rgb(143,65,52);

 private String screen="home";
 private int homeGeneration=0;
 private LocalDate start=LocalDate.now(), end=start.plusDays(4);
 private static final DateTimeFormatter DAY_MONTH=DateTimeFormatter.ofPattern("d MMM",Locale.ENGLISH);
 private static final DateTimeFormatter FULL_DATE=DateTimeFormatter.ofPattern("EEE, d MMM yyyy",Locale.ENGLISH);

 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}

 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  setContentView(R.layout.activity_main);
  content=findViewById(R.id.content);
  store=new TripStore(this);

  // Re-host the scrolling layout so a bottom action bar can stay pinned below it.
  View scroll=findViewById(R.id.scroll);
  ((ViewGroup)scroll.getParent()).removeView(scroll);
  LinearLayout root=new LinearLayout(this);
  root.setOrientation(LinearLayout.VERTICAL);
  root.setBackgroundColor(IVORY);
  scroll.setBackgroundColor(IVORY);
  root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
  bottomBar=new LinearLayout(this);
  bottomBar.setOrientation(LinearLayout.VERTICAL);
  bottomBar.setPadding(dp(20),dp(6),dp(20),dp(14));
  bottomBar.setBackgroundColor(Color.WHITE);
  bottomBar.setElevation(dp(12));
  bottomBar.setVisibility(View.GONE);
  root.addView(bottomBar,new LinearLayout.LayoutParams(-1,-2));
  setContentView(root);
  root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});

  if(android.os.Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->{if(!goBack())finish();});
  if(state!=null&&state.getString("draft")!=null){try{form(new JSONObject(state.getString("draft")));}catch(Exception e){home();}}
  else if(state!=null&&state.getString("trip")!=null){try{current=new JSONObject(state.getString("trip"));showTrip();}catch(JSONException e){home();}}
  else home();
 }

 @Override protected void onSaveInstanceState(Bundle b){
  super.onSaveInstanceState(b);
  if(screen.equals("form")){try{b.putString("draft",readDraft().put("step",formStepNo).toString());}catch(Exception ignored){}}
  if(current!=null&&screen.equals("trip"))b.putString("trip",current.toString());
 }

 /** Returns false when already on Home (caller should exit). */
 private boolean goBack(){
  if(screen.equals("form")&&formStepNo>1){saveStep();formStep(formStepNo-1);return true;}
  if(!screen.equals("home")){home();return true;}
  return false;
 }

 // ---------- small UI helpers ----------
 private void clear(String name){screen=name;content.removeAllViews();showBottomBar(null,null);}
 private TextView text(LinearLayout host,String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(0,dp(6),0,dp(6));host.addView(t);return t;}
 private TextView title(LinearLayout host,String value,int size,int color){TextView t=text(host,value,size,color,false);t.setTypeface(Typeface.create(Typeface.SERIF,Typeface.BOLD));return t;}
 private TextView label(LinearLayout host,String value,int color){TextView t=text(host,value,11,color,true);t.setLetterSpacing(0.16f);return t;}
 private void gap(){Space s=new Space(this);content.addView(s,new LinearLayout.LayoutParams(1,dp(16)));}
 private GradientDrawable bg(int color){return rounded(color,20,Color.TRANSPARENT);}
 private GradientDrawable rounded(int color,int radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;}
 private int soft(int color){return Color.argb(34,Color.red(color),Color.green(color),Color.blue(color));}
 private LinearLayout card(){return card(content);}
 private LinearLayout card(LinearLayout host){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(14),dp(18),dp(14));c.setBackground(rounded(Color.WHITE,20,LINE));c.setElevation(dp(2));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);p.leftMargin=dp(1);p.rightMargin=dp(1);host.addView(c,p);return c;}

 private Button button(LinearLayout host,String label,Runnable action){
  String kind=label.startsWith("+ Plan")||label.startsWith("Create my")||label.startsWith("Next")?"primary":label.startsWith("Delete")?"danger":"secondary";
  return styledButton(host,label,kind,action);
 }
 private Button styledButton(LinearLayout host,String label,String kind,Runnable action){
  Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(16);b.setStateListAnimator(null);
  b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
  int fill,txt,stroke;
  switch(kind){
   case "primary":fill=FOREST;txt=Color.WHITE;stroke=FOREST;break;
   case "gold":fill=GOLD;txt=Color.WHITE;stroke=GOLD;break;
   case "danger":fill=Color.WHITE;txt=DANGER;stroke=Color.rgb(226,200,193);break;
   default:fill=Color.WHITE;txt=FOREST;stroke=LINE;
  }
  b.setTextColor(txt);b.setBackground(rounded(fill,16,stroke));
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(54));p.topMargin=dp(10);host.addView(b,p);
  b.setOnClickListener(v->action.run());return b;
 }
 private void showBottomBar(String label,Runnable action){
  if(bottomBar==null)return;
  bottomBar.removeAllViews();
  if(label==null){bottomBar.setVisibility(View.GONE);return;}
  bottomBar.setVisibility(View.VISIBLE);
  styledButton(bottomBar,label,"primary",action);
 }
 private void error(Exception e){new AlertDialog.Builder(this).setTitle("Something went wrong").setMessage("Your last change could not be completed. Please try again.").setPositiveButton("OK",null).show();}

 // ---------- trip style helpers ----------
 private int styleColor(String purpose){
  switch(purpose){case "Beach":return Color.rgb(31,138,138);case "Business":return Color.rgb(59,74,107);case "Adventure":return Color.rgb(181,101,29);default:return Color.rgb(96,140,104);}
 }
 private String styleEmoji(String purpose){
  switch(purpose){case "Beach":return "🏖️";case "Business":return "💼";case "Adventure":return "⛰️";default:return "🌿";}
 }
 private String categoryEmoji(String c){
  switch(c){case "Documents":return "📄";case "Clothing":return "👕";case "Essentials":return "🧴";case "Electronics":return "🔌";case "Activities":return "🎒";default:return "✨";}
 }
 private LocalDate parse(String s,LocalDate fallback){try{return LocalDate.parse(s);}catch(Exception e){return fallback;}}
 private String friendlyRange(LocalDate s,LocalDate e){long days=ChronoUnit.DAYS.between(s,e)+1;return s.format(DAY_MONTH)+" → "+e.format(DAY_MONTH)+" · "+days+(days==1?" day":" days");}
 private String countdown(LocalDate s,LocalDate e){
  LocalDate today=LocalDate.now();long d=ChronoUnit.DAYS.between(today,s);
  if(d>1)return "in "+d+" days";if(d==1)return "Tomorrow";if(d==0)return "Today";
  return today.isAfter(e)?"Completed":"On the trip";
 }

 // ---------- HOME ----------
 private void home(){
  int generation=++homeGeneration;current=null;clear("home");

  LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(24),dp(26),dp(24),dp(22));
  GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{FOREST,FOREST_LIGHT});g.setCornerRadius(dp(28));hero.setBackground(g);hero.setElevation(dp(6));
  LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=dp(8);hp.bottomMargin=dp(16);content.addView(hero,hp);
  label(hero,"PACK PACK  ·  TRAVEL LIGHT",GOLD);
  title(hero,"Less worry.\nMore adventure.",32,Color.WHITE);
  text(hero,"Thoughtfully packed checklists for every journey — private, offline and always with you.",15,Color.argb(215,255,255,255),false);
  styledButton(hero,"+  Plan a new trip","gold",this::form);

  LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.HORIZONTAL);header.setGravity(Gravity.BOTTOM);
  LinearLayout.LayoutParams hdp=new LinearLayout.LayoutParams(-1,-2);hdp.topMargin=dp(10);content.addView(header,hdp);
  TextView yt=title(header,"Your journeys",24,ink);yt.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));
  TextView count=label(header,"",GOLD);
  TextView loading=text(content,"Loading saved trips…",14,muted,false);

  io.execute(()->{try{JSONArray trips=store.all();runOnUiThread(()->{
   if(!screen.equals("home")||generation!=homeGeneration)return;
   content.removeView(loading);
   count.setText(trips.length()==1?"1 TRIP":trips.length()+" TRIPS");
   if(trips.length()==0){
    LinearLayout c=card();
    text(c,"🧳",30,ink,false);
    title(c,"Your next adventure starts here",19,ink);
    text(c,"Create a trip and we’ll build a packing list around your plans.",15,muted,false);
    return;
   }
   int upcoming=0,itemsPacked=0;
   for(int i=0;i<trips.length();i++){JSONObject t=trips.optJSONObject(i);if(!LocalDate.now().isAfter(parse(t.optString("end"),LocalDate.now())))upcoming++;JSONArray a=t.optJSONArray("items");if(a!=null)for(int j=0;j<a.length();j++)if(a.optJSONObject(j).optBoolean("packed"))itemsPacked++;}
   addStats(1,trips.length(),upcoming,itemsPacked);
   for(int i=0;i<trips.length();i++)tripCard(trips.optJSONObject(i));
  });}catch(Exception e){runOnUiThread(()->error(e));}});
 }

 private void addStats(int index,int trips,int upcoming,int packed){
  LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
  LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(6);content.addView(row,index,rp);
  String[][] stats={{String.valueOf(trips),"Trips"},{String.valueOf(upcoming),"Upcoming"},{String.valueOf(packed),"Items packed"}};
  for(int i=0;i<stats.length;i++){
   LinearLayout s=new LinearLayout(this);s.setOrientation(LinearLayout.VERTICAL);s.setGravity(Gravity.CENTER);s.setPadding(dp(8),dp(10),dp(8),dp(10));s.setBackground(rounded(Color.WHITE,18,LINE));
   LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1f);if(i>0)p.leftMargin=dp(10);row.addView(s,p);
   TextView n=title(s,stats[i][0],22,FOREST);n.setGravity(Gravity.CENTER);n.setPadding(0,0,0,0);
   TextView l=text(s,stats[i][1],12,muted,false);l.setGravity(Gravity.CENTER);l.setPadding(0,0,0,0);
  }
 }

 private void tripCard(JSONObject t){
  String purpose=t.optString("purpose","Leisure");int accent=styleColor(purpose);
  LocalDate s=parse(t.optString("start"),LocalDate.now()),e=parse(t.optString("end"),s);

  LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
  row.setBackground(rounded(Color.WHITE,22,LINE));row.setElevation(dp(3));row.setClipToOutline(true);
  LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(14);rp.leftMargin=dp(1);rp.rightMargin=dp(1);content.addView(row,rp);
  View strip=new View(this);strip.setBackgroundColor(accent);row.addView(strip,new LinearLayout.LayoutParams(dp(6),-1));

  LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(16),dp(18),dp(14));
  row.addView(body,new LinearLayout.LayoutParams(0,-2,1f));

  LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);body.addView(top);
  TextView emo=new TextView(this);emo.setText(styleEmoji(purpose));emo.setTextSize(22);emo.setGravity(Gravity.CENTER);emo.setBackground(rounded(soft(accent),16,Color.TRANSPARENT));
  top.addView(emo,new LinearLayout.LayoutParams(dp(48),dp(48)));
  LinearLayout names=new LinearLayout(this);names.setOrientation(LinearLayout.VERTICAL);names.setPadding(dp(14),0,dp(8),0);
  top.addView(names,new LinearLayout.LayoutParams(0,-2,1f));
  TextView dest=title(names,t.optString("destination"),21,ink);dest.setPadding(0,0,0,dp(2));
  TextView sub=text(names,purpose+"  ·  "+t.optInt("people",1)+" traveller"+(t.optInt("people",1)==1?"":"s"),13,muted,false);sub.setPadding(0,0,0,0);
  TextView badge=new TextView(this);badge.setText(countdown(s,e));badge.setTextSize(12);badge.setTypeface(null,Typeface.BOLD);badge.setTextColor(accent);
  badge.setPadding(dp(10),dp(5),dp(10),dp(5));badge.setBackground(rounded(soft(accent),20,Color.TRANSPARENT));top.addView(badge);

  TextView dates=text(body,"🗓  "+friendlyRange(s,e),14,ink,false);dates.setPadding(0,dp(14),0,dp(8));

  JSONArray a=t.optJSONArray("items");int total=a==null?0:a.length(),done=0;
  for(int j=0;j<total;j++)if(a.optJSONObject(j).optBoolean("packed"))done++;
  ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
  progress.setMax(Math.max(1,total));progress.setProgress(done);
  progress.setProgressTintList(ColorStateList.valueOf(accent));progress.setProgressBackgroundTintList(ColorStateList.valueOf(LINE));
  body.addView(progress,new LinearLayout.LayoutParams(-1,dp(8)));

  LinearLayout foot=new LinearLayout(this);foot.setOrientation(LinearLayout.HORIZONTAL);foot.setGravity(Gravity.CENTER_VERTICAL);body.addView(foot);
  TextView packed=text(foot,done+" of "+total+" packed",13,muted,false);packed.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));
  text(foot,"Open  →",14,FOREST,true);

  row.setOnClickListener(v->{current=t;showTrip();});
  row.setContentDescription("Open packing list for "+t.optString("destination"));
 }

 // ---------- FORM (3 steps) ----------
 private void form(){
  draftData=new JSONObject();start=LocalDate.now();end=start.plusDays(4);draftPass="";
  try{draftData.put("destination","").put("people",1).put("purpose","Leisure").put("weather","Mild").put("transport","Flight").put("stay","Hotel").put("hotel","").put("laundry",false).put("international",false).put("toiletries",true).put("towels",true).put("boardingPass","");}catch(JSONException ignored){}
  formStep(1);
 }
 private void form(JSONObject draft){
  form();
  try{java.util.Iterator<String> keys=draft.keys();while(keys.hasNext()){String k=keys.next();draftData.put(k,draft.get(k));}}catch(JSONException ignored){}
  start=parse(draftData.optString("start"),start);end=parse(draftData.optString("end"),end);draftPass=draftData.optString("boardingPass");
  formStep(Math.max(1,Math.min(3,draftData.optInt("step",1))));
 }

 private void formStep(int step){
  clear("form");current=null;formStepNo=step;
  String[] names={"Trip","Travel","Stay"};
  String[] titles={"Where are\nyou heading?","How are\nyou travelling?","Where will\nyou stay?"};
  String[] subs={"Start with the destination and your dates.","Tell us who’s coming and the kind of trip. We’ll size the list for the group.","Your stay decides what you need to bring yourself."};

  TextView back=text(content,step==1?"←  Your trips":"←  "+names[step-2],14,FOREST,true);
  back.setOnClickListener(v->goBack());back.setPadding(0,dp(4),0,dp(10));

  LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.HORIZONTAL);content.addView(bar,new LinearLayout.LayoutParams(-1,dp(5)));
  for(int i=1;i<=3;i++){View seg=new View(this);seg.setBackground(rounded(i<=step?(i==step?GOLD:FOREST):LINE,3,Color.TRANSPARENT));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1f);if(i>1)p.leftMargin=dp(6);bar.addView(seg,p);}

  TextView stepLabel=label(content,"STEP 0"+step+" OF 03  ·  "+names[step-1].toUpperCase(Locale.ROOT),GOLD);stepLabel.setPadding(0,dp(18),0,dp(2));
  title(content,titles[step-1],30,ink);
  text(content,subs[step-1],15,muted,false);
  gap();

  if(step==1)stepTrip();else if(step==2)stepTravel();else stepStay();
  showBottomBar(step<3?"Next  →":"Create my packing plan",this::nextStep);
 }

 private void stepTrip(){
  LinearLayout c=card();
  label(c,"DESTINATION",muted);
  destInput=input(c,"e.g. Goa",draftData.optString("destination"));
  destInput.setTextSize(18);

  LinearLayout dc=card();
  label(dc,"TRAVEL DATES",muted);
  LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=dp(6);dc.addView(row,rp);
  depValue=dateTile(row,"DEPARTURE",true,0);
  retValue=dateTile(row,"RETURN",false,dp(10));
  durationText=text(dc,"",14,FOREST,true);durationText.setPadding(0,dp(12),0,0);
  refreshDates();
 }
 private TextView dateTile(LinearLayout row,String caption,boolean first,int leftMargin){
  LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);tile.setPadding(dp(14),dp(10),dp(14),dp(12));tile.setBackground(rounded(IVORY,16,LINE));
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1f);p.leftMargin=leftMargin;row.addView(tile,p);
  TextView cap=label(tile,caption,GOLD);cap.setPadding(0,0,0,dp(2));
  TextView value=text(tile,"",16,ink,true);value.setPadding(0,0,0,0);
  tile.setOnClickListener(v->date(first));tile.setContentDescription("Choose "+caption.toLowerCase(Locale.ROOT)+" date");
  return value;
 }
 private void refreshDates(){
  if(depValue!=null)depValue.setText(start.format(FULL_DATE));
  if(retValue!=null)retValue.setText(end.format(FULL_DATE));
  if(durationText!=null){long d=ChronoUnit.DAYS.between(start,end)+1;durationText.setText(d<1?"Return date is before departure":"✦  "+d+(d==1?" day trip":" day trip")+"  ·  "+countdown(start,end));}
 }
 private void date(boolean first){
  LocalDate d=first?start:end;
  new DatePickerDialog(this,(picker,y,m,day)->{LocalDate chosen=LocalDate.of(y,m+1,day);if(first){start=chosen;if(end.isBefore(start))end=start;}else end=chosen;refreshDates();},d.getYear(),d.getMonthValue()-1,d.getDayOfMonth()).show();
 }

 private void stepTravel(){
  LinearLayout pc=card();
  label(pc,"TRAVELLERS",muted);
  LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);pc.addView(row);
  TextView hint=text(row,"Quantities cover the whole group",13,muted,false);hint.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));
  TextView minus=stepperButton(row,"−");
  peopleValue=title(row,"",22,ink);peopleValue.setGravity(Gravity.CENTER);peopleValue.setLayoutParams(new LinearLayout.LayoutParams(dp(48),-2));
  TextView plus=stepperButton(row,"+");
  peopleValue.setText(String.valueOf(draftData.optInt("people",1)));
  minus.setOnClickListener(v->changePeople(-1));plus.setOnClickListener(v->changePeople(1));

  LinearLayout sc=card();label(sc,"TRIP STYLE",muted);
  chipGrid(sc,"purpose",new String[]{"Leisure","Beach","Business","Adventure"},new String[]{"🌿","🏖️","💼","⛰️"},null);
  LinearLayout wc=card();label(wc,"EXPECTED WEATHER",muted);
  chipGrid(wc,"weather",new String[]{"Mild","Warm","Cold","Rainy"},new String[]{"🌤️","☀️","❄️","🌧️"},null);
  LinearLayout tc=card();label(tc,"GETTING THERE",muted);
  chipGrid(tc,"transport",new String[]{"Flight","Train","Bus","Car"},new String[]{"✈️","🚆","🚌","🚗"},null);

  LinearLayout xc=card();label(xc,"EXTRAS",muted);
  laundryInput=toggle(xc,"Laundry available",draftData.optBoolean("laundry"));
  internationalInput=toggle(xc,"International trip",draftData.optBoolean("international"));
 }
 private TextView stepperButton(LinearLayout row,String symbol){
  TextView b=new TextView(this);b.setText(symbol);b.setTextSize(20);b.setTextColor(FOREST);b.setGravity(Gravity.CENTER);b.setBackground(rounded(IVORY,22,LINE));
  row.addView(b,new LinearLayout.LayoutParams(dp(44),dp(44)));b.setContentDescription(symbol.equals("+")?"Add traveller":"Remove traveller");return b;
 }
 private void changePeople(int delta){
  int n=Math.max(1,Math.min(12,draftData.optInt("people",1)+delta));
  try{draftData.put("people",n);}catch(JSONException ignored){}
  peopleValue.setText(String.valueOf(n));
 }

 private void stepStay(){
  LinearLayout sc=card();label(sc,"ACCOMMODATION",muted);
  chipGrid(sc,"stay",new String[]{"Hotel","Hostel","Apartment","Camping","Other"},new String[]{"🏨","🛏️","🏠","⛺","✨"},()->{
   if("Camping".equals(draftData.optString("stay"))&&toiletriesInput!=null){toiletriesInput.setChecked(false);towelsInput.setChecked(false);}
  });

  LinearLayout hc=card();label(hc,"PLACE NAME  (OPTIONAL)",muted);
  hotelInput=new AutoCompleteTextView(this);hotelInput.setHint("Type a name or choose a previous stay");hotelInput.setSingleLine(true);hotelInput.setThreshold(0);
  hotelInput.setTextColor(ink);hotelInput.setTextSize(16);hotelInput.setBackground(rounded(IVORY,14,LINE));hotelInput.setPadding(dp(14),dp(12),dp(14),dp(12));
  String saved=getPreferences(MODE_PRIVATE).getString("hotels","[]");
  try{JSONArray names=new JSONArray(saved);String[] options=new String[names.length()];for(int i=0;i<names.length();i++)options[i]=names.optString(i);hotelInput.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,options));}catch(JSONException ignored){}
  LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=dp(6);hc.addView(hotelInput,hp);
  hotelInput.setText(draftData.optString("hotel"));
  hotelInput.setOnClickListener(v->hotelInput.showDropDown());
  text(hc,"Your saved stays appear here. No live hotel search or booking.",12,muted,false);

  LinearLayout ac=card();label(ac,"PROVIDED BY YOUR STAY",muted);
  toiletriesInput=toggle(ac,"Soap and shampoo",draftData.optBoolean("toiletries",true));
  towelsInput=toggle(ac,"Towels",draftData.optBoolean("towels",true));

  LinearLayout bc=card();label(bc,"TRAVEL DOCUMENT  (OPTIONAL)",muted);
  passStatus=text(bc,draftPass.isEmpty()?"No boarding-pass image attached":"✓  Boarding-pass image attached",15,draftPass.isEmpty()?ink:FOREST,!draftPass.isEmpty());
  button(bc,draftPass.isEmpty()?"Attach boarding-pass image":"Replace image",()->pickPass());
  text(bc,"The image stays on your device; it is not uploaded or scanned.",12,muted,false);
 }

 private void chipGrid(LinearLayout host,String key,String[] opts,String[] emo,Runnable onChange){
  List<TextView> tiles=new ArrayList<>();LinearLayout row=null;
  for(int i=0;i<opts.length;i++){
   if(i%2==0){row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=dp(8);host.addView(row,rp);}
   TextView t=new TextView(this);t.setText(emo[i]+"  "+opts[i]);t.setTextSize(15);t.setGravity(Gravity.CENTER);t.setPadding(dp(8),dp(14),dp(8),dp(14));
   LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1f);if(i%2==1)p.leftMargin=dp(10);row.addView(t,p);tiles.add(t);
   final String value=opts[i];
   t.setOnClickListener(v->{try{draftData.put(key,value);}catch(JSONException ignored){}styleChips(tiles,opts,value);if(onChange!=null)onChange.run();});
  }
  if(opts.length%2==1&&row!=null){Space s=new Space(this);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,1,1f);p.leftMargin=dp(10);row.addView(s,p);}
  styleChips(tiles,opts,draftData.optString(key,opts[0]));
 }
 private void styleChips(List<TextView> tiles,String[] opts,String selected){
  for(int i=0;i<tiles.size();i++){boolean on=opts[i].equals(selected);TextView t=tiles.get(i);
   t.setBackground(rounded(on?FOREST:IVORY,16,on?FOREST:LINE));t.setTextColor(on?Color.WHITE:ink);t.setTypeface(null,on?Typeface.BOLD:Typeface.NORMAL);}
 }

 private EditText input(LinearLayout host,String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextColor(ink);e.setTextSize(16);e.setSingleLine(true);e.setBackground(rounded(IVORY,14,LINE));e.setPadding(dp(14),dp(12),dp(14),dp(12));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(6);host.addView(e,p);return e;}
 private CheckBox toggle(LinearLayout host,String label,boolean checked){CheckBox c=new CheckBox(this);c.setText(label);c.setTextSize(15);c.setTextColor(ink);c.setChecked(checked);c.setMinHeight(dp(48));c.setButtonTintList(ColorStateList.valueOf(FOREST));host.addView(c);return c;}

 /** Copies the widgets of the current step into draftData. */
 private void saveStep(){
  try{
   if(formStepNo==1&&destInput!=null)draftData.put("destination",destInput.getText().toString().trim());
   if(formStepNo==2&&laundryInput!=null)draftData.put("laundry",laundryInput.isChecked()).put("international",internationalInput.isChecked());
   if(formStepNo==3&&hotelInput!=null)draftData.put("hotel",hotelInput.getText().toString().trim()).put("toiletries",toiletriesInput.isChecked()).put("towels",towelsInput.isChecked());
   draftData.put("start",start.toString()).put("end",end.toString()).put("boardingPass",draftPass);
  }catch(JSONException ignored){}
 }
 private JSONObject readDraft() throws JSONException{saveStep();JSONObject d=new JSONObject(draftData.toString());d.remove("step");return d;}

 private void nextStep(){
  saveStep();
  if(formStepNo==1){
   if(draftData.optString("destination").isEmpty()){destInput.setError("Enter a destination");destInput.requestFocus();return;}
   long days=ChronoUnit.DAYS.between(start,end)+1;
   if(days<1||days>90){new AlertDialog.Builder(this).setMessage("Choose a return date on or after departure; maximum 90 days.").setPositiveButton("OK",null).show();return;}
   formStep(2);
  }else if(formStepNo==2)formStep(3);
  else createTrip();
 }
 private void createTrip(){
  int people=draftData.optInt("people",1);
  if(people<1||people>12){formStep(2);return;}
  long days=ChronoUnit.DAYS.between(start,end)+1;
  try{
   current=readDraft().put("id",System.currentTimeMillis()).put("people",people);
   current.put("items",PackingRules.generate((int)days,current.getString("purpose"),current.getString("weather"),current.getBoolean("laundry"),current.getBoolean("international"),people,current.getString("transport"),current.getString("stay"),current.getBoolean("toiletries"),current.getBoolean("towels")));
   String name=current.optString("hotel");
   if(!name.isEmpty()){JSONArray names=new JSONArray(getPreferences(MODE_PRIVATE).getString("hotels","[]"));boolean exists=false;for(int i=0;i<names.length();i++)if(name.equalsIgnoreCase(names.optString(i)))exists=true;if(!exists)names.put(name);getPreferences(MODE_PRIVATE).edit().putString("hotels",names.toString()).apply();}
   persist();showTrip();
  }catch(Exception e){error(e);}
 }

 // ---------- boarding pass ----------
 private void pickPass(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,42);}
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(request!=42||result!=RESULT_OK||data==null||data.getData()==null)return;
  try{Uri uri=data.getData();getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
   if(screen.equals("trip")&&current!=null){current.put("boardingPass",uri.toString());persist();showTrip();}
   else if(screen.equals("form")){draftPass=uri.toString();saveStep();formStep(formStepNo);}
  }catch(Exception e){error(e);}
 }
 private void documentCard(){LinearLayout c=card();text(c,"🎫  Boarding pass",18,ink,true);String pass=current.optString("boardingPass");if(pass.isEmpty()){text(c,"Keep an image handy for this trip.",14,muted,false);}else{
  ImageView image=new ImageView(this);image.setAdjustViewBounds(true);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setContentDescription("Attached boarding-pass image");c.addView(image,new LinearLayout.LayoutParams(-1,dp(200)));text(c,"Saved image · tap to enlarge",13,muted,false);
  io.execute(()->{try{Uri uri=Uri.parse(pass);BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;try(InputStream in=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,options);}options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>1600)options.inSampleSize*=2;options.inJustDecodeBounds=false;Bitmap bitmap;try(InputStream in=getContentResolver().openInputStream(uri)){bitmap=BitmapFactory.decodeStream(in,null,options);}runOnUiThread(()->{if(bitmap!=null)image.setImageBitmap(bitmap);else image.setContentDescription("Cannot preview this image; replace the attachment");});}catch(Exception e){runOnUiThread(()->{image.setVisibility(View.GONE);text(c,"Image is no longer accessible. Please attach it again.",14,muted,false);});}});
  image.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(pass),"image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));}catch(Exception e){error(e);}});
  button(c,"Remove attachment",()->{try{current.put("boardingPass","");persist();showTrip();}catch(Exception e){error(e);}});
 }button(c,pass.isEmpty()?"Attach image":"Replace image",()->pickPass());}
 private void persist(){if(current==null)return;String snapshot=current.toString();io.execute(()->{try{store.save(new JSONObject(snapshot));}catch(Exception e){runOnUiThread(()->error(e));}});}

 // ---------- PACKING PLAN ----------
 private void showTrip(){
  clear("trip");
  String purpose=current.optString("purpose","Leisure");int accent=styleColor(purpose);
  LocalDate s=parse(current.optString("start"),LocalDate.now()),e=parse(current.optString("end"),s);
  TextView back=text(content,"←  Your trips",14,FOREST,true);back.setOnClickListener(v->home());back.setPadding(0,dp(4),0,dp(8));
  label(content,"YOUR PACKING PLAN",GOLD);
  title(content,styleEmoji(purpose)+"  "+current.optString("destination"),32,ink);
  text(content,friendlyRange(s,e)+"  ·  "+countdown(s,e),15,muted,false);
  text(content,purpose+" · "+current.optString("weather")+" · "+current.optString("transport","Travel"),15,muted,false);
  LinearLayout stay=card();text(stay,current.optInt("people",1)+" traveller(s) · "+current.optString("stay","Hotel"),18,ink,true);text(stay,current.optString("hotel").isEmpty()?"Accommodation not specified":current.optString("hotel"),15,muted,false);text(stay,"This checklist covers the whole group. Adjust quantities to suit your bags.",13,muted,false);
  documentCard();
  JSONArray items=current.optJSONArray("items");int packed=0;for(int i=0;i<items.length();i++)if(items.optJSONObject(i).optBoolean("packed"))packed++;
  LinearLayout summary=card();boolean ready=packed==items.length()&&packed>0;
  if(ready)summary.setBackground(rounded(Color.rgb(232,243,236),20,Color.rgb(196,224,206)));
  title(summary,ready?"✓  You’re ready to go!":"A little closer to takeoff",20,green);
  text(summary,packed+" of "+items.length()+" items packed",15,muted,false);
  ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(Math.max(1,items.length()));progress.setProgress(packed);progress.setProgressTintList(ColorStateList.valueOf(accent));progress.setProgressBackgroundTintList(ColorStateList.valueOf(LINE));summary.addView(progress,new LinearLayout.LayoutParams(-1,dp(10)));
  button(content,"+ Add your own item",()->edit(null));gap();
  for(String category:new String[]{"Documents","Clothing","Essentials","Electronics","Activities","Custom"}){boolean heading=false;for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);if(!category.equals(item.optString("category")))continue;if(!heading){label(content,categoryEmoji(category)+"  "+category.toUpperCase(Locale.ROOT),FOREST);heading=true;}LinearLayout c=card();CheckBox check=new CheckBox(this);check.setText(item.optString("name")+" × "+item.optInt("quantity",1));check.setTextSize(17);check.setTextColor(item.optBoolean("packed")?muted:ink);check.setButtonTintList(ColorStateList.valueOf(FOREST));check.setChecked(item.optBoolean("packed"));if(item.optBoolean("packed"))check.setPaintFlags(check.getPaintFlags()|android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);c.addView(check);check.setOnCheckedChangeListener((v,on)->{try{item.put("packed",on);persist();int y=findViewById(R.id.scroll).getScrollY();showTrip();findViewById(R.id.scroll).post(()->findViewById(R.id.scroll).scrollTo(0,y));}catch(Exception ex){error(ex);}});TextView why=text(c,"Why pack this?",14,green,true);why.setContentDescription("Why pack "+item.optString("name"));why.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(item.optString("name")).setMessage(item.optString("why")).setPositiveButton("Got it",null).show());TextView modify=text(c,"Edit / remove",13,muted,false);modify.setContentDescription("Edit or remove "+item.optString("name"));modify.setOnClickListener(v->edit(item));}}
  button(content,"Back to trips",()->home());
  button(content,"Delete this trip",()->new AlertDialog.Builder(this).setTitle("Delete this trip?").setMessage("This removes the trip and its packing checklist.").setNegativeButton("Keep trip",null).setPositiveButton("Delete",(d,w)->{long id=current.optLong("id");io.execute(()->{store.delete(id);runOnUiThread(()->home());});}).show());
 }

 private void edit(JSONObject existing){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(8),dp(20),dp(8));EditText name=input(box,"Item name",existing==null?"":existing.optString("name"));EditText quantity=input(box,"Quantity",existing==null?"1":""+existing.optInt("quantity"));quantity.setInputType(2);AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(existing==null?"Add an item":"Edit item").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save",null);
  if(existing!=null)builder.setNeutralButton("Remove",(d,w)->{JSONArray a=current.optJSONArray("items");for(int i=0;i<a.length();i++)if(a.optJSONObject(i)==existing){a.remove(i);break;}persist();showTrip();});
  AlertDialog dialog=builder.create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{String n=name.getText().toString().trim();if(n.isEmpty()){name.setError("Enter an item name");return;}int q;try{q=Integer.parseInt(quantity.getText().toString());if(q<1||q>9999)throw new NumberFormatException();}catch(NumberFormatException e){quantity.setError("Use a number from 1 to 9999");return;}try{JSONObject item=existing==null?new JSONObject().put("category","Custom").put("why","Added by you for this trip.").put("packed",false):existing;item.put("name",n).put("quantity",q);if(existing==null)current.optJSONArray("items").put(item);persist();dialog.dismiss();showTrip();}catch(Exception e){error(e);}}));dialog.show();
 }

 // Legacy fallback; Android 13+ uses the platform callback registered in onCreate.
 @android.annotation.SuppressLint("GestureBackNavigation")
 @Override public void onBackPressed(){if(!goBack())super.onBackPressed();}
 @Override public void onDestroy(){super.onDestroy();io.shutdown();}
}
