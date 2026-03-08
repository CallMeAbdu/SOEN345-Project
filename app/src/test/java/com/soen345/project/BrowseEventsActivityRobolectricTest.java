package com.soen345.project;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.soen345.project.auth.AuthCallback;
import com.soen345.project.auth.AuthRepository;
import com.soen345.project.auth.AuthService;
import com.soen345.project.auth.AuthServiceProvider;
import com.soen345.project.auth.UserRole;
import com.soen345.project.event.Event;
import com.soen345.project.event.EventActionCallback;
import com.soen345.project.event.EventListCallback;
import com.soen345.project.event.EventListenerHandle;
import com.soen345.project.event.EventRepository;
import com.soen345.project.event.EventService;
import com.soen345.project.event.EventServiceProvider;
import com.soen345.project.event.EventStatus;
import com.soen345.project.reservation.Reservation;
import com.soen345.project.reservation.ReservationRepository;
import com.soen345.project.reservation.ReservationService;
import com.soen345.project.reservation.ReservationServiceProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class BrowseEventsActivityRobolectricTest {

    private FakeEventRepository eventRepository;
    private FakeAuthRepository authRepository;
    private FakeReservationRepository reservationRepository;
    private long pastMillis;
    private long futureMillis;

    @Before
    public void setUp() {
        eventRepository = new FakeEventRepository();
        authRepository = new FakeAuthRepository();
        reservationRepository = new FakeReservationRepository();
        
        AuthServiceProvider.setAuthServiceForTesting(new AuthService(authRepository));
        EventServiceProvider.setEventServiceForTesting(new EventService(eventRepository));
        ReservationServiceProvider.setReservationService(new ReservationService(reservationRepository, eventRepository));
        
        pastMillis   = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L;
        futureMillis = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
    }

    @After
    public void tearDown() {
        AuthServiceProvider.clearAuthServiceForTesting();
        EventServiceProvider.clearEventServiceForTesting();
        ReservationServiceProvider.clearReservationService();
    }

    @Test
    public void activeEvent_showsReserveButton() {
        eventRepository.add(event("d1", "Active", EventStatus.ACTIVE, futureMillis, 100, 50));
        BrowseEventsActivity activity = launch();

        View card = container(activity).getChildAt(0);
        assertEquals(View.VISIBLE, card.findViewById(R.id.browseEventReserveButton).getVisibility());
    }

    @Test
    public void cancelledEvent_showsCancelledBadge_andHidesReserveButton() {
        eventRepository.add(event("d1", "Cancelled", EventStatus.CANCELLED, futureMillis, 100, 50));
        BrowseEventsActivity activity = launch();

        com.google.android.material.materialswitch.MaterialSwitch hideSwitch =
                activity.findViewById(R.id.browseHideCancelledSwitch);
        hideSwitch.setChecked(false);
        shadowOf(Looper.getMainLooper()).idle();

        View card = container(activity).getChildAt(0);
        assertNotNull(card);
        assertEquals(View.VISIBLE, card.findViewById(R.id.browseEventCancelledBadge).getVisibility());
        assertEquals(View.GONE,    card.findViewById(R.id.browseEventReserveButton).getVisibility());
    }

    @Test
    public void soldOutEvent_showsSoldOutBadge_andHidesReserveButton() {
        eventRepository.add(event("d1", "Sold Out", EventStatus.ACTIVE, futureMillis, 100, 0));
        BrowseEventsActivity activity = launch();

        View card = container(activity).getChildAt(0);
        assertEquals(View.VISIBLE, card.findViewById(R.id.browseEventSoldOutBadge).getVisibility());
        assertEquals(View.GONE,    card.findViewById(R.id.browseEventReserveButton).getVisibility());
    }

    @Test
    public void pastEvent_showsPastBadge_whenHidePastOff() {
        eventRepository.add(event("d1", "Past", EventStatus.ACTIVE, pastMillis, 100, 50));
        BrowseEventsActivity activity = launch();

        com.google.android.material.materialswitch.MaterialSwitch hidePastSwitch =
                activity.findViewById(R.id.browseHidePastSwitch);
        hidePastSwitch.setChecked(false);
        shadowOf(Looper.getMainLooper()).idle();

        View card = container(activity).getChildAt(0);
        assertNotNull(card);
        assertEquals(View.VISIBLE, card.findViewById(R.id.browseEventPastBadge).getVisibility());
        assertEquals(View.GONE,    card.findViewById(R.id.browseEventReserveButton).getVisibility());
    }

    @Test
    public void emptyList_showsEmptyText() {
        BrowseEventsActivity activity = launch();
        TextView emptyText = activity.findViewById(R.id.browseEventsEmptyText);
        assertEquals(View.VISIBLE, emptyText.getVisibility());
    }

    @Test
    public void eventDetails_showTitleCategoryLocationCapacity() {
        eventRepository.add(event("d1", "Jazz Night", EventStatus.ACTIVE, futureMillis, 200, 150));
        BrowseEventsActivity activity = launch();

        View card = container(activity).getChildAt(0);
        TextView title   = card.findViewById(R.id.browseEventItemTitle);
        TextView details = card.findViewById(R.id.browseEventItemDetails);

        assertEquals("Jazz Night", title.getText().toString());
        assertTrue(details.getText().toString().contains("150"));
        assertTrue(details.getText().toString().contains("200"));
    }

    @Test
    public void hidePastSwitch_on_excludesPastEvents() {
        eventRepository.add(event("d1", "Past",   EventStatus.ACTIVE, pastMillis,   100, 50));
        eventRepository.add(event("d2", "Future", EventStatus.ACTIVE, futureMillis, 100, 50));
        BrowseEventsActivity activity = launch();

        assertEquals(1, container(activity).getChildCount());
        TextView title = container(activity).getChildAt(0).findViewById(R.id.browseEventItemTitle);
        assertEquals("Future", title.getText().toString());
    }

    @Test
    public void hidePastSwitch_off_includesPastEvents() {
        eventRepository.add(event("d1", "Past",   EventStatus.ACTIVE, pastMillis,   100, 50));
        eventRepository.add(event("d2", "Future", EventStatus.ACTIVE, futureMillis, 100, 50));
        BrowseEventsActivity activity = launch();

        com.google.android.material.materialswitch.MaterialSwitch hidePastSwitch =
                activity.findViewById(R.id.browseHidePastSwitch);
        hidePastSwitch.setChecked(false);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(2, container(activity).getChildCount());
    }

    @Test
    public void hideSoldOutSwitch_on_excludesSoldOutEvents() {
        eventRepository.add(event("d1", "SoldOut", EventStatus.ACTIVE, futureMillis, 100, 0));
        eventRepository.add(event("d2", "Normal",  EventStatus.ACTIVE, futureMillis, 100, 50));
        BrowseEventsActivity activity = launch();

        com.google.android.material.materialswitch.MaterialSwitch hideSoldOut =
                activity.findViewById(R.id.browseHideSoldOutSwitch);
        hideSoldOut.setChecked(true);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(1, container(activity).getChildCount());
        TextView title = container(activity).getChildAt(0).findViewById(R.id.browseEventItemTitle);
        assertEquals("Normal", title.getText().toString());
    }

    @Test
    public void hideCancelledSwitch_on_excludesCancelledEvents() {
        eventRepository.add(event("d1", "Cancelled", EventStatus.CANCELLED, futureMillis, 100, 50));
        eventRepository.add(event("d2", "Active",    EventStatus.ACTIVE,    futureMillis, 100, 50));
        BrowseEventsActivity activity = launch();

        assertEquals(1, container(activity).getChildCount());
        TextView title = container(activity).getChildAt(0).findViewById(R.id.browseEventItemTitle);
        assertEquals("Active", title.getText().toString());
    }

    @Test
    public void categoryFilter_showsOnlyMatchingEvents() {
        eventRepository.add(event("d1", "Rock Concert",   EventStatus.ACTIVE, futureMillis, 100, 50, "Music",  "Montreal"));
        eventRepository.add(event("d2", "Tech Talk",      EventStatus.ACTIVE, futureMillis, 100, 50, "Tech",   "Montreal"));
        BrowseEventsActivity activity = launch();

        android.widget.EditText categoryInput = activity.findViewById(R.id.browseCategoryFilterInput);
        categoryInput.setText("Music");
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(1, container(activity).getChildCount());
        TextView title = container(activity).getChildAt(0).findViewById(R.id.browseEventItemTitle);
        assertEquals("Rock Concert", title.getText().toString());
    }

    @Test
    public void locationFilter_showsOnlyMatchingEvents() {
        eventRepository.add(event("d1", "Event A", EventStatus.ACTIVE, futureMillis, 100, 50, "Music", "Montreal"));
        eventRepository.add(event("d2", "Event B", EventStatus.ACTIVE, futureMillis, 100, 50, "Music", "Toronto"));
        BrowseEventsActivity activity = launch();

        android.widget.EditText locationInput = activity.findViewById(R.id.browseLocationFilterInput);
        locationInput.setText("Toronto");
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(1, container(activity).getChildCount());
        TextView title = container(activity).getChildAt(0).findViewById(R.id.browseEventItemTitle);
        assertEquals("Event B", title.getText().toString());
    }

    @Test
    public void categoryFilter_isCaseInsensitive() {
        eventRepository.add(event("d1", "Concert", EventStatus.ACTIVE, futureMillis, 100, 50, "MUSIC", "Montreal"));
        BrowseEventsActivity activity = launch();

        android.widget.EditText categoryInput = activity.findViewById(R.id.browseCategoryFilterInput);
        categoryInput.setText("music");
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(1, container(activity).getChildCount());
    }

    @Test
    public void clearFilters_resetsAllFilters() {
        eventRepository.add(event("d1", "Concert", EventStatus.ACTIVE, futureMillis, 100, 50, "Music", "Montreal"));
        eventRepository.add(event("d2", "Talk",    EventStatus.ACTIVE, futureMillis, 100, 50, "Tech",  "Toronto"));
        BrowseEventsActivity activity = launch();

        android.widget.EditText categoryInput = activity.findViewById(R.id.browseCategoryFilterInput);
        categoryInput.setText("Music");
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, container(activity).getChildCount());

        activity.findViewById(R.id.browseClearFiltersButton).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(2, container(activity).getChildCount());
        assertEquals("", categoryInput.getText().toString());
    }

    @Test
    public void sortByNameAsc_ordersAlphabetically() {
        eventRepository.add(event("d1", "Zebra Event", EventStatus.ACTIVE, futureMillis + 1000, 100, 50));
        eventRepository.add(event("d2", "Apple Event", EventStatus.ACTIVE, futureMillis + 2000, 100, 50));
        BrowseEventsActivity activity = launch();

        Spinner sortSpinner = activity.findViewById(R.id.browseSortSpinner);
        sortSpinner.setSelection(2);
        shadowOf(Looper.getMainLooper()).idle();

        TextView first  = container(activity).getChildAt(0).findViewById(R.id.browseEventItemTitle);
        TextView second = container(activity).getChildAt(1).findViewById(R.id.browseEventItemTitle);
        assertEquals("Apple Event", first.getText().toString());
        assertEquals("Zebra Event", second.getText().toString());
    }

    @Test
    public void sortByNameDesc_ordersReverseAlphabetically() {
        eventRepository.add(event("d1", "Apple Event", EventStatus.ACTIVE, futureMillis + 1000, 100, 50));
        eventRepository.add(event("d2", "Zebra Event", EventStatus.ACTIVE, futureMillis + 2000, 100, 50));
        BrowseEventsActivity activity = launch();

        Spinner sortSpinner = activity.findViewById(R.id.browseSortSpinner);
        sortSpinner.setSelection(3);
        shadowOf(Looper.getMainLooper()).idle();

        TextView first  = container(activity).getChildAt(0).findViewById(R.id.browseEventItemTitle);
        TextView second = container(activity).getChildAt(1).findViewById(R.id.browseEventItemTitle);
        assertEquals("Zebra Event", first.getText().toString());
        assertEquals("Apple Event", second.getText().toString());
    }

    @Test
    public void sortByDateAsc_ordersByEarliestFirst() {
        eventRepository.add(event("d1", "Later",   EventStatus.ACTIVE, futureMillis + 10000, 100, 50));
        eventRepository.add(event("d2", "Earlier", EventStatus.ACTIVE, futureMillis + 1000,  100, 50));
        BrowseEventsActivity activity = launch();

        Spinner sortSpinner = activity.findViewById(R.id.browseSortSpinner);
        sortSpinner.setSelection(0);
        shadowOf(Looper.getMainLooper()).idle();

        TextView first = container(activity).getChildAt(0).findViewById(R.id.browseEventItemTitle);
        assertEquals("Earlier", first.getText().toString());
    }

    @Test
    public void sortByDateDesc_ordersByLatestFirst() {
        eventRepository.add(event("d1", "Earlier", EventStatus.ACTIVE, futureMillis + 1000,  100, 50));
        eventRepository.add(event("d2", "Later",   EventStatus.ACTIVE, futureMillis + 10000, 100, 50));
        BrowseEventsActivity activity = launch();

        Spinner sortSpinner = activity.findViewById(R.id.browseSortSpinner);
        sortSpinner.setSelection(1);
        shadowOf(Looper.getMainLooper()).idle();

        TextView first = container(activity).getChildAt(0).findViewById(R.id.browseEventItemTitle);
        assertEquals("Later", first.getText().toString());
    }

    @Test
    public void filterToggleChip_expandsAndCollapsesPanel() {
        BrowseEventsActivity activity = launch();

        com.google.android.material.chip.Chip filterChip =
                activity.findViewById(R.id.browseFilterToggleChip);
        LinearLayout filterPanel = activity.findViewById(R.id.browseFilterPanel);

        assertEquals(View.GONE, filterPanel.getVisibility());
        filterChip.performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(View.VISIBLE, filterPanel.getVisibility());

        filterChip.performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(View.GONE, filterPanel.getVisibility());
    }

    @Test
    public void toolbar_showsEmailInSubtitle() {
        BrowseEventsActivity activity = launch();
        com.google.android.material.appbar.MaterialToolbar toolbar =
                activity.findViewById(R.id.browseToolbar);
        assertNotNull(toolbar.getSubtitle());
        assertTrue(toolbar.getSubtitle().toString().contains("customer@example.com"));
    }

    @Test
    public void signOut_viaMenu_navigatesToMain() {
        BrowseEventsActivity activity = launch();

        shadowOf(Looper.getMainLooper()).idle();
        shadowOf(activity).clickMenuItem(R.id.action_sign_out);
        shadowOf(Looper.getMainLooper()).idle();

        Intent started = shadowOf(activity).getNextStartedActivity();
        assertNotNull(started);
        assertNotNull(started.getComponent());
        assertEquals(MainActivity.class.getName(), started.getComponent().getClassName());
    }

    @Test
    public void loadError_showsErrorText() {
        eventRepository.loadError = "network unavailable";
        BrowseEventsActivity activity = launch();

        TextView emptyText = activity.findViewById(R.id.browseEventsEmptyText);
        assertEquals(activity.getString(R.string.home_events_load_failed),
                emptyText.getText().toString());
    }

    @Test
    public void activeChip_location_appearsAndDismissClears() {
        eventRepository.add(event("d1", "Event A", EventStatus.ACTIVE, futureMillis, 100, 50, "Music", "Montreal"));
        eventRepository.add(event("d2", "Event B", EventStatus.ACTIVE, futureMillis, 100, 50, "Music", "Toronto"));
        BrowseEventsActivity activity = launch();

        android.widget.EditText locInput = activity.findViewById(R.id.browseLocationFilterInput);
        locInput.setText("Montreal");
        shadowOf(Looper.getMainLooper()).idle();

        LinearLayout chipsContainer = activity.findViewById(R.id.browseActiveChipsContainer);
        assertTrue(chipsContainer.getChildCount() > 0);

        com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) chipsContainer.getChildAt(0);
        chip.performCloseIconClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("", locInput.getText().toString());
        assertEquals(2, container(activity).getChildCount());
    }

    @Test
    public void activeChip_hidePast_appearsWhenToggleOn() {
        eventRepository.add(event("d1", "Past",   EventStatus.ACTIVE, pastMillis,   100, 50));
        eventRepository.add(event("d2", "Future", EventStatus.ACTIVE, futureMillis, 100, 50));
        BrowseEventsActivity activity = launch();

        com.google.android.material.materialswitch.MaterialSwitch hidePast =
                activity.findViewById(R.id.browseHidePastSwitch);
        assertTrue(hidePast.isChecked());
        assertEquals(1, container(activity).getChildCount());
    }

    @Test
    public void activeChip_dateRange_dismiss_resetsDatesAndButtons() throws Exception {
        long day1 = futureMillis;
        long day2 = futureMillis + 24 * 60 * 60 * 1000L;

        eventRepository.add(event("d1", "Day1", EventStatus.ACTIVE, day1, 100, 50));
        eventRepository.add(event("d2", "Day2", EventStatus.ACTIVE, day2, 100, 50));

        BrowseEventsActivity activity = launch();

        java.lang.reflect.Field fromField =
                BrowseEventsActivity.class.getDeclaredField("filterDateFromMillis");
        fromField.setAccessible(true);
        fromField.set(activity, day1);

        java.lang.reflect.Field toField =
                BrowseEventsActivity.class.getDeclaredField("filterDateToMillis");
        toField.setAccessible(true);
        toField.set(activity, day2);

        java.lang.reflect.Method method =
                BrowseEventsActivity.class.getDeclaredMethod("applyFiltersAndRender");
        method.setAccessible(true);
        method.invoke(activity);
        shadowOf(Looper.getMainLooper()).idle();

        LinearLayout chipsContainer =
                activity.findViewById(R.id.browseActiveChipsContainer);
        assertTrue(chipsContainer.getChildCount() > 0);

        com.google.android.material.chip.Chip chip =
                (com.google.android.material.chip.Chip) chipsContainer.getChildAt(0);
        chip.performCloseIconClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(0L, fromField.get(activity));
        assertEquals(0L, toField.get(activity));

        android.widget.Button fromButton =
                activity.findViewById(R.id.browseDateFromButton);
        android.widget.Button toButton =
                activity.findViewById(R.id.browseDateToButton);

        assertEquals(
                activity.getString(R.string.browse_filter_date_from_hint),
                fromButton.getText().toString()
        );
        assertEquals(
                activity.getString(R.string.browse_filter_date_to_hint),
                toButton.getText().toString()
        );

        assertEquals(2, container(activity).getChildCount());
    }

    @Test
    public void activeChip_category_dismiss_clearsFilterAndInput() {
        eventRepository.add(event("d1", "Concert", EventStatus.ACTIVE, futureMillis, 100, 50, "Music", "Montreal"));
        eventRepository.add(event("d2", "Talk", EventStatus.ACTIVE, futureMillis, 100, 50, "Tech", "Montreal"));

        BrowseEventsActivity activity = launch();

        android.widget.EditText categoryInput =
                activity.findViewById(R.id.browseCategoryFilterInput);

        categoryInput.setText("Music");
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(1, container(activity).getChildCount());

        LinearLayout chipsContainer =
                activity.findViewById(R.id.browseActiveChipsContainer);
        assertTrue(chipsContainer.getChildCount() > 0);

        com.google.android.material.chip.Chip chip =
                (com.google.android.material.chip.Chip) chipsContainer.getChildAt(0);

        chip.performCloseIconClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("", categoryInput.getText().toString());
        assertEquals(2, container(activity).getChildCount());
    }

    @Test
    public void bottomNav_myTicketsTab_startsMyTicketsActivity() {
        BrowseEventsActivity activity = launch();

        com.google.android.material.bottomnavigation.BottomNavigationView nav =
                activity.findViewById(R.id.browseBottomNav);
        nav.setSelectedItemId(R.id.nav_my_tickets);
        shadowOf(Looper.getMainLooper()).idle();

        Intent started = shadowOf(activity).getNextStartedActivity();
        assertNotNull(started);
        assertNotNull(started.getComponent());
        assertEquals(MyTicketsActivity.class.getName(), started.getComponent().getClassName());
    }

    @Test
    public void onDestroy_removesEventListenerHandle() {
        BrowseEventsActivity activity = launch();

        final boolean[] removed = { false };

        EventListenerHandle fakeHandle = new EventListenerHandle() {
            @Override public void remove() {
                removed[0] = true;
            }
        };

        try {
            java.lang.reflect.Field field =
                    BrowseEventsActivity.class.getDeclaredField("eventsListenerHandle");
            field.setAccessible(true);
            field.set(activity, fakeHandle);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        activity.onDestroy();

        assertTrue(removed[0]);

        try {
            java.lang.reflect.Field field =
                    BrowseEventsActivity.class.getDeclaredField("eventsListenerHandle");
            field.setAccessible(true);
            assertNull(field.get(activity));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void onStart_whenNotSignedIn_redirectsToMain() {
        BrowseEventsActivity activity = launch();

        authRepository.signedIn = false;
        activity.onStart();
        shadowOf(Looper.getMainLooper()).idle();

        Intent started = shadowOf(activity).getNextStartedActivity();
        assertNotNull(started);
        assertNotNull(started.getComponent());
        assertEquals(MainActivity.class.getName(), started.getComponent().getClassName());
    }

    @Test
    public void renderEvents_nullTitle_showsFallbackText() {
        eventRepository.add(new Event("d1", "d1", null, "Music", "Montreal", futureMillis, EventStatus.ACTIVE, 100, 50));
        BrowseEventsActivity activity = launch();

        View card = container(activity).getChildAt(0);
        TextView title = card.findViewById(R.id.browseEventItemTitle);
        assertEquals(activity.getString(R.string.home_event_untitled), title.getText().toString());
    }

    @Test
    public void renderEvents_zeroDateTime_showsNoTimeFallback() {
        eventRepository.add(new Event("d1", "d1", "No Time Event", "Music", "Montreal", 0L, EventStatus.ACTIVE, 100, 50));
        BrowseEventsActivity activity = launch();

        com.google.android.material.materialswitch.MaterialSwitch hidePast =
                activity.findViewById(R.id.browseHidePastSwitch);
        hidePast.setChecked(false);
        shadowOf(Looper.getMainLooper()).idle();

        View card = container(activity).getChildAt(0);
        TextView details = card.findViewById(R.id.browseEventItemDetails);
        assertTrue(details.getText().toString().contains(activity.getString(R.string.home_event_no_time)));
    }

    @Test
    public void signOut_viaMenu_callsSignOutAndNavigatesToMain() {
        BrowseEventsActivity activity = launch();
        shadowOf(Looper.getMainLooper()).idle();
        shadowOf(activity).clickMenuItem(R.id.action_sign_out);
        shadowOf(Looper.getMainLooper()).idle();

        Intent started = shadowOf(activity).getNextStartedActivity();
        assertNotNull(started);
        assertNotNull(started.getComponent());
        assertEquals(MainActivity.class.getName(), started.getComponent().getClassName());
    }

    @Test
    public void loadError_showsErrorTextAndHidesContainer() {
        eventRepository.loadError = "server down";
        BrowseEventsActivity activity = launch();

        TextView emptyText = activity.findViewById(R.id.browseEventsEmptyText);
        assertEquals(View.VISIBLE, emptyText.getVisibility());
        assertEquals(activity.getString(R.string.home_events_load_failed), emptyText.getText().toString());
        assertEquals(0, container(activity).getChildCount());
    }

    @Test
    public void emptyList_withActiveFilter_showsNoMatchText() {
        eventRepository.add(event("d1", "Jazz", EventStatus.ACTIVE, futureMillis, 100, 50, "Music", "Montreal"));
        BrowseEventsActivity activity = launch();

        android.widget.EditText catInput = activity.findViewById(R.id.browseCategoryFilterInput);
        catInput.setText("NonExistent");
        shadowOf(Looper.getMainLooper()).idle();

        TextView emptyText = activity.findViewById(R.id.browseEventsEmptyText);
        assertEquals(View.VISIBLE, emptyText.getVisibility());
        assertEquals(activity.getString(R.string.browse_events_no_match), emptyText.getText().toString());
    }

    @Test
    public void dateRangeFilter_includesOnlyEventsInsideRange() throws Exception {
        long day1 = futureMillis;
        long day2 = futureMillis + 24 * 60 * 60 * 1000L;
        long day3 = futureMillis + 2 * 24 * 60 * 60 * 1000L;

        eventRepository.add(event("d1", "Day1", EventStatus.ACTIVE, day1, 100, 50));
        eventRepository.add(event("d2", "Day2", EventStatus.ACTIVE, day2, 100, 50));
        eventRepository.add(event("d3", "Day3", EventStatus.ACTIVE, day3, 100, 50));

        BrowseEventsActivity activity = launch();

        java.lang.reflect.Field fromField =
                BrowseEventsActivity.class.getDeclaredField("filterDateFromMillis");
        fromField.setAccessible(true);
        fromField.set(activity, day2);

        java.lang.reflect.Field toField =
                BrowseEventsActivity.class.getDeclaredField("filterDateToMillis");
        toField.setAccessible(true);
        toField.set(activity, day3);

        java.lang.reflect.Method method =
                BrowseEventsActivity.class.getDeclaredMethod("applyFiltersAndRender");
        method.setAccessible(true);
        method.invoke(activity);

        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(2, container(activity).getChildCount());
    }

    @Test
    public void showDatePicker_validFrom_updatesFilterAndButton() throws Exception {
        BrowseEventsActivity activity = launch();

        java.lang.reflect.Method method =
                BrowseEventsActivity.class.getDeclaredMethod("showDatePicker", boolean.class);
        method.setAccessible(true);
        method.invoke(activity, true);

        shadowOf(Looper.getMainLooper()).idle();

        DatePickerDialog dialog =
                (DatePickerDialog) ShadowDialog.getLatestDialog();
        assertNotNull(dialog);

        dialog.getDatePicker().updateDate(2030, 0, 1);
        dialog.getButton(DatePickerDialog.BUTTON_POSITIVE).performClick();

        shadowOf(Looper.getMainLooper()).idle();

        android.widget.Button fromButton =
                activity.findViewById(R.id.browseDateFromButton);

        assertTrue(fromButton.getText().toString().contains("2030"));
    }

    @Test
    public void showDatePicker_fromAfterTo_showsToastAndDoesNotUpdate() throws Exception {
        BrowseEventsActivity activity = launch();

        java.lang.reflect.Field toField =
                BrowseEventsActivity.class.getDeclaredField("filterDateToMillis");
        toField.setAccessible(true);
        toField.set(activity, futureMillis);

        java.lang.reflect.Method method =
                BrowseEventsActivity.class.getDeclaredMethod("showDatePicker", boolean.class);
        method.setAccessible(true);
        method.invoke(activity, true);

        shadowOf(Looper.getMainLooper()).idle();

        DatePickerDialog dialog =
                (DatePickerDialog) ShadowDialog.getLatestDialog();

        dialog.getDatePicker().updateDate(2099, 0, 1);
        dialog.getButton(DatePickerDialog.BUTTON_POSITIVE).performClick();

        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(
                activity.getString(R.string.browse_filter_date_from_after_to),
                org.robolectric.shadows.ShadowToast.getTextOfLatestToast()
        );
    }


    @Test
    public void showDatePicker_toBeforeFrom_showsToastAndDoesNotUpdate() throws Exception {
        BrowseEventsActivity activity = launch();

        java.lang.reflect.Field fromField =
                BrowseEventsActivity.class.getDeclaredField("filterDateFromMillis");
        fromField.setAccessible(true);
        fromField.set(activity, futureMillis);

        java.lang.reflect.Method method =
                BrowseEventsActivity.class.getDeclaredMethod("showDatePicker", boolean.class);
        method.setAccessible(true);
        method.invoke(activity, false);

        shadowOf(Looper.getMainLooper()).idle();

        DatePickerDialog dialog =
                (DatePickerDialog) ShadowDialog.getLatestDialog();

        dialog.getDatePicker().updateDate(2000, 0, 1);
        dialog.getButton(DatePickerDialog.BUTTON_POSITIVE).performClick();

        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(
                activity.getString(R.string.browse_filter_date_to_before_from),
                org.robolectric.shadows.ShadowToast.getTextOfLatestToast()
        );
    }

    @Test
    public void showReserveConfirmation_opensDialog() {
        Event e = event("d1", "Concert", EventStatus.ACTIVE, futureMillis, 10, 5);
        eventRepository.add(e);
        BrowseEventsActivity activity = launch();
        
        View card = container(activity).getChildAt(0);
        card.findViewById(R.id.browseEventReserveButton).performClick();
        
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        assertEquals("Confirm Reservation", shadowOf(dialog).getTitle());
        assertTrue(shadowOf(dialog).getMessage().toString().contains("Concert"));
    }

    @Test
    public void confirmReservation_success_showsBookedDialog() throws Exception {
        Event e = event("d1", "Concert", EventStatus.ACTIVE, futureMillis, 10, 5);
        eventRepository.add(e);
        BrowseEventsActivity activity = launch();

        // 1. Trigger confirmation dialog
        java.lang.reflect.Method method = BrowseEventsActivity.class.getDeclaredMethod("showReserveConfirmation", Event.class);
        method.setAccessible(true);
        method.invoke(activity, e);
        
        // 2. Click "Yes"
        AlertDialog confirmDialog = (AlertDialog) ShadowDialog.getLatestDialog();
        confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        // 3. Verify final Success dialog
        AlertDialog resultDialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(resultDialog);
        assertEquals("Success", shadowOf(resultDialog).getTitle());
        assertEquals("BOOKED", shadowOf(resultDialog).getMessage());
    }

    @Test
    public void confirmReservation_error_showsErrorDialog() throws Exception {
        Event e = event("d1", "Concert", EventStatus.ACTIVE, futureMillis, 10, 5);
        eventRepository.add(e);
        reservationRepository.forcedError = "Database Busy";
        BrowseEventsActivity activity = launch();

        java.lang.reflect.Method method = BrowseEventsActivity.class.getDeclaredMethod("showReserveConfirmation", Event.class);
        method.setAccessible(true);
        method.invoke(activity, e);
        
        AlertDialog confirmDialog = (AlertDialog) ShadowDialog.getLatestDialog();
        confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        AlertDialog resultDialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(resultDialog);
        assertEquals("Error", shadowOf(resultDialog).getTitle());
        assertTrue(shadowOf(resultDialog).getMessage().toString().contains("Database Busy"));
    }

    @Test
    public void showResultDialog_displaysCorrectText() throws Exception {
        BrowseEventsActivity activity = launch();
        
        java.lang.reflect.Method method = BrowseEventsActivity.class.getDeclaredMethod("showResultDialog", String.class, String.class);
        method.setAccessible(true);
        method.invoke(activity, "TestTitle", "TestMessage");
        
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        assertEquals("TestTitle", shadowOf(dialog).getTitle());
        assertEquals("TestMessage", shadowOf(dialog).getMessage());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private BrowseEventsActivity launch() {
        Intent intent = BrowseEventsActivity.newIntent(
                ApplicationProvider.getApplicationContext(),
                "customer@example.com",
                "CUSTOMER"
        );
        return Robolectric.buildActivity(BrowseEventsActivity.class, intent).setup().get();
    }

    private LinearLayout container(BrowseEventsActivity activity) {
        return activity.findViewById(R.id.browseEventsContainer);
    }

    private Event event(String docId, String title, EventStatus status, long millis,
                        int total, int remaining) {
        return new Event(docId, docId, title, "Category", "Location", millis, status, total, remaining);
    }

    private Event event(String docId, String title, EventStatus status, long millis,
                        int total, int remaining, String category, String location) {
        return new Event(docId, docId, title, category, location, millis, status, total, remaining);
    }

    // ── Fakes ────────────────────────────────────────────────────────────────

    private static final class FakeAuthRepository implements AuthRepository {
        boolean signedIn = true;
        @Override public void signIn(String id, String pw, AuthCallback cb) {}
        @Override public void register(String e, String p, String pw, AuthCallback cb) {}
        @Override public boolean isSignedIn() { return signedIn; }
        @Override public String getSignedInEmail() { return "customer@example.com"; }
        @Override public UserRole getSignedInRole() { return UserRole.CUSTOMER; }
        @Override public void signOut() { signedIn = false; }
    }

    private static final class FakeEventRepository implements EventRepository {
        private final List<Event> events = new ArrayList<>();
        String loadError = null;

        void add(Event e) { events.add(e); }

        @Override
        public void loadEvents(EventListCallback callback) {
            if (loadError != null) { callback.onError(loadError); return; }
            callback.onSuccess(new ArrayList<>(events));
        }

        @Override
        public EventListenerHandle listenToEvents(EventListCallback callback) {
            if (callback != null) {
                if (loadError != null) {
                    callback.onError(loadError);
                } else {
                    List<Event> sorted = new ArrayList<>(events);
                    sorted.sort((a, b) -> Long.compare(b.getDateTimeMillis(), a.getDateTimeMillis()));
                    callback.onSuccess(sorted);
                }
            }
            return new EventListenerHandle() { @Override public void remove() {} };
        }

        @Override public void createEvent(Event e, EventActionCallback cb) { cb.onSuccess(); }
        @Override public void updateEvent(Event e, EventActionCallback cb) { cb.onSuccess(); }
        @Override public void updateStatus(String id, EventStatus s, EventActionCallback cb) { cb.onSuccess(); }
    }

    private static final class FakeReservationRepository implements ReservationRepository {
        private final List<Reservation> reservations = new ArrayList<>();
        String forcedError = null;
        @Override public void createReservation(Reservation r, ReservationActionCallback cb) {
            if (forcedError != null) { cb.onError(forcedError); return; }
            reservations.add(r);
            cb.onSuccess();
        }
        @Override public void cancelReservation(String id, ReservationActionCallback cb) {
            reservations.removeIf(r -> r.getDocumentId().equals(id));
            cb.onSuccess();
        }
        @Override public void getReservationsForUser(String email, ReservationListCallback cb) {
            List<Reservation> userRes = new ArrayList<>();
            for(Reservation r : reservations) if(r.getUserEmail().equals(email)) userRes.add(r);
            cb.onSuccess(userRes);
        }
    }
}
