package com.tly.mapper;

import com.tly.entity.FloorPlan;
import com.tly.entity.FloorPlanItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FloorPlanMapper {

    int insert(FloorPlan floorPlan);

    int update(FloorPlan floorPlan);

    int logicalDelete(@Param("id") Long id);

    FloorPlan selectById(@Param("id") Long id);

    long countByCondition(@Param("keyword") String keyword,
                          @Param("status") String status);

    List<FloorPlan> listByCondition(@Param("keyword") String keyword,
                                    @Param("status") String status,
                                    @Param("offset") long offset,
                                    @Param("pageSize") long pageSize);

    int insertFloorPlanItem(FloorPlanItem item);

    int insertFloorPlanItemRel(@Param("floorPlanId") Long floorPlanId,
                               @Param("floorPlanItemId") Long floorPlanItemId);

    List<FloorPlanItem> listItemsByFloorPlanId(@Param("floorPlanId") Long floorPlanId);

    int deleteItemRelationsByFloorPlanId(@Param("floorPlanId") Long floorPlanId);

    int deleteItemsByFloorPlanId(@Param("floorPlanId") Long floorPlanId);

    int updateFloorPlanItem(FloorPlanItem item);

    FloorPlanItem selectItemByUid(@Param("itemUid") String itemUid);

    FloorPlanItem selectItemByVenueId(@Param("venueId") Long venueId);

    Long selectFloorPlanIdByItemId(@Param("itemId") Long itemId);

    int bindItemVenue(@Param("itemId") Long itemId, @Param("venueId") Long venueId);

    int unbindItemsByVenueId(@Param("venueId") Long venueId);

    List<FloorPlanItem> listSelectableItemsByFloorPlanId(@Param("floorPlanId") Long floorPlanId,
                                                         @Param("venueId") Long venueId);

    int deleteFloorPlanItemById(@Param("id") Long id);
}

