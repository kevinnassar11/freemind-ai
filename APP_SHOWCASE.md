# FreeMind AI — App Showcase

FreeMind AI is an Android travel-planning app that turns a plain sentence like "3 days in Beirut, food and sights" into a real, editable itinerary. It does this without any AI model: the plan is built from live Google Places data, and a written overview of the whole trip is shown above the day-by-day list.

You can search by free prompt or switch to Cities, Countries, Places, Food, Culture or Shopping modes, each with its own filters, and use "Near me" chips (such as open now) with distances from your location. Plans are fully editable: swap or move stops, undo changes, add your own stops with notes, keep a notes-and-checklist sheet, pick travel dates to see live weather from Open-Meteo, and tap "Match weather" to reshuffle days to fit the forecast. Every place opens a details sheet with photo, hours, call, website and directions. Plans can be shared as text or a PDF with Google Maps route links, saved, and revisited from History; favorites can be filed into named lists. Plans, place details and photos work offline, plans sync to your account through Supabase (with per-user access rules), and local trip reminders are scheduled on the device. Settings cover light/dark/system theme, language (English, Arabic with right-to-left layout, French), reminders, history on/off, CSV export, sign out and account deletion.

## Known limitations

- The exported PDF is generated and the share sheet opens, but its page layout has not been visually checked.
- Arabic and French translations have not been reviewed by a native speaker.
- Everything was tested on the emulator, not on a release build on a real phone.
- Map tiles are not available offline (plan text, details and photos are).
- Prompts written in Arabic or French are not parsed yet; only the interface is translated.
- Server-side push notifications are not deployed; only local reminders work.
- Stops are reordered with Move up / Move down in the stop menu; drag-and-drop handles are not built yet.

---

## 1. Search

| | |
|---|---|
| ![Home, prompt mode](screenshots/01_home_prompt_mode.png) **Home — prompt mode** | ![Home with near-me chips](screenshots/02_home_near_me_chips.png) **Home — near-me chips** |
| ![Near me, open now](screenshots/03_near_me_open_now.png) **Near me, open now** (with distances) | ![Filters active](screenshots/04_filters_active.png) **Search results with filters** |
| ![No results](screenshots/05_no_results_match_filters.png) **Empty state when filters match nothing** | ![Cities mode](screenshots/06_cities_mode.png) **Cities mode** |
| ![Countries mode](screenshots/07_countries_mode.png) **Countries mode** | ![Places mode](screenshots/08_places_mode.png) **Places mode** |
| ![Food mode](screenshots/09_food_mode.png) **Food mode** | ![Culture mode](screenshots/10_culture_mode.png) **Culture mode** |
| ![Shopping mode](screenshots/11_shopping_mode.png) **Shopping mode** | |

## 2. Trip plan

| | |
|---|---|
| ![Plan with overview](screenshots/12_trip_plan_with_overview.png) **Trip plan with written overview** (drag the handle to resize the sheet) | ![Day list](screenshots/13_trip_plan_day_list.png) **Day-by-day list** (the quick-action chips fold away while scrolling) |
| ![Day selected](screenshots/14_day_selected_map_zoom.png) **Day selected, map zooms to its stops** | ![Stop menu](screenshots/15_stop_menu.png) **Stop menu** (move up/down, move to day, swap, note, remove) |
| ![Add stop dialog](screenshots/16_add_stop_dialog.png) **Add a stop** (pick a time of day) | ![Add stop results](screenshots/17_add_stop_results.png) **Add-stop search results** |
| ![Custom stop added](screenshots/18_custom_stop_added.png) **Custom stop added** (placed in time order, labelled Afternoon · Added by you) | ![Stop note dialog](screenshots/19_stop_note_dialog.png) **Stop note dialog** |
| ![Stop with note](screenshots/20_stop_with_note.png) **Stop with note** | ![Date picker](screenshots/21_date_picker.png) **Date picker** |
| ![Dates and weather](screenshots/22_dates_and_weather.png) **Dates and live weather** | ![Match weather](screenshots/23_match_weather_result.png) **Match weather result** |
| ![Notes and checklist](screenshots/24_notes_and_checklist.png) **Trip notes and checklist** | |

## 3. Place details, sharing and saving

| | |
|---|---|
| ![Place details](screenshots/25_place_details.png) **Place details** | ![Place hours](screenshots/26_place_details_hours.png) **Place details — today's hours highlighted, phone number once** |
| ![Share menu](screenshots/27_share_menu.png) **Share menu** | ![Share as PDF](screenshots/28_share_as_pdf.png) **Share as PDF** |
| ![Plan saved](screenshots/29_plan_saved.png) **Plan saved** | ![History](screenshots/30_history.png) **History** |

Sample PDF export: [sample_exported_trip_plan.pdf](screenshots/sample_exported_trip_plan.pdf)

## 4. Favorites and lists

| | |
|---|---|
| ![Favorites](screenshots/31_favorites_places.png) **Favorite places** | ![New list](screenshots/32_new_list_dialog.png) **New list dialog** |
| ![List created](screenshots/33_list_created.png) **List created** | ![File under list](screenshots/34_file_under_list_dialog.png) **File a favorite under a list** |
| ![List filter](screenshots/35_list_filter_rename_delete.png) **Filter by list, rename or delete** | ![Saved plans](screenshots/36_favorites_saved_plans.png) **Saved plans** |

## 5. Settings

| | |
|---|---|
| ![Settings top](screenshots/37_settings_top.png) **Settings — top** | ![Settings bottom](screenshots/38_settings_bottom.png) **Settings — bottom** (tappable rows with icons, separate danger zone) |
| ![Delete account confirmation](screenshots/38b_delete_account_confirm.png) **Delete account — type DELETE to confirm** | |

## 6. Dark mode

| | | |
|---|---|---|
| ![Dark plan](screenshots/39_dark_mode_plan.png) **Plan** | ![Dark favorites](screenshots/40_dark_mode_favorites.png) **Favorites** | ![Dark settings](screenshots/41_dark_mode_settings.png) **Settings** |

## 7. Languages

**Arabic (right-to-left)**

| | | | |
|---|---|---|---|
| ![Arabic plan day](screenshots/42_arabic_rtl_plan_day.png) **Plan day** | ![Arabic overview](screenshots/43_arabic_rtl_plan_overview.png) **Overview** | ![Arabic favorites](screenshots/44_arabic_rtl_favorites.png) **Favorites** | ![Arabic settings](screenshots/45_arabic_rtl_settings.png) **Settings** |

**French**

| | |
|---|---|
| ![French overview](screenshots/46_french_plan_overview.png) **Plan overview** | ![French settings](screenshots/47_french_settings.png) **Settings** |
