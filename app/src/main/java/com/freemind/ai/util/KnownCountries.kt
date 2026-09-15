package com.freemind.ai.util

/**
 * Every UN member/observer state's common name, lowercased, plus a few widely-used
 * short forms - used to tell "Lebanon" (a country) apart from "Beirut" (a city).
 *
 * This can't be done by asking the Places API "what type is this?", because Places
 * Text Search is a business/POI index, not a geocoder: searching the bare word
 * "Lebanon" doesn't surface the country at all, just a small town in Mount Lebanon
 * that happens to share the name - so classifying by Places `types` alone would let
 * exactly the input this feature exists to reject sail through as a "locality".
 */
private val COUNTRY_NAMES: Set<String> = setOf(
    "afghanistan", "albania", "algeria", "andorra", "angola", "antigua and barbuda",
    "argentina", "armenia", "australia", "austria", "azerbaijan", "bahamas", "bahrain",
    "bangladesh", "barbados", "belarus", "belgium", "belize", "benin", "bhutan",
    "bolivia", "bosnia and herzegovina", "botswana", "brazil", "brunei", "bulgaria",
    "burkina faso", "burundi", "cabo verde", "cambodia", "cameroon", "canada",
    "central african republic", "chad", "chile", "china", "colombia", "comoros",
    "congo", "democratic republic of the congo", "costa rica", "croatia", "cuba",
    "cyprus", "czechia", "czech republic", "denmark", "djibouti", "dominica",
    "dominican republic", "ecuador", "egypt", "el salvador", "equatorial guinea",
    "eritrea", "estonia", "eswatini", "ethiopia", "fiji", "finland", "france",
    "gabon", "gambia", "georgia", "germany", "ghana", "greece", "grenada",
    "guatemala", "guinea", "guinea-bissau", "guyana", "haiti", "honduras",
    "hungary", "iceland", "india", "indonesia", "iran", "iraq", "ireland",
    "israel", "italy", "jamaica", "japan", "jordan", "kazakhstan", "kenya",
    "kiribati", "kosovo", "kuwait", "kyrgyzstan", "laos", "latvia", "lebanon",
    "lesotho", "liberia", "libya", "liechtenstein", "lithuania", "luxembourg",
    "madagascar", "malawi", "malaysia", "maldives", "mali", "malta",
    "marshall islands", "mauritania", "mauritius", "mexico", "micronesia",
    "moldova", "monaco", "mongolia", "montenegro", "morocco", "mozambique",
    "myanmar", "namibia", "nauru", "nepal", "netherlands", "new zealand",
    "nicaragua", "niger", "nigeria", "north korea", "north macedonia", "norway",
    "oman", "pakistan", "palau", "palestine", "panama", "papua new guinea",
    "paraguay", "peru", "philippines", "poland", "portugal", "qatar", "romania",
    "russia", "rwanda", "saint kitts and nevis", "saint lucia",
    "saint vincent and the grenadines", "samoa", "san marino",
    "sao tome and principe", "saudi arabia", "senegal", "serbia", "seychelles",
    "sierra leone", "singapore", "slovakia", "slovenia", "solomon islands",
    "somalia", "south africa", "south korea", "south sudan", "spain",
    "sri lanka", "sudan", "suriname", "sweden", "switzerland", "syria",
    "taiwan", "tajikistan", "tanzania", "thailand", "timor-leste", "togo",
    "tonga", "trinidad and tobago", "tunisia", "turkey", "turkmenistan",
    "tuvalu", "uganda", "ukraine", "united arab emirates", "united kingdom",
    "united states", "uruguay", "uzbekistan", "vanuatu", "vatican city",
    "venezuela", "vietnam", "yemen", "zambia", "zimbabwe",
    // Common short forms / aliases.
    "usa", "us", "u.s.", "u.s.a.", "uk", "u.k.", "uae", "ivory coast",
    "cote d'ivoire", "côte d'ivoire", "turkiye", "türkiye", "burma", "holland",
    "macedonia",
)

fun isKnownCountryName(query: String): Boolean = COUNTRY_NAMES.contains(query.trim().lowercase())
