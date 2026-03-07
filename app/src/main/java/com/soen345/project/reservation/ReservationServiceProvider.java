package com.soen345.project.reservation;

import com.soen345.project.event.EventRepository;
import com.soen345.project.event.FirebaseEventRepository;

public class ReservationServiceProvider
{
    private static ReservationService reservationServiceForTesting;

    public static ReservationService getReservationService()
    {
        if (reservationServiceForTesting != null) {
            return reservationServiceForTesting;
        }
        ReservationRepository reservationRepository = new FirebaseReservationRepository();
        EventRepository eventRepository = new FirebaseEventRepository();
        return new ReservationService(reservationRepository, eventRepository);
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
