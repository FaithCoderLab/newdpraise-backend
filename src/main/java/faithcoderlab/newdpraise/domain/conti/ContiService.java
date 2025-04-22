package faithcoderlab.newdpraise.domain.conti;

import faithcoderlab.newdpraise.domain.conti.dto.ContiCreateRequest;
import faithcoderlab.newdpraise.domain.conti.dto.ContiSearchRequest;
import faithcoderlab.newdpraise.domain.conti.dto.ContiUpdateRequest;
import faithcoderlab.newdpraise.domain.song.Song;
import faithcoderlab.newdpraise.domain.song.SongRepository;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.global.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  @Transactional
  public Conti updateConti(Long contiId, ContiUpdateRequest request, User user) {
    Conti conti = getContiByIdAndCreator(contiId, user);

    if (request.getTitle() != null) {
      conti.setTitle(request.getTitle());
    }

    if (request.getDescription() != null) {
      conti.setDescription(request.getDescription());
    }

    if (request.getScheduledAt() != null) {
      conti.setScheduledAt(request.getScheduledAt());
    }

    if (request.getStatus() != null) {
      conti.setStatus(request.getStatus());
    }

    if (request.getSongs() != null) {
      List<Song> updatedSongs = request.getSongs().stream()
          .map(songDto -> {
            Song song;
            if (songDto.getId() != null) {
              song = songRepository.findById(songDto.getId())
                  .orElseThrow(
                      () -> new ResourceNotFoundException("곡을 찾을 수 없습니다. ID: " + songDto.getId()));

              song.setTitle(songDto.getTitle());
              song.setOriginalKey(songDto.getOriginalKey());
              song.setPerformanceKey(songDto.getPerformanceKey());
              song.setArtist(songDto.getArtist());
              song.setYoutubeUrl(songDto.getYoutubeUrl());
              song.setReferenceUrl(songDto.getReferenceUrl());
              song.setSpecialInstructions(songDto.getSpecialInstructions());
              song.setBpm(songDto.getBpm());
            } else {
              song = Song.builder()
                  .title(songDto.getTitle())
                  .originalKey(songDto.getOriginalKey())
                  .performanceKey(songDto.getPerformanceKey())
                  .artist(songDto.getArtist())
                  .youtubeUrl(songDto.getYoutubeUrl())
                  .referenceUrl(songDto.getReferenceUrl())
                  .specialInstructions(songDto.getSpecialInstructions())
                  .bpm(songDto.getBpm())
                  .build();
            }
            return song;
          })
          .collect(Collectors.toList());

      List<Song> savedSongs = songRepository.saveAll(updatedSongs);
      conti.setSongs(savedSongs);
    }

    return contiRepository.save(conti);
  }

  @Transactional(readOnly = true)
  public List<Conti> getUserContiList(User user) {
    return contiRepository.findByCreatorOrderByScheduledAtDesc(user);
  }

  @Transactional(readOnly = true)
  public Page<Conti> getUserContiList(User user, Pageable pageable) {
    return contiRepository.findByCreator(user, pageable);
  }

  @Transactional(readOnly = true)
  public Conti getContiById(Long contiId) {
    return contiRepository.findById(contiId)
        .orElseThrow(() -> new ResourceNotFoundException("콘티를 찾을 수 없습니다. ID: " + contiId));
  }

  @Transactional(readOnly = true)
  public Conti getContiByIdAndCreator(Long contiId, User creator) {
    Conti conti = getContiById(contiId);
    if (conti.getCreator() != null && !conti.getCreator().getId().equals(creator.getId())) {
      throw new ResourceNotFoundException("해당 사용자의 콘티를 찾을 수 없습니다. ID: " + contiId);
    }
    return conti;
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

  @Transactional(readOnly = true)
  public List<Conti> searchByDateRange(LocalDate startDate, LocalDate endDate) {
    return contiRepository.findByScheduledAtBetweenOrderByScheduledAtDesc(startDate, endDate);
  }

  @Transactional(readOnly = true)
  public List<Conti> searchByTitle(String keyword) {
    return contiRepository.findByTitleContainingIgnoreCaseOrderByScheduledAtDesc(keyword);
  }

  @Transactional(readOnly = true)
  public List<Conti> searchByTitleForUser(User user, String keyword) {
    return contiRepository.findByCreatorAndTitleContainingIgnoreCaseOrderByScheduledAtDesc(user,
        keyword);
  }

  @Transactional(readOnly = true)
  public List<Conti> searchByDateRangeAndTitle(LocalDate startDate, LocalDate endDate,
      String keyword) {
    return contiRepository.findByScheduledAtBetweenAndTitleContainingIgnoreCaseOrderByScheduledAtDesc(
        startDate, endDate, keyword);
  }

  @Transactional(readOnly = true)
  public Page<Conti> advancedSearch(ContiSearchRequest request, Pageable pageable) {
    return contiRepository.searchContis(
        request.getStartDate(),
        request.getEndDate(),
        request.getTitle(),
        request.getCreatorId(),
        request.getStatus(),
        pageable
    );
  }

  @Transactional(readOnly = true)
  public List<Conti> getUpcomingContis(LocalDate date) {
    return contiRepository.findByScheduledAtGreaterThanEqualOrderByScheduledAtAsc(date);
  }

  @Transactional(readOnly = true)
  public List<Conti> getPastContis(LocalDate date) {
    return contiRepository.findByScheduledAtLessThanEqualOrderByScheduledAtDesc(date);
  }

  @Transactional(readOnly = true)
  public List<Conti> getContisByStatus(ContiStatus status) {
    return contiRepository.findByStatus(status);
  }

  @Transactional(readOnly = true)
  public List<Conti> getUserContisByStatus(User user, ContiStatus status) {
    return contiRepository.findByCreatorAndStatus(user, status);
  }
}
