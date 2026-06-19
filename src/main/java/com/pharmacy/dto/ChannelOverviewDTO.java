package com.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelOverviewDTO {

    private ChannelStatisticsDTO fastChannel;
    private ChannelStatisticsDTO normalChannel;
    private List<WindowDTO> windows;
    private String crossChannelStatus;
}
