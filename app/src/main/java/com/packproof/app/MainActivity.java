package com.packproof.app;

import android.app.*;
import android.os.Bundle;
import android.content.Intent;
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
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class MainActivity extends Activity {
 private LinearLayout content;
 private TripStore store;
 private final ExecutorService io=Executors.newSingleThreadExecutor();
 private JSONObject current;
 private EditText destInput, peopleInput;
 private AutoCompleteTextView hotelInput;
 private Spinner purposeInput, weatherInput, transportInput, stayInput;
 private CheckBox laundryInput, internationalInput, toiletriesInput, towelsInput;
 private TextView passStatus;
 private String draftPass="";
 private Button departureButton, returnButton;
 private final int green=Color.rgb(23,107,85), ink=Color.rgb(30,48,42), muted=Color.rgb(100,112,103);
 private String screen="home";
 private int homeGeneration=0;
 private LocalDate start=LocalDate.now(), end=start.plusDays(4);
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
 @Override public void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_main);content=findViewById(R.id.content);store=new TripStore(this);
  if(android.os.Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->{if(!screen.equals("home"))home();else finish();});
  findViewById(R.id.scroll).setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
  if(state!=null&&state.getString("draft")!=null){try{form(new JSONObject(state.getString("draft")));}catch(Exception e){home();}}else if(state!=null&&state.getString("trip")!=null){try{current=new JSONObject(state.getString("trip"));showTrip();}catch(JSONException e){home();}}else home();
 }
 @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);if(screen.equals("form")){try{b.putString("draft",readDraft().toString());}catch(Exception ignored){}}if(current!=null&&screen.equals("trip"))b.putString("trip",current.toString());}
 private void clear(String name){screen=name;content.removeAllViews();}
 private TextView text(LinearLayout host,String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(0,dp(6),0,dp(6));host.addView(t);return t;}
 private void gap(){Space s=new Space(this);content.addView(s,new LinearLayout.LayoutParams(1,dp(16)));}
 private GradientDrawable bg(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(20));return d;}
 private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(14),dp(18),dp(14));c.setBackground(bg(Color.WHITE));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);content.addView(c,p);return c;}
 private Button button(LinearLayout host,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);boolean primary=label.startsWith("+ Plan")||label.startsWith("Create my");b.setTextColor(primary?Color.WHITE:green);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary?green:Color.rgb(231,237,227)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(54));p.topMargin=dp(8);host.addView(b,p);b.setOnClickListener(v->action.run());return b;}
 private void error(Exception e){new AlertDialog.Builder(this).setTitle("Something went wrong").setMessage("Your last change could not be completed. Please try again.").setPositiveButton("OK",null).show();}
 private void home(){int generation=++homeGeneration;current=null;clear("home");text(content,"PACK PACK  /  TRAVEL LIGHT",12,green,true);text(content,"Less worry.\nMore adventure.",34,ink,true);text(content,"Your trip, thoughtfully packed. Offline and always with you.",16,muted,false);button(content,"+ Plan a new trip",()->form());gap();text(content,"Your trips",22,ink,true);text(content,"Loading saved trips…",14,muted,false);
  io.execute(()->{try{JSONArray trips=store.all();runOnUiThread(()->{if(!screen.equals("home")||generation!=homeGeneration)return;content.removeViewAt(content.getChildCount()-1);if(trips.length()==0){LinearLayout c=card();text(c,"Your next adventure starts here",19,ink,true);text(c,"Create a trip and we’ll build a packing list around your plans.",15,muted,false);}for(int i=0;i<trips.length();i++){JSONObject t=trips.optJSONObject(i);LinearLayout c=card();text(c,t.optString("destination"),22,ink,true);text(c,t.optString("start")+" → "+t.optString("end")+" · "+t.optString("purpose"),14,muted,false);JSONArray a=t.optJSONArray("items");int done=0;for(int j=0;j<a.length();j++)if(a.optJSONObject(j).optBoolean("packed"))done++;text(c,done+" / "+a.length()+" items packed",14,green,true);button(c,"Open packing list",()->{current=t;showTrip();});}});}catch(Exception e){runOnUiThread(()->error(e));}});
 }
 private EditText input(LinearLayout host,String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextColor(ink);e.setSingleLine(true);e.setPadding(dp(10),dp(10),dp(10),dp(10));host.addView(e,new LinearLayout.LayoutParams(-1,-2));return e;}
 private Spinner choice(String title,String[] options){text(content,title,14,muted,true);Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,options));content.addView(s,new LinearLayout.LayoutParams(-1,dp(52)));return s;}
 private void date(Button b,boolean first){LocalDate d=first?start:end;new DatePickerDialog(this,(picker,y,m,day)->{LocalDate chosen=LocalDate.of(y,m+1,day);if(first){start=chosen;if(end.isBefore(start))end=start;}else end=chosen;departureButton.setText("Departure: "+start);returnButton.setText("Return: "+end);},d.getYear(),d.getMonthValue()-1,d.getDayOfMonth()).show();}
 private void form(){ form(null); }
 private void form(JSONObject draft){
  clear("form");current=null;draftPass=draft==null?"":draft.optString("boardingPass");
  if(draft!=null){start=LocalDate.parse(draft.optString("start"));end=LocalDate.parse(draft.optString("end"));}
  text(content,"01 / PLAN YOUR GETAWAY",12,green,true);
  text(content,"Every trip.\nYour kind of packing.",30,ink,true);
  text(content,"Tell us who’s coming and how you’re travelling. We’ll handle the checklist.",16,muted,false);
  text(content,"Destination",14,muted,true);
  destInput=input(content,"e.g. Goa",draft==null?"":draft.optString("destination"));
  departureButton=button(content,"Departure: "+start,()->date(departureButton,true));
  returnButton=button(content,"Return: "+end,()->date(returnButton,false));
  gap();text(content,"02 / YOUR TRAVEL DETAILS",12,green,true);
  peopleInput=input(content,"Number of travellers (1–12)",draft==null?"1":draft.optString("people","1"));peopleInput.setInputType(2);
  text(content,"Quantities cover the whole group. Shared items are suggested separately.",13,muted,false);
  purposeInput=choice("Trip style",new String[]{"Leisure","Beach","Business","Adventure"});
  weatherInput=choice("Expected weather",new String[]{"Mild","Warm","Cold","Rainy"});
  transportInput=choice("How are you travelling?",new String[]{"Flight","Train","Bus","Car"});
  gap();text(content,"03 / YOUR STAY",12,green,true);
  stayInput=choice("Accommodation",new String[]{"Hotel","Hostel","Apartment","Camping","Other"});
  text(content,"Hotel / accommodation name (optional)",14,muted,true);
  hotelInput=new AutoCompleteTextView(this);hotelInput.setHint("Type a name or choose a previous stay");hotelInput.setSingleLine(true);hotelInput.setThreshold(0);
  String saved=getPreferences(MODE_PRIVATE).getString("hotels", "[]");
  try{JSONArray names=new JSONArray(saved);String[] options=new String[names.length()];for(int i=0;i<names.length();i++)options[i]=names.optString(i);hotelInput.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,options));}catch(JSONException ignored){}
  content.addView(hotelInput,new LinearLayout.LayoutParams(-1,dp(56)));hotelInput.setOnClickListener(v->hotelInput.showDropDown());
  text(content,"Your saved stays appear here. No live hotel search or booking.",13,muted,false);
  laundryInput=toggle("Laundry available",false);internationalInput=toggle("International trip",false);
  toiletriesInput=toggle("Stay provides soap and shampoo",true);towelsInput=toggle("Stay provides towels",true);
  final int[] lastStay={draft==null?0:java.util.Arrays.asList("Hotel","Hostel","Apartment","Camping","Other").indexOf(draft.optString("stay"))};
  stayInput.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> a){}public void onItemSelected(android.widget.AdapterView<?> a,android.view.View v,int position,long id){if(position==3&&position!=lastStay[0]){toiletriesInput.setChecked(false);towelsInput.setChecked(false);}lastStay[0]=position;}});
  if(draft!=null){select(purposeInput,draft.optString("purpose"));select(weatherInput,draft.optString("weather"));select(transportInput,draft.optString("transport"));select(stayInput,draft.optString("stay"));hotelInput.setText(draft.optString("hotel"));laundryInput.setChecked(draft.optBoolean("laundry"));internationalInput.setChecked(draft.optBoolean("international"));toiletriesInput.setChecked(draft.optBoolean("toiletries"));towelsInput.setChecked(draft.optBoolean("towels"));}
  gap();text(content,"04 / TRAVEL DOCUMENT",12,green,true);
  passStatus=text(content,draftPass.isEmpty()?"No boarding-pass image attached":"Boarding-pass image attached",14,muted,false);
  button(content,"Attach boarding-pass image",()->pickPass());
  text(content,"Optional. Image stays on your device; it is not uploaded or scanned.",13,muted,false);
  button(content,"Create my packing plan",()->{
   if(destInput.getText().toString().trim().isEmpty()){destInput.setError("Enter a destination");return;}
   int people;try{people=Integer.parseInt(peopleInput.getText().toString());if(people<1||people>12)throw new NumberFormatException();}catch(NumberFormatException e){peopleInput.setError("Enter 1–12 travellers");return;}
   long days=ChronoUnit.DAYS.between(start,end)+1;
   if(days<1||days>90){new AlertDialog.Builder(this).setMessage("Choose a return date on or after departure; maximum 90 days.").setPositiveButton("OK",null).show();return;}
   try{current=readDraft().put("id",System.currentTimeMillis()).put("people",people);
    current.put("items",PackingRules.generate((int)days,current.getString("purpose"),current.getString("weather"),current.getBoolean("laundry"),current.getBoolean("international"),people,current.getString("transport"),current.getString("stay"),current.getBoolean("toiletries"),current.getBoolean("towels")));
    String name=hotelInput.getText().toString().trim();if(!name.isEmpty()){JSONArray names=new JSONArray(getPreferences(MODE_PRIVATE).getString("hotels","[]"));boolean exists=false;for(int i=0;i<names.length();i++)if(name.equalsIgnoreCase(names.optString(i)))exists=true;if(!exists)names.put(name);getPreferences(MODE_PRIVATE).edit().putString("hotels",names.toString()).apply();}
    persist();showTrip();
   }catch(Exception e){error(e);}
  });button(content,"Back to trips",()->home());
 }
 private CheckBox toggle(String label,boolean checked){CheckBox c=new CheckBox(this);c.setText(label);c.setChecked(checked);c.setMinHeight(dp(48));content.addView(c);return c;}
 private void select(Spinner spinner,String value){for(int i=0;i<spinner.getCount();i++)if(spinner.getItemAtPosition(i).equals(value))spinner.setSelection(i);}
 private JSONObject readDraft() throws JSONException{return new JSONObject().put("destination",destInput.getText().toString().trim()).put("start",start.toString()).put("end",end.toString()).put("people",peopleInput.getText().toString()).put("purpose",purposeInput.getSelectedItem()).put("weather",weatherInput.getSelectedItem()).put("transport",transportInput.getSelectedItem()).put("stay",stayInput.getSelectedItem()).put("hotel",hotelInput.getText().toString().trim()).put("laundry",laundryInput.isChecked()).put("international",internationalInput.isChecked()).put("toiletries",toiletriesInput.isChecked()).put("towels",towelsInput.isChecked()).put("boardingPass",draftPass);}
 private void pickPass(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,42);}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=42||result!=RESULT_OK||data==null||data.getData()==null)return;try{Uri uri=data.getData();getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);if(screen.equals("trip")&&current!=null){current.put("boardingPass",uri.toString());persist();showTrip();}else if(screen.equals("form")){draftPass=uri.toString();passStatus.setText("Boarding-pass image attached");}}catch(Exception e){error(e);}}
 private void documentCard(){LinearLayout c=card();text(c,"Boarding pass",18,ink,true);String pass=current.optString("boardingPass");if(pass.isEmpty()){text(c,"Keep an image handy for this trip.",14,muted,false);}else{
  ImageView image=new ImageView(this);image.setAdjustViewBounds(true);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setContentDescription("Attached boarding-pass image");c.addView(image,new LinearLayout.LayoutParams(-1,dp(200)));text(c,"Saved image · tap to enlarge",13,muted,false);
  io.execute(()->{try{Uri uri=Uri.parse(pass);BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;try(InputStream in=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,options);}options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>1600)options.inSampleSize*=2;options.inJustDecodeBounds=false;Bitmap bitmap;try(InputStream in=getContentResolver().openInputStream(uri)){bitmap=BitmapFactory.decodeStream(in,null,options);}runOnUiThread(()->{if(bitmap!=null)image.setImageBitmap(bitmap);else image.setContentDescription("Cannot preview this image; replace the attachment");});}catch(Exception e){runOnUiThread(()->{image.setVisibility(View.GONE);text(c,"Image is no longer accessible. Please attach it again.",14,muted,false);});}});
  image.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(pass),"image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));}catch(Exception e){error(e);}});
  button(c,"Remove attachment",()->{try{current.put("boardingPass","");persist();showTrip();}catch(Exception e){error(e);}});
 }button(c,pass.isEmpty()?"Attach image":"Replace image",()->pickPass());}
 private void persist(){if(current==null)return;String snapshot=current.toString();io.execute(()->{try{store.save(new JSONObject(snapshot));}catch(Exception e){runOnUiThread(()->error(e));}});}
 private void showTrip(){clear("trip");text(content,"YOUR PACKING PLAN",12,green,true);text(content,current.optString("destination"),34,ink,true);text(content,current.optString("start")+" → "+current.optString("end"),15,muted,false);text(content,current.optString("purpose")+" · "+current.optString("weather")+" · "+current.optString("transport","Travel"),15,muted,false);
  LinearLayout stay=card();text(stay,current.optInt("people",1)+" traveller(s) · "+current.optString("stay","Hotel"),18,ink,true);text(stay,current.optString("hotel").isEmpty()?"Accommodation not specified":current.optString("hotel"),15,muted,false);text(stay,"This checklist covers the whole group. Adjust quantities to suit your bags.",13,muted,false);documentCard();JSONArray items=current.optJSONArray("items");int packed=0;for(int i=0;i<items.length();i++)if(items.optJSONObject(i).optBoolean("packed"))packed++;
  LinearLayout summary=card();text(summary,packed==items.length()&&packed>0?"You’re ready to go!": "A little closer to takeoff",20,green,true);text(summary,packed+" of "+items.length()+" items packed",15,muted,false);ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(Math.max(1,items.length()));progress.setProgress(packed);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(green));summary.addView(progress,new LinearLayout.LayoutParams(-1,dp(12)));
  button(content,"+ Add your own item",()->edit(null));gap();
  for(String category:new String[]{"Documents","Clothing","Essentials","Electronics","Activities","Custom"}){boolean heading=false;for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);if(!category.equals(item.optString("category")))continue;if(!heading){text(content,category.toUpperCase(java.util.Locale.ROOT),13,green,true);heading=true;}LinearLayout c=card();CheckBox check=new CheckBox(this);check.setText(item.optString("name")+" × "+item.optInt("quantity",1));check.setTextSize(17);check.setTextColor(ink);check.setChecked(item.optBoolean("packed"));c.addView(check);check.setOnCheckedChangeListener((v,on)->{try{item.put("packed",on);persist();int y=findViewById(R.id.scroll).getScrollY();showTrip();findViewById(R.id.scroll).post(()->findViewById(R.id.scroll).scrollTo(0,y));}catch(Exception e){error(e);}});TextView why=text(c,"Why pack this?",14,green,true);why.setContentDescription("Why pack "+item.optString("name"));why.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(item.optString("name")).setMessage(item.optString("why")).setPositiveButton("Got it",null).show());TextView modify=text(c,"Edit / remove",13,muted,false);modify.setContentDescription("Edit or remove "+item.optString("name"));modify.setOnClickListener(v->edit(item));}}
  button(content,"Back to trips",()->home());Button delete=button(content,"Delete this trip",()->new AlertDialog.Builder(this).setTitle("Delete this trip?").setMessage("This removes the trip and its packing checklist.").setNegativeButton("Keep trip",null).setPositiveButton("Delete",(d,w)->{long id=current.optLong("id");io.execute(()->{store.delete(id);runOnUiThread(()->home());});}).show());delete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(143,65,52)));
 }
 private void edit(JSONObject existing){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(8),dp(20),dp(8));EditText name=input(box,"Item name",existing==null?"":existing.optString("name"));EditText quantity=input(box,"Quantity",existing==null?"1":""+existing.optInt("quantity"));quantity.setInputType(2);AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(existing==null?"Add an item":"Edit item").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save",null);
  if(existing!=null)builder.setNeutralButton("Remove",(d,w)->{JSONArray a=current.optJSONArray("items");for(int i=0;i<a.length();i++)if(a.optJSONObject(i)==existing){a.remove(i);break;}persist();showTrip();});
  AlertDialog dialog=builder.create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{String n=name.getText().toString().trim();if(n.isEmpty()){name.setError("Enter an item name");return;}int q;try{q=Integer.parseInt(quantity.getText().toString());if(q<1||q>9999)throw new NumberFormatException();}catch(NumberFormatException e){quantity.setError("Use a number from 1 to 9999");return;}try{JSONObject item=existing==null?new JSONObject().put("category","Custom").put("why","Added by you for this trip.").put("packed",false):existing;item.put("name",n).put("quantity",q);if(existing==null)current.optJSONArray("items").put(item);persist();dialog.dismiss();showTrip();}catch(Exception e){error(e);}}));dialog.show();
 }
 // Legacy fallback; Android 13+ uses the platform callback registered in onCreate.
 @android.annotation.SuppressLint("GestureBackNavigation")
 @Override public void onBackPressed(){if(!screen.equals("home"))home();else super.onBackPressed();}
 @Override public void onDestroy(){super.onDestroy();io.shutdown();}
}
