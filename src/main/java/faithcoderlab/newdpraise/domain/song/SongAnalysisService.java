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
import faithcoderlab.newdpraise.global.exception.SongAnalysisException;
import java.io.File;
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

  private static final Pattern YOUTUBE_URL_PATTERN = Pattern.compile(
      "(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})"
  );

  private static final int AUDIO_BUFFER_SIZE = 1024;
  private static final int AUDIO_OVERLAP = 0;

  private static final float MIN_BEAT_INTERVAL = 0.1f;
  private static final float MAX_BEAT_INTERVAL = 2.0f;
  private static final int MIN_BPM = 60;
  private static final int MAX_BPM = 200;

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
    String videoId = extractVideoId(youtubeUrl);
    if (videoId == null) {
      throw new SongAnalysisException("유효하지 않은 유튜브 URL입니다: " + youtubeUrl);
    }

    File audioFile = null;
    try {
      audioFile = downloadAudio(videoId);
      String key = detectKey(audioFile);
      int bpm = detectBPM(audioFile);

      return new MusicAnalysisResult(key, bpm);
    } finally {
      if (audioFile != null && audioFile.exists()) {
        audioFile.delete();
      }
    }
  }

  String extractVideoId(String youtubeUrl) {
    Matcher matcher = YOUTUBE_URL_PATTERN.matcher(youtubeUrl);
    return matcher.find() ? matcher.group(1) : null;
  }

  private File downloadAudio(String videoId) {
    try {
      RequestVideoInfo request = new RequestVideoInfo(videoId);
      Response<VideoInfo> response = youtubeDownloader.getVideoInfo(request);
      VideoInfo videoInfo = response.data();

      if (videoInfo == null) {
        throw new SongAnalysisException("비디오 정보를 가져올 수 없습니다. 비디오 ID: " + videoId);
      }

      List<AudioFormat> audioFormats = videoInfo.audioFormats();
      if (audioFormats.isEmpty()) {
        throw new SongAnalysisException("사용 가능한 오디오 형식이 없습니다. 비디오 ID: " + videoId);
      }

      AudioFormat audioFormat = audioFormats.get(0);
      File outputDir = new File(System.getProperty("java.io.tmpdir"));
      RequestVideoFileDownload downloadRequest = new RequestVideoFileDownload(audioFormat)
          .saveTo(outputDir)
          .renameTo(videoId + "." + audioFormat.extension());

      Response<File> downloadResponse = youtubeDownloader.downloadVideoFile(downloadRequest);
      File downloadedFile = downloadResponse.data();

      if (downloadedFile == null) {
        throw new SongAnalysisException("오디오 파일 다운로드에 실패했습니다. 비디오 ID: " + videoId);
      }

      return downloadedFile;
    } catch (Exception e) {
      throw new SongAnalysisException("예기치 않은 오류 발생: " + videoId, e);
    }
  }

  String detectKey(File audioFile) {
    try {
      AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);

      float sampleRate = audioInputStream.getFormat().getSampleRate();
      int[] pitchDistribution = new int[12];

      AudioDispatcher dispatcher = new AudioDispatcher(
          new JVMAudioInputStream(audioInputStream),
          AUDIO_BUFFER_SIZE,
          AUDIO_OVERLAP
      );

      dispatcher.addAudioProcessor(new PitchProcessor(
          PitchProcessor.PitchEstimationAlgorithm.YIN,
          sampleRate,
          AUDIO_BUFFER_SIZE,
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
      throw new SongAnalysisException("키 탐지에 실패했습니다.", e);
    }
  }

  int detectBPM(File audioFile) {
    try {
      AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);

      float sampleRate = audioInputStream.getFormat().getSampleRate();
      final List<Float> beatTimes = new ArrayList<>();

      OnsetHandler beatHandler = (time, salience) -> {
        beatTimes.add((float) time);
      };

      BeatRootOnsetEventHandler onsetHandler = new BeatRootOnsetEventHandler();

      AudioDispatcher dispatcher = new AudioDispatcher(
          new JVMAudioInputStream(audioInputStream),
          AUDIO_BUFFER_SIZE,
          AUDIO_OVERLAP
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
        if (interval > MIN_BEAT_INTERVAL && interval < MAX_BEAT_INTERVAL) {
          totalIntervals += interval;
          count++;
        }
      }

      if (count == 0) {
        return 0;
      }

      float averageInterval = totalIntervals / count;
      int bpm = Math.round(60f / averageInterval);

      if (bpm < MIN_BPM) {
        bpm *= 2;
      }
      if (bpm > MAX_BPM) {
        bpm /= 2;
      }

      return bpm;
    } catch (Exception e) {
      log.error("BPM 탐지 중 오류 발생: {}", e.getMessage(), e);
      throw new SongAnalysisException("BPM 탐지에 실패했습니다.", e);
    }
  }

  @Data
  @AllArgsConstructor
  public static class MusicAnalysisResult {
    private String key;
    private int bpm;
  }
}
