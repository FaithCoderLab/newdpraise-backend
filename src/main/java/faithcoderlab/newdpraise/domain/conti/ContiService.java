package faithcoderlab.newdpraise.domain.conti;

import faithcoderlab.newdpraise.domain.conti.dto.ContiCreateRequest;
import faithcoderlab.newdpraise.domain.song.Song;
import faithcoderlab.newdpraise.domain.song.SongRepository;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.global.exception.ResourceNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContiService {

  private final ContiRepository contiRepository;
  private final SongRepository songRepository;
  private final ContiParserService contiParserService;

  @Transactional
  public Conti createConti(ContiCreateRequest request, User creator) {
    Conti conti;

    if (request.getContiText() != null && !request.getContiText().isEmpty()) {
      conti = contiParserService.parseContiText(request.getContiText(), creator);
    } else {
      conti = Conti.builder()
          .title(request.getTitle())
          .description(request.getDescription())
          .scheduledAt(request.getScheduledAt())
          .creator(creator)
          .version("1.0")
          .status(ContiStatus.DRAFT)
          .build();

      if (request.getSongs() != null) {
        List<Song> songs = request.getSongs().stream()
            .map(songDto -> Song.builder()
                .title(songDto.getTitle())
                .originalKey(songDto.getOriginalKey())
                .performanceKey(songDto.getPerformanceKey())
                .artist(songDto.getArtist())
                .youtubeUrl(songDto.getYoutubeUrl())
                .referenceUrl(songDto.getReferenceUrl())
                .specialInstructions(songDto.getSpecialInstructions())
                .bpm(songDto.getBpm())
                .build())
            .collect(Collectors.toList());

        songs = songRepository.saveAll(songs);
        conti.setSongs(songs);
      }
    }

    return contiRepository.save(conti);
  }

  @Transactional(readOnly = true)
  public List<Conti> getUserContiList(User user) {
    return contiRepository.findByCreatorOrderByScheduledAtDesc(user);
  }

  @Transactional(readOnly = true)
  public Conti getContiById(Long contiId) {
    return contiRepository.findById(contiId)
        .orElseThrow(() -> new ResourceNotFoundException("콘티를 찾을 수 없습니다. ID: " + contiId));
  }

  @Transactional
  public void updateContiStatus(Long contiId, ContiStatus status) {
    Conti conti = getContiById(contiId);
    conti.setStatus(status);
    contiRepository.save(conti);
  }

  @Transactional
  public void deleteConti(Long contiId) {
    Conti conti = getContiById(contiId);
    contiRepository.delete(conti);
  }
}
