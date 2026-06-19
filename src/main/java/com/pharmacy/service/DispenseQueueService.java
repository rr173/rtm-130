package com.pharmacy.service;

import com.pharmacy.dto.*;
import com.pharmacy.entity.DispenseQueueItem;
import com.pharmacy.entity.DispenseRecord;
import com.pharmacy.entity.DispensingWindow;
import com.pharmacy.entity.Prescription;
import com.pharmacy.enums.DispenseChannel;
import com.pharmacy.enums.DispensingWindowStatus;
import com.pharmacy.enums.PrescriptionStatus;
import com.pharmacy.enums.PrescriptionType;
import com.pharmacy.enums.QueueItemStatus;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.DispenseQueueItemRepository;
import com.pharmacy.repository.DispenseRecordRepository;
import com.pharmacy.repository.DispensingWindowRepository;
import com.pharmacy.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispenseQueueService {

    private static final int FAST_CHANNEL_DRUG_THRESHOLD = 3;
    private static final int CROSS_CHANNEL_QUEUE_THRESHOLD = 3;
    private static final int AVERAGE_FAST_DISPENSE_MINUTES = 2;
    private static final int AVERAGE_NORMAL_DISPENSE_MINUTES = 6;
    private static final int TIMEOUT_MINUTES = 15;
    private static final int MAX_RETURN_COUNT = 3;

    private static final int PRIORITY_RETURNED_EMERGENCY = 10;
    private static final int PRIORITY_RETURNED_NORMAL = 20;
    private static final int PRIORITY_NEW_EMERGENCY = 100;
    private static final int PRIORITY_NEW_NORMAL = 200;

    private final DispenseQueueItemRepository queueItemRepository;
    private final DispensingWindowRepository windowRepository;
    private final DispenseRecordRepository recordRepository;
    private final PrescriptionRepository prescriptionRepository;

    @Transactional
    public QueueItemDTO enqueue(String prescriptionNo) {
        log.info("处方[{}]加入配药排队队列", prescriptionNo);

        Prescription prescription = prescriptionRepository.findByPrescriptionNo(prescriptionNo)
                .orElseThrow(() -> new ResourceNotFoundException("处方不存在: " + prescriptionNo));

        if (prescription.getStatus() != PrescriptionStatus.PREOCCUPIED) {
            throw new BusinessException("处方状态[" + prescription.getStatus().getDescription() + "]不允许加入排队");
        }

        queueItemRepository.findByPrescriptionNoAndStatus(prescriptionNo, QueueItemStatus.WAITING)
                .ifPresent(existing -> {
                    throw new BusinessException("处方[" + prescriptionNo + "]已在排队队列中");
                });

        int drugItemCount = prescription.getItems() != null ? prescription.getItems().size() : 0;
        DispenseChannel channel = DispenseChannel.classifyByDrugCount(drugItemCount);

        DispenseQueueItem item = new DispenseQueueItem();
        item.setPrescriptionNo(prescriptionNo);
        item.setPrescriptionType(prescription.getType());
        item.setChannel(channel);
        item.setDrugItemCount(drugItemCount);
        item.setEnqueueTime(LocalDateTime.now());
        item.setStatus(QueueItemStatus.WAITING);
        item.setReturnCount(0);
        item.setSortPriority(prescription.getType() == PrescriptionType.EMERGENCY
                ? PRIORITY_NEW_EMERGENCY : PRIORITY_NEW_NORMAL);

        item = queueItemRepository.save(item);
        log.info("处方[{}]已加入[{}]，药品数: {}，类型: {}",
                prescriptionNo, channel.getDescription(), drugItemCount, prescription.getType().getDescription());

        notifyIdleWindows();
        return enrichWithPosition(item);
    }

    @Transactional
    public QueueItemDTO claimNext(String windowNo, String pharmacistId, String pharmacistName) {
        log.info("窗口[{}]领取下一张处方，配药师: {}", windowNo, pharmacistName);

        DispensingWindow window = windowRepository.findByWindowNoWithLock(windowNo)
                .orElseThrow(() -> new ResourceNotFoundException("窗口不存在: " + windowNo));

        if (window.getStatus() == DispensingWindowStatus.CLOSED) {
            throw new BusinessException("窗口[" + windowNo + "]已关闭，无法领取处方");
        }
        if (window.getStatus() == DispensingWindowStatus.BUSY) {
            throw new BusinessException("窗口[" + windowNo + "]正在处理处方[" + window.getCurrentPrescriptionNo() + "]，请先完成或释放");
        }

        DispenseChannel serviceChannel = window.getServiceChannel();
        List<DispenseChannel> targetChannels = determineTargetChannels(serviceChannel);

        DispenseQueueItem nextItem = queueItemRepository.findNextWaitingByChannels(targetChannels)
                .orElse(null);

        if (nextItem == null) {
            log.info("队列为空（目标通道: {}），窗口[{}]进入等待状态", targetChannels, windowNo);
            throw new BusinessException("当前排队队列为空，没有可领取的处方");
        }

        Prescription prescription = prescriptionRepository.findByPrescriptionNoWithLock(nextItem.getPrescriptionNo())
                .orElseThrow(() -> new ResourceNotFoundException("处方不存在: " + nextItem.getPrescriptionNo()));

        if (prescription.getStatus() == PrescriptionStatus.CANCELLED) {
            nextItem.setStatus(QueueItemStatus.COMPLETED);
            nextItem.setCompleteTime(LocalDateTime.now());
            queueItemRepository.save(nextItem);
            log.info("处方[{}]已取消，跳过", nextItem.getPrescriptionNo());
            return claimNext(windowNo, pharmacistId, pharmacistName);
        }

        nextItem.setStatus(QueueItemStatus.PROCESSING);
        nextItem.setWindowNo(windowNo);
        nextItem.setClaimTime(LocalDateTime.now());
        queueItemRepository.save(nextItem);

        prescription.setStatus(PrescriptionStatus.DISPENSING);
        prescriptionRepository.save(prescription);

        window.setStatus(DispensingWindowStatus.BUSY);
        window.setCurrentServingChannel(nextItem.getChannel());
        window.setCurrentPrescriptionNo(nextItem.getPrescriptionNo());
        window.setCurrentPharmacistId(pharmacistId);
        window.setCurrentPharmacistName(pharmacistName);
        window.setDispenseStartTime(LocalDateTime.now());
        windowRepository.save(window);

        log.info("窗口[{}]({})已领取处方[{}]({})，配药师: {}",
                windowNo, serviceChannel.getDescription(),
                nextItem.getPrescriptionNo(), nextItem.getChannel().getDescription(),
                pharmacistName);
        return enrichWithPosition(nextItem);
    }

    private List<DispenseChannel> determineTargetChannels(DispenseChannel serviceChannel) {
        if (serviceChannel != DispenseChannel.BOTH) {
            return Collections.singletonList(serviceChannel);
        }

        long fastWaiting = queueItemRepository.countWaitingByChannel(DispenseChannel.FAST);
        long normalWaiting = queueItemRepository.countWaitingByChannel(DispenseChannel.NORMAL);

        List<DispenseChannel> channels = new ArrayList<>();

        if (fastWaiting == 0 && normalWaiting > CROSS_CHANNEL_QUEUE_THRESHOLD) {
            channels.add(DispenseChannel.NORMAL);
            log.debug("双通道调度: 快速通道为空，普通通道排队{}人，优先处理普通通道", normalWaiting);
        } else if (normalWaiting == 0 && fastWaiting > CROSS_CHANNEL_QUEUE_THRESHOLD) {
            channels.add(DispenseChannel.FAST);
            log.debug("双通道调度: 普通通道为空，快速通道排队{}人，优先处理快速通道", fastWaiting);
        } else {
            channels.add(DispenseChannel.FAST);
            channels.add(DispenseChannel.NORMAL);
        }

        return channels;
    }

    public String getCrossChannelStatus() {
        long fastWaiting = queueItemRepository.countWaitingByChannel(DispenseChannel.FAST);
        long normalWaiting = queueItemRepository.countWaitingByChannel(DispenseChannel.NORMAL);

        if (fastWaiting == 0 && normalWaiting > CROSS_CHANNEL_QUEUE_THRESHOLD) {
            return "已触发跨通道调度：快速通道为空，双通道窗口将协助处理普通通道（当前排队" + normalWaiting + "人）";
        } else if (normalWaiting == 0 && fastWaiting > CROSS_CHANNEL_QUEUE_THRESHOLD) {
            return "已触发跨通道调度：普通通道为空，双通道窗口将协助处理快速通道（当前排队" + fastWaiting + "人）";
        } else {
            return "未触发跨通道调度：快速通道" + fastWaiting + "人排队，普通通道" + normalWaiting + "人排队";
        }
    }

    @Transactional
    public WindowDTO completeDispense(String windowNo) {
        log.info("窗口[{}]确认配药完成", windowNo);

        DispensingWindow window = windowRepository.findByWindowNoWithLock(windowNo)
                .orElseThrow(() -> new ResourceNotFoundException("窗口不存在: " + windowNo));

        if (window.getStatus() != DispensingWindowStatus.BUSY) {
            throw new BusinessException("窗口[" + windowNo + "]当前没有正在处理的处方");
        }

        String prescriptionNo = window.getCurrentPrescriptionNo();
        String pharmacistId = window.getCurrentPharmacistId();
        String pharmacistName = window.getCurrentPharmacistName();
        DispenseChannel channel = window.getCurrentServingChannel();
        LocalDateTime startTime = window.getDispenseStartTime();
        LocalDateTime now = LocalDateTime.now();

        DispenseQueueItem queueItem = queueItemRepository
                .findByPrescriptionNoAndStatus(prescriptionNo, QueueItemStatus.PROCESSING)
                .orElseThrow(() -> new BusinessException("队列中未找到处方[" + prescriptionNo + "]的配药记录"));

        queueItem.setStatus(QueueItemStatus.COMPLETED);
        queueItem.setCompleteTime(now);
        queueItemRepository.save(queueItem);

        Prescription prescription = prescriptionRepository.findByPrescriptionNoWithLock(prescriptionNo)
                .orElseThrow(() -> new ResourceNotFoundException("处方不存在: " + prescriptionNo));
        prescription.setStatus(PrescriptionStatus.DISPENSE_READY);
        prescriptionRepository.save(prescription);

        DispenseRecord record = new DispenseRecord();
        record.setWindowNo(windowNo);
        record.setPrescriptionNo(prescriptionNo);
        record.setChannel(channel);
        record.setPharmacistId(pharmacistId);
        record.setPharmacistName(pharmacistName);
        record.setStartTime(startTime);
        record.setEndTime(now);
        record.setDurationSeconds(Duration.between(startTime, now).getSeconds());
        recordRepository.save(record);

        resetWindowToIdle(window);
        windowRepository.save(window);

        log.info("窗口[{}]配药完成，处方[{}]({})耗时{}秒，已流转到发药确认环节",
                windowNo, prescriptionNo, channel != null ? channel.getDescription() : "未知",
                record.getDurationSeconds());

        tryAutoClaimNext(windowNo, pharmacistId, pharmacistName);
        return WindowDTO.fromEntity(window);
    }

    @Transactional
    public WindowDTO returnPrescription(String windowNo) {
        log.info("窗口[{}]主动释放处方", windowNo);

        DispensingWindow window = windowRepository.findByWindowNoWithLock(windowNo)
                .orElseThrow(() -> new ResourceNotFoundException("窗口不存在: " + windowNo));

        if (window.getStatus() != DispensingWindowStatus.BUSY) {
            throw new BusinessException("窗口[" + windowNo + "]当前没有正在处理的处方");
        }

        String pharmacistId = window.getCurrentPharmacistId();
        String pharmacistName = window.getCurrentPharmacistName();

        returnPrescriptionToQueue(window);

        resetWindowToIdle(window);
        windowRepository.save(window);

        log.info("窗口[{}]已释放处方，恢复空闲", windowNo);

        tryAutoClaimNext(windowNo, pharmacistId, pharmacistName);
        return WindowDTO.fromEntity(window);
    }

    @Transactional
    public List<DispensingWindow> checkAndReturnTimeout() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

        List<DispensingWindow> busyWindows = windowRepository.findByStatus(DispensingWindowStatus.BUSY);
        List<DispensingWindow> timedOutWindows = new ArrayList<>();

        for (DispensingWindow window : busyWindows) {
            if (window.getDispenseStartTime() != null
                    && window.getDispenseStartTime().isBefore(timeoutThreshold)) {
                log.warn("窗口[{}]处方[{}]配药超时(超过{}分钟)，自动退回队列",
                        window.getWindowNo(), window.getCurrentPrescriptionNo(), TIMEOUT_MINUTES);

                DispensingWindow locked = windowRepository.findByWindowNoWithLock(window.getWindowNo())
                        .orElseThrow();
                returnPrescriptionToQueue(locked);
                resetWindowToIdle(locked);
                windowRepository.save(locked);
                timedOutWindows.add(locked);
            }
        }

        return timedOutWindows;
    }

    private void returnPrescriptionToQueue(DispensingWindow window) {
        String prescriptionNo = window.getCurrentPrescriptionNo();

        DispenseQueueItem queueItem = queueItemRepository
                .findByPrescriptionNoAndStatus(prescriptionNo, QueueItemStatus.PROCESSING)
                .orElseThrow(() -> new BusinessException("队列中未找到处方[" + prescriptionNo + "]的配药记录"));

        if (queueItem.getReturnCount() >= MAX_RETURN_COUNT) {
            log.warn("处方[{}]已退回{}次，不再退回，标记为已完成", prescriptionNo, MAX_RETURN_COUNT);
            queueItem.setStatus(QueueItemStatus.COMPLETED);
            queueItem.setCompleteTime(LocalDateTime.now());
            queueItemRepository.save(queueItem);

            Prescription prescription = prescriptionRepository.findByPrescriptionNoWithLock(prescriptionNo)
                    .orElseThrow(() -> new ResourceNotFoundException("处方不存在: " + prescriptionNo));
            prescription.setStatus(PrescriptionStatus.PREOCCUPIED);
            prescriptionRepository.save(prescription);
            return;
        }

        queueItem.setStatus(QueueItemStatus.WAITING);
        queueItem.setWindowNo(null);
        queueItem.setClaimTime(null);
        queueItem.setReturnCount(queueItem.getReturnCount() + 1);

        int basePriority = queueItem.getPrescriptionType() == PrescriptionType.EMERGENCY
                ? PRIORITY_RETURNED_EMERGENCY : PRIORITY_RETURNED_NORMAL;
        queueItem.setSortPriority(basePriority);

        LocalDateTime now = LocalDateTime.now();
        Optional<LocalDateTime> minEnqueueTime = Optional.ofNullable(
                queueItemRepository.findMinEnqueueTime(QueueItemStatus.WAITING, basePriority));
        if (minEnqueueTime.isPresent()) {
            queueItem.setEnqueueTime(minEnqueueTime.get().minusNanos(1));
        } else {
            queueItem.setEnqueueTime(now);
        }
        queueItemRepository.save(queueItem);

        Prescription prescription = prescriptionRepository.findByPrescriptionNoWithLock(prescriptionNo)
                .orElseThrow(() -> new ResourceNotFoundException("处方不存在: " + prescriptionNo));
        prescription.setStatus(PrescriptionStatus.PREOCCUPIED);
        prescriptionRepository.save(prescription);

        log.info("处方[{}]已退回[{}]队列头部，退回次数: {}",
                prescriptionNo, queueItem.getChannel().getDescription(), queueItem.getReturnCount());
    }

    private void resetWindowToIdle(DispensingWindow window) {
        window.setStatus(DispensingWindowStatus.IDLE);
        window.setCurrentServingChannel(null);
        window.setCurrentPrescriptionNo(null);
        window.setCurrentPharmacistId(null);
        window.setCurrentPharmacistName(null);
        window.setDispenseStartTime(null);
    }

    private void tryAutoClaimNext(String windowNo, String pharmacistId, String pharmacistName) {
        try {
            DispensingWindow window = windowRepository.findByWindowNo(windowNo)
                    .orElseThrow();
            if (window.getStatus() == DispensingWindowStatus.IDLE) {
                claimNext(windowNo, pharmacistId, pharmacistName);
            }
        } catch (Exception e) {
            log.info("窗口[{}]自动领取下一张处方: {}", windowNo, e.getMessage());
        }
    }

    private void notifyIdleWindows() {
        try {
            List<DispensingWindow> idleWindows = windowRepository.findByStatus(DispensingWindowStatus.IDLE);
            for (DispensingWindow window : idleWindows) {
                try {
                    claimNext(window.getWindowNo(), null, null);
                    break;
                } catch (Exception e) {
                    log.debug("窗口[{}]自动领取失败: {}", window.getWindowNo(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("通知空闲窗口失败: {}", e.getMessage());
        }
    }

    @Transactional
    public WindowDTO closeWindow(String windowNo) {
        log.info("关闭窗口[{}]", windowNo);

        DispensingWindow window = windowRepository.findByWindowNoWithLock(windowNo)
                .orElseThrow(() -> new ResourceNotFoundException("窗口不存在: " + windowNo));

        if (window.getStatus() == DispensingWindowStatus.CLOSED) {
            throw new BusinessException("窗口[" + windowNo + "]已经处于关闭状态");
        }

        if (window.getStatus() == DispensingWindowStatus.BUSY) {
            String prescriptionNo = window.getCurrentPrescriptionNo();
            returnPrescriptionToQueue(window);
            log.info("窗口[{}]关闭前已将处方[{}]退回队列", windowNo, prescriptionNo);
        }

        resetWindowToIdle(window);
        window.setStatus(DispensingWindowStatus.CLOSED);
        windowRepository.save(window);

        log.info("窗口[{}]已关闭", windowNo);
        return WindowDTO.fromEntity(window);
    }

    @Transactional
    public WindowDTO openWindow(String windowNo) {
        log.info("开启窗口[{}]", windowNo);

        DispensingWindow window = windowRepository.findByWindowNoWithLock(windowNo)
                .orElseThrow(() -> new ResourceNotFoundException("窗口不存在: " + windowNo));

        if (window.getStatus() != DispensingWindowStatus.CLOSED) {
            throw new BusinessException("窗口[" + windowNo + "]当前状态为[" + window.getStatus().getDescription() + "]，无法开启");
        }

        window.setStatus(DispensingWindowStatus.IDLE);
        windowRepository.save(window);

        log.info("窗口[{}]已开启", windowNo);

        tryAutoClaimNext(windowNo, null, null);
        return WindowDTO.fromEntity(window);
    }

    @Transactional
    public WindowDTO updateWindowChannel(String windowNo, DispenseChannel serviceChannel) {
        log.info("更新窗口[{}]服务通道为: {}", windowNo, serviceChannel.getDescription());

        DispensingWindow window = windowRepository.findByWindowNoWithLock(windowNo)
                .orElseThrow(() -> new ResourceNotFoundException("窗口不存在: " + windowNo));

        if (window.getStatus() == DispensingWindowStatus.BUSY) {
            throw new BusinessException("窗口[" + windowNo + "]正在处理处方，请先完成后再修改通道配置");
        }

        window.setServiceChannel(serviceChannel);
        windowRepository.save(window);

        log.info("窗口[{}]通道配置已更新为: {}", windowNo, serviceChannel.getDescription());
        return WindowDTO.fromEntity(window);
    }

    public List<WindowDTO> getAllWindows() {
        return windowRepository.findAllByOrderByWindowNoAsc().stream()
                .map(WindowDTO::fromEntity)
                .toList();
    }

    public WindowDTO getWindow(String windowNo) {
        DispensingWindow window = windowRepository.findByWindowNo(windowNo)
                .orElseThrow(() -> new ResourceNotFoundException("窗口不存在: " + windowNo));
        return WindowDTO.fromEntity(window);
    }

    public List<QueueItemDTO> getQueueList() {
        List<DispenseQueueItem> waitingItems = queueItemRepository.findAllWaitingOrdered();
        List<QueueItemDTO> result = new ArrayList<>();

        int fastPos = 0;
        int normalPos = 0;
        for (DispenseQueueItem item : waitingItems) {
            QueueItemDTO dto = QueueItemDTO.fromEntity(item);
            if (item.getChannel() == DispenseChannel.FAST) {
                fastPos++;
                dto.setPosition(fastPos);
                dto.setEstimatedWaitMinutes(fastPos * AVERAGE_FAST_DISPENSE_MINUTES);
            } else {
                normalPos++;
                dto.setPosition(normalPos);
                dto.setEstimatedWaitMinutes(normalPos * AVERAGE_NORMAL_DISPENSE_MINUTES);
            }
            result.add(dto);
        }

        return result;
    }

    public List<QueueItemDTO> getQueueListByChannel(DispenseChannel channel) {
        List<DispenseQueueItem> waitingItems = queueItemRepository.findAllWaitingOrderedByChannel(channel);
        List<QueueItemDTO> result = new ArrayList<>();

        int avgMinutes = channel == DispenseChannel.FAST ? AVERAGE_FAST_DISPENSE_MINUTES : AVERAGE_NORMAL_DISPENSE_MINUTES;
        int position = 0;
        for (DispenseQueueItem item : waitingItems) {
            position++;
            QueueItemDTO dto = QueueItemDTO.fromEntity(item);
            dto.setPosition(position);
            dto.setEstimatedWaitMinutes(position * avgMinutes);
            result.add(dto);
        }

        return result;
    }

    public QueuePositionDTO getQueuePosition(String prescriptionNo) {
        DispenseQueueItem targetItem = queueItemRepository
                .findByPrescriptionNoAndStatus(prescriptionNo, QueueItemStatus.WAITING)
                .orElse(null);

        if (targetItem != null) {
            List<DispenseQueueItem> waitingItems = queueItemRepository.findAllWaitingOrderedByChannel(targetItem.getChannel());
            int avgMinutes = targetItem.getChannel() == DispenseChannel.FAST
                    ? AVERAGE_FAST_DISPENSE_MINUTES : AVERAGE_NORMAL_DISPENSE_MINUTES;
            int position = 0;
            for (DispenseQueueItem item : waitingItems) {
                position++;
                if (item.getPrescriptionNo().equals(prescriptionNo)) {
                    return new QueuePositionDTO(prescriptionNo, position, position * avgMinutes, waitingItems.size());
                }
            }
        }

        DispenseQueueItem processingItem = queueItemRepository
                .findByPrescriptionNoAndStatus(prescriptionNo, QueueItemStatus.PROCESSING)
                .orElse(null);

        if (processingItem != null) {
            long channelWaiting = queueItemRepository.countWaitingByChannel(processingItem.getChannel());
            return new QueuePositionDTO(prescriptionNo, 0, 0, (int) channelWaiting);
        }

        throw new ResourceNotFoundException("处方[" + prescriptionNo + "]不在排队队列中");
    }

    public List<WindowStatisticsDTO> getWindowStatistics(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDateTime startTime = targetDate.atStartOfDay();
        LocalDateTime endTime = targetDate.atTime(LocalTime.MAX);

        List<DispensingWindow> windows = windowRepository.findAllByOrderByWindowNoAsc();
        List<Object[]> rawStats = recordRepository.statsByWindowAndDateRange(startTime, endTime);

        Map<String, Object[]> statsMap = new HashMap<>();
        for (Object[] row : rawStats) {
            String wNo = (String) row[0];
            statsMap.put(wNo, row);
        }

        List<WindowStatisticsDTO> result = new ArrayList<>();
        for (DispensingWindow window : windows) {
            WindowStatisticsDTO dto = new WindowStatisticsDTO();
            dto.setWindowNo(window.getWindowNo());
            dto.setWindowName(window.getWindowName());
            dto.setServiceChannel(window.getServiceChannel());
            dto.setServiceChannelDescription(window.getServiceChannel().getDescription());

            Object[] stats = statsMap.get(window.getWindowNo());
            if (stats != null) {
                dto.setTodayCount(((Number) stats[1]).longValue());
                Double avgSec = stats[2] != null ? ((Number) stats[2]).doubleValue() : 0.0;
                dto.setAvgDurationSeconds(avgSec);
                dto.setAvgDurationFormatted(formatDuration(avgSec));
            } else {
                dto.setTodayCount(0L);
                dto.setAvgDurationSeconds(0.0);
                dto.setAvgDurationFormatted("0分0秒");
            }
            result.add(dto);
        }

        return result;
    }

    public ChannelStatisticsDTO getChannelStatistics(DispenseChannel channel, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDateTime startTime = targetDate.atStartOfDay();
        LocalDateTime endTime = targetDate.atTime(LocalTime.MAX);

        ChannelStatisticsDTO dto = new ChannelStatisticsDTO();
        dto.setChannel(channel);
        dto.setChannelDescription(channel.getDescription());

        dto.setCurrentWaitingCount(queueItemRepository.countWaitingByChannel(channel));
        dto.setTodayProcessedCount(recordRepository.countByChannelAndDateRange(channel, startTime, endTime));

        Double avgSec = recordRepository.avgDurationSecondsByChannelAndDateRange(channel, startTime, endTime);
        dto.setAvgDurationSeconds(avgSec != null ? avgSec : 0.0);
        dto.setAvgDurationFormatted(formatDuration(avgSec != null ? avgSec : 0.0));

        return dto;
    }

    public ChannelOverviewDTO getChannelOverview() {
        LocalDate today = LocalDate.now();

        ChannelOverviewDTO overview = new ChannelOverviewDTO();
        overview.setFastChannel(getChannelStatistics(DispenseChannel.FAST, today));
        overview.setNormalChannel(getChannelStatistics(DispenseChannel.NORMAL, today));
        overview.setWindows(getAllWindows());
        overview.setCrossChannelStatus(getCrossChannelStatus());

        return overview;
    }

    public List<QueueStatisticsDTO> getQueueStatistics(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();

        List<DispenseQueueItem> allItems = queueItemRepository.findAll();

        List<QueueStatisticsDTO> result = new ArrayList<>();

        for (int hour = 0; hour < 24; hour++) {
            LocalDateTime slotStart = targetDate.atTime(hour, 0);
            LocalDateTime slotEnd = targetDate.atTime(hour, 59, 59);

            if (slotStart.isAfter(LocalDateTime.now())) {
                break;
            }

            int peakLength = 0;
            double totalWaitMinutes = 0;
            int claimedCount = 0;

            for (int minute = 0; minute <= 59; minute++) {
                LocalDateTime checkTime = targetDate.atTime(hour, minute);
                int queueLen = 0;
                for (DispenseQueueItem item : allItems) {
                    if (item.getEnqueueTime() == null || item.getEnqueueTime().isAfter(checkTime)) {
                        continue;
                    }
                    boolean claimed = item.getClaimTime() != null && !item.getClaimTime().isAfter(checkTime);
                    if (!claimed) {
                        queueLen++;
                    }
                }
                if (queueLen > peakLength) {
                    peakLength = queueLen;
                }
            }

            for (DispenseQueueItem item : allItems) {
                if (item.getClaimTime() != null
                        && !item.getClaimTime().isBefore(slotStart)
                        && !item.getClaimTime().isAfter(slotEnd)
                        && item.getEnqueueTime() != null) {
                    totalWaitMinutes += Duration.between(item.getEnqueueTime(), item.getClaimTime()).toMinutes();
                    claimedCount++;
                }
            }

            if (peakLength > 0 || claimedCount > 0) {
                QueueStatisticsDTO dto = new QueueStatisticsDTO();
                dto.setTimeSlot(String.format("%02d:00-%02d:00", hour, hour + 1));
                dto.setPeakQueueLength(peakLength);
                dto.setAvgWaitMinutes(claimedCount > 0 ? totalWaitMinutes / claimedCount : 0);
                result.add(dto);
            }
        }

        return result;
    }

    private QueueItemDTO enrichWithPosition(DispenseQueueItem item) {
        QueueItemDTO dto = QueueItemDTO.fromEntity(item);
        if (item.getStatus() == QueueItemStatus.WAITING) {
            List<DispenseQueueItem> waitingItems = queueItemRepository.findAllWaitingOrderedByChannel(item.getChannel());
            int avgMinutes = item.getChannel() == DispenseChannel.FAST
                    ? AVERAGE_FAST_DISPENSE_MINUTES : AVERAGE_NORMAL_DISPENSE_MINUTES;
            for (int i = 0; i < waitingItems.size(); i++) {
                if (waitingItems.get(i).getId().equals(item.getId())) {
                    dto.setPosition(i + 1);
                    dto.setEstimatedWaitMinutes((i + 1) * avgMinutes);
                    break;
                }
            }
        } else if (item.getStatus() == QueueItemStatus.PROCESSING) {
            dto.setPosition(0);
            dto.setEstimatedWaitMinutes(0);
        }
        return dto;
    }

    private String formatDuration(Double seconds) {
        if (seconds == null || seconds == 0) {
            return "0分0秒";
        }
        long totalSeconds = seconds.longValue();
        long minutes = totalSeconds / 60;
        long secs = totalSeconds % 60;
        return minutes + "分" + secs + "秒";
    }
}
