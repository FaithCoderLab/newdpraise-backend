package faithcoderlab.newdpraise.domain.song;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.beatroot.BeatRootOnsetEventHandler;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.AudioFormat;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SongAnalysisService {

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

  private final YoutubeDownloader youtubeDownloader;

  public MusicAnalysisResult analyzeMusic(String youtubeUrl) {
    try {
      String videoId = extractVideoId(youtubeUrl);
      if (videoId == null) {
        log.error("유효하지 않은 유튜브 URL: {}", youtubeUrl);
        return null;
      }

      File audioFile = downloadAudio(videoId);
      if (audioFile == null) {
        return null;
      }

      String key = detectKey(audioFile);
      int bpm = detectBPM(audioFile);

      audioFile.delete();

      return new MusicAnalysisResult(key, bpm);
    } catch (Exception e) {
      log.error("유튜브 URL에서 음악 분석 중 오류 발생: {}", youtubeUrl, e);
      return null;
    }
  }

  String extractVideoId(String youtubeUrl) {
    Pattern pattern = Pattern.compile("(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})");
    Matcher matcher = pattern.matcher(youtubeUrl);
    return matcher.find() ? matcher.group(1) : null;
  }

  private File downloadAudio(String videoId) {
    try {
      RequestVideoInfo request = new RequestVideoInfo(videoId);
      Response<VideoInfo> response = youtubeDownloader.getVideoInfo(request);
      VideoInfo videoInfo = response.data();

      if (videoInfo == null) {
        log.error("비디오 정보를 가져오지 못했습니다: {}", videoId);
        return null;
      }

      List<AudioFormat> audioFormats = videoInfo.audioFormats();
      if (audioFormats.isEmpty()) {
        log.error("사용 가능한 오디오 형식이 없습니다. 비디오: {}", videoId);
        return null;
      }

      AudioFormat audioFormat = audioFormats.get(0);

      File outputDir = new File(System.getProperty("java.io.tmpdir"));
      RequestVideoFileDownload downloadRequest = new RequestVideoFileDownload(audioFormat)
          .saveTo(outputDir)
          .renameTo(videoId + "." + audioFormat.extension());

      Response<File> downloadResponse = youtubeDownloader.downloadVideoFile(downloadRequest);
      return downloadResponse.data();
    } catch (Exception e) {
      log.error("유튜브에서 오디오 다운로드 중 오류 발생: {}", videoId, e);
      return null;
    }
  }

  String detectKey(File audioFile) {
    try {
      AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);

      float sampleRate = audioInputStream.getFormat().getSampleRate();
      int bufferSize = 1024;
      int overlap = 0;

      int[] pitchDistribution = new int[12];

      AudioDispatcher dispatcher = new AudioDispatcher(
          new JVMAudioInputStream(audioInputStream),
          bufferSize,
          overlap
      );

      dispatcher.addAudioProcessor(new PitchProcessor(
          PitchProcessor.PitchEstimationAlgorithm.YIN,
          sampleRate,
          bufferSize,
          new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult result,
                AudioEvent event) {
              if (result.getPitch() != -1) {
                float pitch = result.getPitch();
                int pitchClass = (int) (12 * (Math.log(pitch / 440) / Math.log(2))) % 12;
                if (pitchClass < 0) pitchClass += 12;

                pitchDistribution[pitchClass]++;
              }
            }
          }
      ));

      dispatcher.run();

      int maxCount = -1;
      int dominantPitchClass = 0;

      for (int i = 0; i < 12; i++) {
        if (pitchDistribution[i] > maxCount) {
          maxCount = pitchDistribution[i];
          dominantPitchClass = i;
        }
      }

      boolean isMajor = pitchDistribution[(dominantPitchClass + 4) % 12] >
          pitchDistribution[(dominantPitchClass + 3) % 12];

      return PITCH_CLASS_TO_KEY.get(dominantPitchClass) + (isMajor ? "" : "m");
    } catch (Exception e) {
      log.error("키 탐지 중 오류 발생: {}", e.getMessage(), e);
      return null;
    }
  }

  int detectBPM(File audioFile) {
    try {
      AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);

      float sampleRate = audioInputStream.getFormat().getSampleRate();
      int bufferSize = 1024;
      int overlap = 0;

      final List<Float> beatTimes = new ArrayList<>();

      OnsetHandler beatHandler = (time, salience) -> {
        beatTimes.add((float) time);
      };

      BeatRootOnsetEventHandler onsetHandler = new BeatRootOnsetEventHandler();

      AudioDispatcher dispatcher = new AudioDispatcher(
          new JVMAudioInputStream(audioInputStream),
          bufferSize,
          overlap
      );

      ComplexOnsetDetector onsetDetector = new ComplexOnsetDetector((int) sampleRate);
      onsetDetector.setHandler(onsetHandler);
      dispatcher.addAudioProcessor(onsetDetector);

      dispatcher.run();

      onsetHandler.trackBeats(beatHandler);

      if (beatTimes.size() < 2) {
        return 0;
      }

      float totalIntervals = 0;
      int count = 0;

      for (int i = 1; i < beatTimes.size(); i++) {
        float interval = beatTimes.get(i) - beatTimes.get(i - 1);
        if (interval > 0.1 && interval < 2.0) {
          totalIntervals += interval;
          count++;
        }
      }

      if (count == 0) {
        return 0;
      }

      float averageInterval = totalIntervals / count;
      int bpm = Math.round(60f / averageInterval);

      if (bpm < 60) {
        bpm *= 2;
      }
      if (bpm > 200) {
        bpm /= 2;
      }

      return bpm;
    } catch (Exception e) {
      log.error("BPM 탐지 중 오류 발생: {}", e.getMessage(), e);
      return 0;
    }
  }

  @Data
  @AllArgsConstructor
  public static class MusicAnalysisResult {
    private String key;
    private int bpm;
  }
}
