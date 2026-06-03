package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.TimelineDTO;
import sorbonne.professional_website.entity.Timeline;

public final class TimelineMapper {

    private TimelineMapper() {
    }

    public static TimelineDTO toDTO(Timeline timeline) {
        if (timeline == null) {
            return null;
        }

        return new TimelineDTO(
                timeline.getTitle(),
                timeline.getDescription(),
                ExperienceMapper.toDTOList(timeline.getExperiences())
        );
    }
}