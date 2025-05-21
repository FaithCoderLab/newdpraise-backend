package faithcoderlab.newdpraise.domain.song.service;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import faithcoderlab.newdpraise.domain.song.PitchDistribution;
import faithcoderlab.newdpraise.domain.song.dto.AudioMetadata;
import faithcoderlab.newdpraise.global.exception.AudioProcessingException;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AudioMetadataService {

  private static final int BUFFER_SIZE = 1024;
  private static final int OVERLAP = 0;
  private static final float REFERENCE_FREQUENCY_A4 = 440.0f;
  private static final double OCTAVE_RATIO = 2.0;
  private static final int SEMITONES_PER_OCTAVE = 12;

  private static final Map<Integer, String> PITCH_CLASS_TO_KEY = Map.ofEntries(
      Map.entry(0, "C"),
      Map.entry(1, "C#"),
      Map.entry(2, "D"),
      Map.entry(3, "D#"),
      Map.entry(4, "E"),
      Map.entry(5, "F"),
      Map.entry(6, "F#"),
      Map.entry(7, "G"),
      Map.entry(8, "G#"),
      Map.entry(9, "A"),
      Map.entry(10, "A#"),
      Map.entry(11, "B")
  );

  public AudioMetadata extractMetadata(File audioFile) {
    try {
      String key = detectKey(audioFile);
      int bpm = calculateBPM(audioFile);
      float durationInSeconds = calculateDuration(audioFile);

      return AudioMetadata.builder()
          .key(key)
          .bpm(bpm)
          .durationSeconds((long) durationInSeconds)
          .build();
    } catch (Exception e) {
      log.error("음원 메타데이터 추출 중 오류 발생: {}", e.getMessage(), e);
      throw new AudioProcessingException("음원 메타데이터 추출 중 오류가 발생했습니다: " + e.getMessage(), e);
    }
  }

  private String detectKey(File audioFile) throws Exception {
    AudioInputStream audioInputStream = null;
    try {
      audioInputStream = AudioSystem.getAudioInputStream(audioFile);
      float sampleRate = audioInputStream.getFormat().getSampleRate();

      PitchDistribution pitchDistribution = new PitchDistribution();

      AudioDispatcher dispatcher = new AudioDispatcher(
          new JVMAudioInputStream(audioInputStream),
          BUFFER_SIZE,
          OVERLAP
      );

      dispatcher.addAudioProcessor(new PitchProcessor(
          PitchEstimationAlgorithm.YIN,
          sampleRate,
          BUFFER_SIZE,
          (PitchDetectionResult result, AudioEvent event) -> {
            if (result.getPitch() != -1) {
              float pitch = result.getPitch();
              int pitchClass = (int) (SEMITONES_PER_OCTAVE *
                  (Math.log(pitch / REFERENCE_FREQUENCY_A4) / Math.log(OCTAVE_RATIO)))
                  % SEMITONES_PER_OCTAVE;

              if (pitchClass < 0) {
                pitchClass += SEMITONES_PER_OCTAVE;
              }

              pitchDistribution.addPitchClass(pitchClass);
            }
          }
      ));

      dispatcher.run();

      if (!pitchDistribution.hasEnoughData()) {
        return "Unknown";
      }

      int dominantPitchClass = pitchDistribution.getDominantPitchClass();
      boolean isMajor = pitchDistribution.isMajor();

      return PITCH_CLASS_TO_KEY.get(dominantPitchClass) + (isMajor ? "" : "m");
    } catch (UnsupportedAudioFileException e) {
      log.error("지원되지 않는 오디오 파일 형식: {}", e.getMessage());
      return "Unknown";
    } finally {
      if (audioInputStream != null) {
        try {
          audioInputStream.close();
        } catch (Exception e) {
          log.error("오디오 스트림 닫기 실패: {}", e.getMessage());
        }
      }
    }
  }

  private int calculateBPM(File audioFile) {
    // TODO: BPM 계산 로직 추가
    try {
      return (int) (Math.random() * 60) + 90;
    } catch (Exception e) {
      log.error("BPM 계산 중 오류: {}", e.getMessage());
      return 0;
    }
  }

  private float calculateDuration(File audioFile) {
    try {
      AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
      float frameRate = audioInputStream.getFormat().getFrameRate();
      long frameLength = audioInputStream.getFrameLength();

      if (frameRate <= 0 || frameLength <= 0) {
        return 0f;
      }

      return frameLength / frameRate;
    } catch (Exception e) {
      log.error("오디오 파일 길이 계산 중 오류: {}", e.getMessage());
      return 0f;
    }
  }

  private static class PitchDistribution {
    private Map<Integer, Integer> pitchClassCounts = new HashMap<>();
    private static final int MIN_SAMPLES = 100;

    private static final List<Integer> MAJOR_SCALE = List.of(0, 2, 4, 5, 7, 9, 11);
    private static final List<Integer> MINOR_SCALE = List.of(0, 2, 3, 5, 7, 8, 10);

    public void addPitchClass(int pitchClass) {
      pitchClassCounts.put(pitchClass, pitchClassCounts.getOrDefault(pitchClass, 0) + 1);
    }

    public boolean hasEnoughData() {
      return pitchClassCounts.values().stream().mapToInt(Integer::intValue).sum() >= MIN_SAMPLES;
    }

    public int getDominantPitchClass() {
      return pitchClassCounts.entrySet().stream()
          .max(Map.Entry.comparingByValue())
          .map(Map.Entry::getKey)
          .orElse(0);
    }

    public boolean isMajor() {
      int tonic = getDominantPitchClass();

      int majorScore = 0;
      int minorScore = 0;

      for (Map.Entry<Integer, Integer> entry : pitchClassCounts.entrySet()) {
        int pitchClass = entry.getKey();
        int count = entry.getValue();

        int relativeInterval = (pitchClass - tonic + 12) % 12;

        if (MAJOR_SCALE.contains(relativeInterval)) {
          majorScore += count;
        }

        if (MINOR_SCALE.contains(relativeInterval)) {
          minorScore += count;
        }
      }

      return majorScore >= minorScore;
    }
  }
}
