package com.pharmacy.dto;

import com.pharmacy.enums.DispenseChannel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelStatisticsDTO {

    private DispenseChannel channel;
    private String channelDescription;
    private Long currentWaitingCount;
    private Long todayProcessedCount;
    private Double avgDurationSeconds;
    private String avgDurationFormatted;
}
