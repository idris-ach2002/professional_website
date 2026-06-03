package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.request.TimelineRequestDTO;
import sorbonne.professional_website.dto.response.TimelineResponseDTO;
import sorbonne.professional_website.entity.Experience;
import sorbonne.professional_website.entity.Timeline;

import java.util.List;

public final class TimelineMapper {

    private TimelineMapper() {
    }

    public static TimelineResponseDTO toResponse(Timeline timeline) {
        if (timeline == null) {
            return null;
        }

        return new TimelineResponseDTO(
                timeline.getId(),
                timeline.getTitle(),
                timeline.getDescription(),
                ExperienceMapper.toResponseList(timeline.getExperiences())
        );
    }

    public static Timeline fromRequest(TimelineRequestDTO timelineDTO) {
        if (timelineDTO == null) {
            return null;
        }

        Timeline timeline = new Timeline();
        setTimelineProperties(timeline, timelineDTO);

        return timeline;
    }

    public static void updateEntityFromRequest(Timeline timeline, TimelineRequestDTO timelineDTO) {
        if (timeline == null || timelineDTO == null) {
            return;
        }

        setTimelineProperties(timeline, timelineDTO);
    }

    private static void setTimelineProperties(Timeline timeline, TimelineRequestDTO timelineDTO) {
        timeline.setTitle(timelineDTO.title());
        timeline.setDescription(timelineDTO.description());

        timeline.getExperiences().clear();

        List<Experience> experiences = ExperienceMapper.fromRequestList(timelineDTO.experiences());

        for (Experience experience : experiences) {
            experience.setTimeline(timeline);
            timeline.getExperiences().add(experience);
        }
    }
}
