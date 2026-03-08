package com.soen345.project;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.soen345.project.reservation.ReservationService;
import com.soen345.project.reservation.ReservationServiceProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.TIRAMISU)
public class ReservationServiceProviderRobolectricTest {

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        FirebaseApp.clearInstancesForTest();
        FirebaseApp.initializeApp(context, new FirebaseOptions.Builder()
                .setApplicationId("1:1234567890:android:test")
                .setApiKey("fake-api-key")
                .setProjectId("test-project")
                .build());
    }

    @After
    public void tearDown() {
        ReservationServiceProvider.clearReservationService();
    }

    @Test
    public void getReservationService_returnsInstance() {
        ReservationService service = ReservationServiceProvider.getReservationService();
        assertNotNull(service);
    }

    @Test
    public void setReservationService_overridesInstance() {
        ReservationService mockService = mock(ReservationService.class);
        ReservationServiceProvider.setReservationService(mockService);

        assertSame(mockService, ReservationServiceProvider.getReservationService());
    }

    @Test
    public void clearReservationService_resetsOverride() {
        ReservationService mockService = mock(ReservationService.class);
        ReservationServiceProvider.setReservationService(mockService);
        
        ReservationServiceProvider.clearReservationService();
        ReservationService newService = ReservationServiceProvider.getReservationService();

        assertNotNull(newService);
        assertNotSame(mockService, newService);
    }
}
