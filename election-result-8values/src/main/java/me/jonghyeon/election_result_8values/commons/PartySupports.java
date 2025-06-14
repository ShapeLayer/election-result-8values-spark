package me.jonghyeon.election_result_8values.commons;

import me.jonghyeon.election_result_8values.models.Party;
import me.jonghyeon.election_result_8values.models.Struct8Values;

import java.util.HashMap;
import java.util.Map;

public class PartySupports {
  public static Map<Party, String> partyEnumToString = new HashMap<Party, String>() {{
    put(Party.DEMOCRATIC_PARTY_OF_KOREA, Presets.NAME_DEMOCRATIC_PARTY_OF_KOREA);
    put(Party.DEMOCRATIC_CITIZENS_PARTY, Presets.NAME_DEMOCRATIC_CITIZENS_PARTY);
    put(Party.UNITED_FUTURE_PARTY, Presets.NAME_UNITED_FUTURE_PARTY);
    put(Party.FUTURE_KOREA_PARTY, Presets.NAME_FUTURE_KOREA_PARTY);
    put(Party.MINSAENGDANG, Presets.NAME_MINSAENGDANG);
    put(Party.JUSTICE_PARTY, Presets.NAME_JUSTICE_PARTY);
    put(Party.OUR_REPUBLICAN_PARTY, Presets.NAME_OUR_REPUBLICAN_PARTY);
    put(Party.THE_PEOPLES_PARTY_OF_SOUTH_KOREA, Presets.NAME_THE_PEOPLES_PARTY_OF_SOUTH_KOREA);
    put(Party.KOREAN_ECONOMIC_PARTY, Presets.NAME_KOREAN_ECONOMIC_PARTY);
    put(Party.THE_PEOPLES_PARTY, Presets.NAME_THE_PEOPLES_PARTY);
    put(Party.PRO_PARK_NEW_PARTY, Presets.NAME_PRO_PARK_NEW_PARTY);
    put(Party.OPEN_DEMOCRATIC_PARTY, Presets.NAME_OPEN_DEMOCRATIC_PARTY);
  }};
  public static String toString(Party party) {
    return partyEnumToString.get(party);
  }
  public static Party[] parties = {
      Party.DEMOCRATIC_PARTY_OF_KOREA,
      Party.DEMOCRATIC_CITIZENS_PARTY,
      Party.UNITED_FUTURE_PARTY,
      Party.FUTURE_KOREA_PARTY,
      Party.MINSAENGDANG,
      Party.JUSTICE_PARTY,
      Party.OUR_REPUBLICAN_PARTY,
      Party.THE_PEOPLES_PARTY_OF_SOUTH_KOREA,
      Party.KOREAN_ECONOMIC_PARTY,
      Party.THE_PEOPLES_PARTY,
      Party.PRO_PARK_NEW_PARTY,
      Party.OPEN_DEMOCRATIC_PARTY,
  };
  public static String[] getTotalString() {
    return partyEnumToString.values().toArray(new String[0]);
  }

  public static Map<Party, Struct8Values> partyEnumTo8Values = new HashMap<Party, Struct8Values>() {{
    put(Party.DEMOCRATIC_PARTY_OF_KOREA, Presets.VALUES_DEMOCRATIC_PARTY_OF_KOREA);
    put(Party.UNITED_FUTURE_PARTY, Presets.VALUES_UNITED_FUTURE_PARTY);
    put(Party.MINSAENGDANG, Presets.VALUES_MINSAENGDANG);
    put(Party.JUSTICE_PARTY, Presets.VALUES_JUSTICE_PARTY);
    put(Party.OUR_REPUBLICAN_PARTY, Presets.VALUES_OUR_REPUBLICAN_PARTY);
    put(Party.THE_PEOPLES_PARTY_OF_SOUTH_KOREA, Presets.VALUES_THE_PEOPLES_PARTY_OF_SOUTH_KOREA);
    put(Party.KOREAN_ECONOMIC_PARTY, Presets.VALUES_KOREAN_ECONOMIC_PARTY);
    put(Party.THE_PEOPLES_PARTY, Presets.VALUES_THE_PEOPLES_PARTY);
    put(Party.PRO_PARK_NEW_PARTY, Presets.VALUES_PRO_PARK_NEW_PARTY);
    put(Party.OPEN_DEMOCRATIC_PARTY, Presets.VALUES_OPEN_DEMOCRATIC_PARTY);
  }};
  
  public static Struct8Values to8Values(Party party) {
    return partyEnumTo8Values.get(party);
  }
}
