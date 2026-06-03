package sorbonne.professional_website.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.request.ExperienceRequestDTO;
import sorbonne.professional_website.dto.response.ExperienceResponseDTO;
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

    public void createExperience(ExperienceRequestDTO experienceRequestDTO) {
        Experience experience = ExperienceMapper.fromRequest(experienceRequestDTO);
        rpExperience.save(experience);
    }

    @Transactional(readOnly = true)
    public List<ExperienceResponseDTO> getAllExperiences() {
        return rpExperience.findAll()
                .stream()
                .map(ExperienceMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExperienceResponseDTO getExperienceById(Long experienceId) {
        Experience experience = findExperienceById(experienceId);
        return ExperienceMapper.toResponse(experience);
    }

    public void updateExperience(Long experienceId, ExperienceRequestDTO experienceRequestDTO) {
        Experience experience = findExperienceById(experienceId);
        ExperienceMapper.updateEntityFromRequest(experience, experienceRequestDTO);
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
