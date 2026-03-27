package com.tly.mapper;

import com.tly.entity.BookingReservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tly.dto.booking.BookingAllReservationRecord;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BookingReservationMapper {

    int insert(BookingReservation reservation);

    BookingReservation selectById(@Param("id") Long id);

    int updateStatusToCanceled(@Param("id") Long id,
                               @Param("cancelTime") LocalDateTime cancelTime,
                               @Param("cancelReason") String cancelReason,
                               @Param("cancelRemark") String cancelRemark);

    int updateStatusToViolationByCancel(@Param("id") Long id,
                                        @Param("cancelTime") LocalDateTime cancelTime,
                                        @Param("cancelReason") String cancelReason,
                                        @Param("cancelRemark") String cancelRemark,
                                        @Param("violationTime") LocalDateTime violationTime,
                                        @Param("violationType") String violationType);

    int updateStatusToVerified(@Param("id") Long id,
                               @Param("verifyTime") LocalDateTime verifyTime);

    List<Long> listAppliedIdsToNoShow(@Param("deadline") LocalDateTime deadline,
                                      @Param("limit") int limit);

    int updateStatusToViolationByNoShow(@Param("id") Long id,
                                        @Param("violationTime") LocalDateTime violationTime,
                                        @Param("violationType") String violationType);

    long countMyByCondition(@Param("userId") Long userId,
                            @Param("venueNameLike") String venueNameLike,
                            @Param("status") String status,
                            @Param("startTime") LocalDateTime startTime,
                            @Param("endTime") LocalDateTime endTime);

    List<BookingReservation> listMyByCondition(@Param("userId") Long userId,
                                               @Param("venueNameLike") String venueNameLike,
                                               @Param("status") String status,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime,
                                               @Param("offset") long offset,
                                               @Param("pageSize") long pageSize);

    long countAllByCondition(@Param("venueNameLike") String venueNameLike,
                             @Param("status") String status,
                             @Param("startTime") LocalDateTime startTime,
                             @Param("endTime") LocalDateTime endTime,
                             @Param("usernameLike") String usernameLike);

    List<BookingAllReservationRecord> listAllByCondition(@Param("venueNameLike") String venueNameLike,
                                                          @Param("status") String status,
                                                          @Param("startTime") LocalDateTime startTime,
                                                          @Param("endTime") LocalDateTime endTime,
                                                          @Param("usernameLike") String usernameLike,
                                                          @Param("offset") long offset,
                                                          @Param("pageSize") long pageSize);

    List<Long> listAppliedIdsByVenueId(@Param("venueId") Long venueId);

    int cancelAppliedByVenue(@Param("venueId") Long venueId,
                             @Param("cancelTime") LocalDateTime cancelTime,
                             @Param("cancelReason") String cancelReason,
                             @Param("cancelRemark") String cancelRemark);
}

