package faithcoderlab.newdpraise.domain.conti.template;

import faithcoderlab.newdpraise.domain.conti.Conti;
import faithcoderlab.newdpraise.domain.conti.ContiRepository;
import faithcoderlab.newdpraise.domain.conti.ContiService;
import faithcoderlab.newdpraise.domain.conti.ContiStatus;
import faithcoderlab.newdpraise.domain.conti.template.dto.ContiTemplateCreateRequest;
import faithcoderlab.newdpraise.domain.conti.template.dto.ContiTemplateUpdateRequest;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.global.exception.AuthenticationException;
import faithcoderlab.newdpraise.global.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContiTemplateService {

  private final ContiTemplateRepository contiTemplateRepository;
  private final ContiRepository contiRepository;
  private final ContiService contiService;

  @Transactional
  public ContiTemplate createTemplate(ContiTemplateCreateRequest request, User creator) {
    if (request.getContiId() != null) {
      Conti conti = contiRepository.findById(request.getContiId())
          .orElseThrow(() -> new ResourceNotFoundException("콘티를 찾을 수 없습니다. ID: " + request.getContiId()));

      if (!conti.getCreator().equals(creator)) {
        throw new AuthenticationException("해당 콘티로부터 템플릿을 생성할 권한이 없습니다.");
      }

      ContiTemplate template = ContiTemplate.fromConti(conti, request.getName(),
          request.isPublic());
      template.setDescription(request.getDescription());

      return contiTemplateRepository.save(template);
    }
    else {
      ContiTemplate template = ContiTemplate.builder()
          .name(request.getName())
          .description(request.getDescription())
          .creator(creator)
          .isPublic(request.isPublic())
          .usageCount(0)
          .build();

      return contiTemplateRepository.save(template);
    }
  }

  public ContiTemplate getTemplate(Long templateId) {
    return contiTemplateRepository.findById(templateId)
        .orElseThrow(() -> new ResourceNotFoundException("템플릿을 찾을 수 없습니다. ID: " + templateId));
  }

  public ContiTemplate getAccessibleTemplate(Long templateId, User user) {
    ContiTemplate template = getTemplate(templateId);

    if (!template.isPublic() && !template.getCreator().getId().equals(user.getId())) {
      throw new AuthenticationException("해당 템플릿에 접근할 권한이 없습니다.");
    }

    return template;
  }

  @Transactional
  public ContiTemplate updateTemplate(Long templateId, ContiTemplateUpdateRequest request,
      User user) {
    ContiTemplate template = getTemplate(templateId);

    if (!template.getCreator().getId().equals(user.getId())) {
      throw new AuthenticationException("해당 템플릿을 수정할 권한이 없습니다.");
    }

    if (request.getName() != null) {
      template.setName(request.getName());
    }

    if (request.getDescription() != null) {
      template.setDescription(request.getDescription());
    }

    if (request.getIsPublic() != null) {
      template.setPublic(request.getIsPublic());
    }

    return contiTemplateRepository.save(template);
  }

  @Transactional
  public void deleteTemplate(Long templateId, User user) {
    ContiTemplate template = getTemplate(templateId);

    if (!template.getCreator().getId().equals(user.getId())) {
      throw new AuthenticationException("해당 템플릿을 삭제할 권한이 없습니다.");
    }

    contiTemplateRepository.delete(template);
  }

  public List<ContiTemplate> getUserTemplates(User user) {
    return contiTemplateRepository.findByCreator(user);
  }

  public Page<ContiTemplate> getUserTemplates(User user, Pageable pageable) {
    return contiTemplateRepository.findByCreator(user, pageable);
  }

  public List<ContiTemplate> getPublicTemplates() {
    return contiTemplateRepository.findByIsPublicTrue();
  }

  public Page<ContiTemplate> getPublicTemplates(Pageable pageable) {
    return contiTemplateRepository.findByIsPublicTrue(pageable);
  }

  public List<ContiTemplate> getAccessibleTemplates(User user) {
    return contiTemplateRepository.findAccessibleTemplates(user.getId());
  }

  public Page<ContiTemplate> getAccessibleTemplates(User user, Pageable pageable) {
    return contiTemplateRepository.findAccessibleTemplates(user.getId(), pageable);
  }

  public List<ContiTemplate> getPopularTemplates(int limit) {
    return contiTemplateRepository.findPopularTemplates(Pageable.ofSize(limit));
  }

  @Transactional
  public Conti applyTemplate(Long templateId, User user) {
    ContiTemplate template = getAccessibleTemplate(templateId, user);

    Conti conti = template.toConti(user);
    conti.setStatus(ContiStatus.DRAFT);

    contiTemplateRepository.save(template);

    return contiRepository.save(conti);
  }
}
