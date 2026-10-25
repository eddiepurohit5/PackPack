package com.packproof.app;
import org.json.*;
public final class PackingRules {
 private static void add(JSONArray a,String name,int quantity,String category,String why) throws JSONException { a.put(new JSONObject().put("name",name).put("quantity",quantity).put("category",category).put("why",why).put("packed",false)); }
 public static JSONArray generate(int days,String purpose,String weather,boolean laundry,boolean international) throws JSONException {
  if(days < 1 || days > 90) throw new IllegalArgumentException("Trip must be 1–90 days");
  JSONArray a=new JSONArray(); int clothes=laundry?Math.min(days,3):days;
  add(a,"Tops",clothes,"Clothing",laundry?"Laundry access lets you reuse clothing after three days.":"One top for each day of your trip.");
  add(a,"Underwear",laundry?Math.min(days,4):days,"Clothing","Daily essentials; quantity reflects your laundry choice.");
  add(a,"Bottoms",Math.max(1,(clothes+1)/2),"Clothing","One pair for approximately every two days between washes.");
  add(a,"Sleepwear",1,"Clothing","One reusable sleepwear set for the trip.");
  add(a,"Toothbrush & toothpaste",1,"Essentials","Basic personal care for every trip.");
  add(a,"Phone charger",1,"Electronics","Keep your phone charged while travelling.");
  add(a,"ID & booking details",1,"Documents","Useful for travel check-ins and accommodation.");
  add(a,"Personal medicines",1,"Essentials","Pack your usual medicines in the quantity you personally need.");
  if(international){ add(a,"Passport & entry documents",1,"Documents","You selected international travel. Check your destination's requirements."); add(a,"Compatible travel adapter",1,"Electronics","You selected international travel; check the destination's socket type."); }
  if(purpose.equals("Beach")){add(a,"Swimwear",2,"Activities","You selected a beach trip.");add(a,"Sunscreen",1,"Activities","You selected a beach trip.");add(a,"Sandals",1,"Activities","Useful for your beach trip.");}
  if(purpose.equals("Business")){add(a,"Formal outfit",Math.min(days,2),"Activities","You selected a business trip.");add(a,"Laptop & charger",1,"Electronics","You selected a business trip.");}
  if(purpose.equals("Adventure")){add(a,"Walking shoes",1,"Activities","You selected an adventure trip.");add(a,"Daypack & water bottle",1,"Activities","Useful for carrying essentials on outings.");}
  if(weather.equals("Cold")){add(a,"Warm jacket",1,"Clothing","You selected cold weather.");add(a,"Warm layers",2,"Clothing","You selected cold weather; layers help adjust to temperature changes.");}
  if(weather.equals("Rainy")){add(a,"Rain jacket",1,"Clothing","You selected rainy weather.");add(a,"Waterproof pouch",1,"Essentials","You selected rainy weather; protect small belongings.");}
  if(weather.equals("Warm"))add(a,"Sun hat",1,"Activities","You selected warm weather.");
  return a;
 }
 public static JSONArray generate(int days, String purpose, String weather, boolean laundry,
   boolean international, int people, String transport, String stay, boolean toiletries, boolean towels) throws JSONException {
  if(people < 1 || people > 12) throw new IllegalArgumentException("Choose 1–12 travellers");
  JSONArray a=generate(days,purpose,weather,laundry,international);
  for(int i=0;i<a.length();i++) {
   JSONObject item=a.getJSONObject(i);
   boolean shared=item.getString("name").equals("Sunscreen") || item.getString("name").equals("Compatible travel adapter");
   if(!shared) item.put("quantity",item.getInt("quantity")*people);
   item.put("why",item.getString("why")+(shared?" Shared group item; adjust for your group.":" Quantity is for "+people+" traveller(s) combined."));
  }
  add(a,"Reusable water bottle",people,"Essentials","One per traveller to use during the journey.");
  add(a,"Travel snacks",people,"Essentials","One snack pack per traveller; adjust for the journey length.");
  if(!toiletries) add(a,"Soap & shampoo kit",people,"Essentials","Your stay does not provide toiletries. One kit per traveller.");
  if(!towels) add(a,"Travel towel",people,"Essentials","Your stay does not provide towels. One per traveller.");
  if(transport.equals("Flight")) {
   add(a,"Boarding pass",people,"Documents","One per traveller. Attach your boarding-pass image to the trip below.");
   add(a,"Cabin liquids pouch",people,"Essentials","For air travel. Check your airline and airport liquid restrictions.");
   add(a,"Luggage identification tag",people,"Essentials","One per traveller; adjust for the number of bags.");
  } else if(transport.equals("Train") || transport.equals("Bus")) {
   add(a,"Journey ticket",people,"Documents","You selected "+transport.toLowerCase(java.util.Locale.ROOT)+" travel. Keep each traveller’s ticket accessible.");
   add(a,"Power bank",Math.max(1,(people+1)/2),"Electronics","A shared backup for a long ground journey; adjust for your devices.");
  } else if(transport.equals("Car")) {
   add(a,"Driving licence & vehicle documents",1,"Documents","Shared reminder for the driver on your road trip.");
   add(a,"Car charger",1,"Electronics","One shared charger for the car.");
  }
  if(stay.equals("Camping")) {
   add(a,"Tent",Math.max(1,(people+1)/2),"Activities","Assumes two travellers per tent. Edit to match your tent capacity.");
   add(a,"Sleeping bag",people,"Activities","One per traveller for camping.");
   add(a,"Torch",people,"Electronics","One per traveller for moving around after dark.");
  } else if(stay.equals("Hostel")) {
   add(a,"Locker padlock",people,"Essentials","One per traveller for hostel lockers; check the hostel’s setup.");
   add(a,"Earplugs",people,"Essentials","Useful for shared hostel rooms.");
  }
  if(days>=7) add(a,"Laundry bag",people,"Essentials","Separate used clothing on trips lasting a week or longer.");
  return a;
 }

}
