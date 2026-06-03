package sorbonne.professional_website.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.request.TimelineRequestDTO;
import sorbonne.professional_website.dto.response.TimelineResponseDTO;
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

    public void createTimeline(TimelineRequestDTO timelineRequestDTO) {
        Timeline timeline = TimelineMapper.fromRequest(timelineRequestDTO);
        rpTimeline.save(timeline);
    }

    @Transactional(readOnly = true)
    public List<TimelineResponseDTO> getAllTimelines() {
        return rpTimeline.findAll()
                .stream()
                .map(TimelineMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TimelineResponseDTO getTimelineById(Long timelineId) {
        Timeline timeline = findTimelineById(timelineId);
        return TimelineMapper.toResponse(timeline);
    }

    public void updateTimeline(Long timelineId, TimelineRequestDTO timelineRequestDTO) {
        Timeline timeline = findTimelineById(timelineId);
        TimelineMapper.updateEntityFromRequest(timeline, timelineRequestDTO);
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
