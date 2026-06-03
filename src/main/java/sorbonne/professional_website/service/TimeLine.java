package sorbonne.professional_website.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.TimelineDTO;
import sorbonne.professional_website.entity.Timeline;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.mapper.TimelineMapper;
import sorbonne.professional_website.repository.TimelineRepository;

import java.util.List;

@Service
@Transactional
public class TimelineService {

    private final TimelineRepository rpTimeline;

    public TimelineService(TimelineRepository rpTimeline) {
        this.rpTimeline = rpTimeline;
    }

    public void createTimeline(TimelineDTO timelineDTO) {
        Timeline timeline = TimelineMapper.fromCreateDTO(timelineDTO);
        rpTimeline.save(timeline);
    }

    @Transactional(readOnly = true)
    public List<TimelineDTO> getAllTimelines() {
        return rpTimeline.findAll()
                .stream()
                .map(TimelineMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public TimelineDTO getTimelineById(Long timelineId) {
        Timeline timeline = findTimelineById(timelineId);
        return TimelineMapper.toDTO(timeline);
    }

    public void updateTimeline(Long timelineId, TimelineDTO timelineDTO) {
        Timeline timeline = findTimelineById(timelineId);

        TimelineMapper.updateEntityFromDTO(timeline, timelineDTO);

        rpTimeline.save(timeline);
    }

    public void deleteTimeline(Long timelineId) {
        Timeline timeline = findTimelineById(timelineId);
        rpTimeline.delete(timeline);
    }

    private Timeline findTimelineById(Long timelineId) {
        return rpTimeline.findById(timelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Timeline"));
    }
}