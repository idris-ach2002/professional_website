package sorbonne.professional_website.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.ExperienceDTO;
import sorbonne.professional_website.entity.Experience;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.mapper.ExperienceMapper;
import sorbonne.professional_website.repository.ExperienceRepository;

import java.util.List;

@Service
@Transactional
public class ExperienceService {

    private final ExperienceRepository rpExperience;

    public ExperienceService(ExperienceRepository rpExperience) {
        this.rpExperience = rpExperience;
    }

    public void createExperience(ExperienceDTO experienceDTO) {
        Experience experience = ExperienceMapper.fromCreateDTO(experienceDTO);
        rpExperience.save(experience);
    }

    @Transactional(readOnly = true)
    public List<ExperienceDTO> getAllExperiences() {
        return rpExperience.findAll()
                .stream()
                .map(ExperienceMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExperienceDTO getExperienceById(Long experienceId) {
        Experience experience = findExperienceById(experienceId);
        return ExperienceMapper.toDTO(experience);
    }

    public void updateExperience(Long experienceId, ExperienceDTO experienceDTO) {
        Experience experience = findExperienceById(experienceId);

        ExperienceMapper.updateEntityFromDTO(experience, experienceDTO);

        rpExperience.save(experience);
    }

    public void deleteExperience(Long experienceId) {
        Experience experience = findExperienceById(experienceId);
        rpExperience.delete(experience);
    }

    private Experience findExperienceById(Long experienceId) {
        return rpExperience.findById(experienceId)
                .orElseThrow(() -> new ResourceNotFoundException("Experience"));
    }
}