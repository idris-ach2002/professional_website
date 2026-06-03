package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.TimelineDTO;
import sorbonne.professional_website.entity.Experience;
import sorbonne.professional_website.entity.Timeline;

import java.util.List;

public final class TimelineMapper {

    private TimelineMapper() {
    }

    public static TimelineDTO toDTO(Timeline timeline) {
        if (timeline == null) {
            return null;
        }

        return new TimelineDTO(
                timeline.getId(),
                timeline.getTitle(),
                timeline.getDescription(),
                ExperienceMapper.toDTOList(timeline.getExperiences())
        );
    }

    public static Timeline fromCreateDTO(TimelineDTO timelineDTO) {
        if (timelineDTO == null) {
            return null;
        }

        Timeline timeline = new Timeline();

        timeline.setTitle(timelineDTO.title());
        timeline.setDescription(timelineDTO.description());

        List<Experience> experiences = ExperienceMapper.fromDTOList(timelineDTO.experiences());

        for (Experience experience : experiences) {
            experience.setTimeline(timeline);
        }

        timeline.setExperiences(experiences);

        return timeline;
    }

    public static void updateEntityFromDTO(Timeline timeline, TimelineDTO timelineDTO) {
        if (timeline == null || timelineDTO == null) {
            return;
        }

        timeline.setTitle(timelineDTO.title());
        timeline.setDescription(timelineDTO.description());

        timeline.getExperiences().clear();

        List<Experience> experiences = ExperienceMapper.fromDTOList(timelineDTO.experiences());

        for (Experience experience : experiences) {
            experience.setTimeline(timeline);
            timeline.getExperiences().add(experience);
        }
    }
}