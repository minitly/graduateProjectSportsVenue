package com.tly.mapper;

import com.tly.entity.BookingReservationSlot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BookingReservationSlotMapper {

    int insert(BookingReservationSlot slot);

    int deleteByReservationId(@Param("reservationId") Long reservationId);

    int deleteByReservationIds(@Param("reservationIds") List<Long> reservationIds);

    List<BookingReservationSlot> listOccupied(@Param("venueId") Long venueId,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);
}

