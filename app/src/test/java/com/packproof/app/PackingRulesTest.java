package com.packproof.app;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
public class PackingRulesTest {
 private JSONObject find(JSONArray a,String name){for(int i=0;i<a.length();i++)if(a.optJSONObject(i).optString("name").equals(name))return a.optJSONObject(i);return null;}
 @Test public void laundryCapsClothing() throws Exception {assertEquals(3,find(PackingRules.generate(14,"Leisure","Mild",true,false),"Tops").getInt("quantity"));assertEquals(14,find(PackingRules.generate(14,"Leisure","Mild",false,false),"Tops").getInt("quantity"));}
 @Test public void internationalDocumentsAreConditional() throws Exception {assertNull(find(PackingRules.generate(2,"Leisure","Mild",false,false),"Passport & entry documents"));assertNotNull(find(PackingRules.generate(2,"Leisure","Mild",false,true),"Passport & entry documents"));}
 @Test public void beachAndWeatherRulesCombine() throws Exception {JSONArray a=PackingRules.generate(5,"Beach","Rainy",false,false);assertNotNull(find(a,"Swimwear"));assertNotNull(find(a,"Rain jacket"));assertNull(find(a,"Laptop & charger"));}
 @Test public void everyRuleHasReasonAndPositiveQuantity() throws Exception {for(String p:new String[]{"Leisure","Business","Beach","Adventure"})for(String w:new String[]{"Mild","Warm","Cold","Rainy"})for(int days:new int[]{1,5,90}){JSONArray a=PackingRules.generate(days,p,w,true,true);for(int i=0;i<a.length();i++){JSONObject item=a.getJSONObject(i);assertTrue(item.getInt("quantity")>0);assertFalse(item.getString("why").isEmpty());assertFalse(item.getBoolean("packed"));}}}

 @Test public void groupQuantitiesSeparateSharedItems() throws Exception {
  JSONArray a=PackingRules.generate(5,"Beach","Warm",false,false,3,"Flight","Hotel",true,true);
  assertEquals(15,find(a,"Tops").getInt("quantity"));
  assertEquals(3,find(a,"Boarding pass").getInt("quantity"));
  assertEquals(1,find(a,"Sunscreen").getInt("quantity"));
  assertNull(find(a,"Travel towel"));
 }
 @Test public void campingAndMissingAmenitiesAddEquipment() throws Exception {
  JSONArray a=PackingRules.generate(8,"Adventure","Cold",true,false,5,"Car","Camping",false,false);
  assertEquals(3,find(a,"Tent").getInt("quantity"));assertEquals(5,find(a,"Sleeping bag").getInt("quantity"));
  assertEquals(5,find(a,"Travel towel").getInt("quantity"));assertEquals(1,find(a,"Car charger").getInt("quantity"));
  assertNotNull(find(a,"Laundry bag"));assertNull(find(a,"Boarding pass"));
 }
 @Test public void hostelGroundTravelHasRelevantItems() throws Exception {
  JSONArray a=PackingRules.generate(2,"Business","Rainy",false,true,2,"Train","Hostel",false,true);
  assertNotNull(find(a,"Journey ticket"));assertNotNull(find(a,"Locker padlock"));
  assertNotNull(find(a,"Soap & shampoo kit"));assertNull(find(a,"Tent"));assertNull(find(a,"Travel towel"));
 }
 @Test(expected=IllegalArgumentException.class) public void invalidGroupRejected() throws Exception {PackingRules.generate(2,"Leisure","Mild",false,false,0,"Flight","Hotel",true,true);}
 @Test(expected=IllegalArgumentException.class) public void invalidDurationRejected() throws Exception {PackingRules.generate(0,"Leisure","Mild",false,false);}
}
