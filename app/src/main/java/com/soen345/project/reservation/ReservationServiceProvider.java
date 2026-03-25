package com.soen345.project.reservation;

import com.soen345.project.BuildConfig;
import com.soen345.project.event.EventRepository;
import com.soen345.project.event.FirebaseEventRepository;
import com.soen345.project.notification.BookingConfirmationDispatcher;
import com.soen345.project.notification.HttpBookingConfirmationDispatcher;

public class ReservationServiceProvider
{
    private static ReservationService reservationServiceForTesting;

    private ReservationServiceProvider() {}

    public static ReservationService getReservationService()
    {
        if (reservationServiceForTesting != null) {
            return reservationServiceForTesting;
        }
        ReservationRepository reservationRepository = new FirebaseReservationRepository();
        EventRepository eventRepository = new FirebaseEventRepository();
        BookingConfirmationDispatcher bookingConfirmationDispatcher =
                new HttpBookingConfirmationDispatcher(
                        BuildConfig.MAIL_RELAY_BASE_URL
                );
        return new ReservationService(
                reservationRepository,
                eventRepository,
                bookingConfirmationDispatcher
        );
    }

    public static void setReservationService(ReservationService reservationService)
    {
        reservationServiceForTesting = reservationService;
    }

    public static void clearReservationService()
    {
        reservationServiceForTesting = null;
    }
}
