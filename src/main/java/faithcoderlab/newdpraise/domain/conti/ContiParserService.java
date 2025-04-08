package faithcoderlab.newdpraise.domain.conti;

import faithcoderlab.newdpraise.domain.user.User;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContiParserService {

  private static final Pattern DATE_PATTERN = Pattern.compile(
      "\\d{8}|" +
          "\\d{4}[-./]\\d{1,2}[-./]\\d{1,2}|" +
          "\\d{4}년\\s*\\d{1,2}월\\s*\\d{1,2}일|" +
          "\\d{1,2}[-./]\\d{1,2}[-./]\\d{4}|" +
          "\\d{1,2}[-./]\\d{1,2}"
  );

  private static final Pattern SONG_LINE_PATTERN = Pattern.compile(
      "(?im)^\\s*(?:(\\d+)[.\\s]+)([^\\r\\n]+)$");

  private static final Pattern KEY_PATTERN = Pattern.compile(
      "\\b([A-Ga-g][#b♯♭]?m?(?:-[A-Ga-g][#b♯♭]?m?)?)\\b");

  private static final Pattern YOUTUBE_URL_PATTERN = Pattern.compile(
      "(?:https?://)?(?:www\\.)?(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})(?:[?&][^\\s]*)?");

  private static final Pattern BPM_PATTERN = Pattern.compile(
      "\\b(?:BPM|템포)[:\\s]*([0-9]+)\\b");

  private static final Pattern ARTIST_PATTERN = Pattern.compile(
      "/\\s*([^/\\r\\n]+)(?:\\s*$|\\s*(?=[/\\(\\[]))");

  private static final Pattern THEME_PATTERN = Pattern.compile(
      "(?i)주제\\s*[:\\s]+(.+)$");

  private final SongRepository songRepository;

  public Conti parseContiText(String contiText, User creator) {
    log.debug("콘티 텍스트 파싱 시작: {} 글자", contiText.length());

    String title = extractThemeAsTitle(contiText);

    if (title == null || title.isEmpty()) {
      String[] lines = contiText.split("\\r?\\n");
      title = extractTitle(lines);
    }

    LocalDate performanceDate = extractDate(contiText);

    List<Song> songs = extractSongs(contiText);

    Conti conti = Conti.builder()
        .title(title)
        .performanceDate(performanceDate != null ? performanceDate : LocalDate.now())
        .creator(creator)
        .songs(songs)
        .version("1.0")
        .originalText(contiText)
        .status(ContiStatus.DRAFT)
        .build();

    log.debug("콘티 파싱 완료: 제목={}, 날짜={}, 곡 수={}",
        conti.getTitle(), conti.getPerformanceDate(), conti.getSongs().size());

    return conti;
  }

  private String extractThemeAsTitle(String text) {
    Pattern themePattern = Pattern.compile("(?i)주제\\s*[:\\s]+(.+)$", Pattern.MULTILINE);
    Matcher matcher = themePattern.matcher(text);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    return null;
  }

  private String extractTitle(String[] lines) {
    if (lines.length == 0) {
      return "무제";
    }

    String firstLine = lines[0].trim();

    return firstLine.isEmpty() ? "무제" : firstLine;
  }

  private LocalDate extractDate(String text) {
    Matcher matcher = DATE_PATTERN.matcher(text);
    if (matcher.find()) {
      String dateStr = matcher.group();

      try {
        if (dateStr.matches("\\d{8}")) {
          int year = Integer.parseInt(dateStr.substring(0, 4));
          int month = Integer.parseInt(dateStr.substring(4, 6));
          int day = Integer.parseInt(dateStr.substring(6, 8));
          return LocalDate.of(year, month, day);
        }

        Matcher ymdMatcher = Pattern.compile("(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})").matcher(dateStr);
        if (ymdMatcher.find()) {
          int year = Integer.parseInt(ymdMatcher.group(1));
          int month = Integer.parseInt(ymdMatcher.group(2));
          int day = Integer.parseInt(ymdMatcher.group(3));
          return LocalDate.of(year, month, day);
        }

        Matcher koreanMatcher = Pattern.compile("(\\d{4})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일").matcher(dateStr);
        if (koreanMatcher.find()) {
          int year = Integer.parseInt(koreanMatcher.group(1));
          int month = Integer.parseInt(koreanMatcher.group(2));
          int day = Integer.parseInt(koreanMatcher.group(3));
          return LocalDate.of(year, month, day);
        }

        Matcher mdyMatcher = Pattern.compile("(\\d{1,2})[-./](\\d{1,2})[-./](\\d{4})").matcher(dateStr);
        if (mdyMatcher.find()) {
          int month = Integer.parseInt(mdyMatcher.group(1));
          int day = Integer.parseInt(mdyMatcher.group(2));
          int year = Integer.parseInt(mdyMatcher.group(3));
          return LocalDate.of(year, month, day);
        }

        Matcher mdMatcher = Pattern.compile("(\\d{1,2})[-./](\\d{1,2})").matcher(dateStr);
        if (mdMatcher.find() && !mdyMatcher.find()) {
          int month = Integer.parseInt(mdMatcher.group(1));
          int day = Integer.parseInt(mdMatcher.group(2));
          return LocalDate.of(LocalDate.now().getYear(), month, day);
        }

        log.warn("인식할 수 없는 날짜 형식: {}", dateStr);
      } catch (Exception e) {
        log.warn("날짜 파싱 실패: {}", dateStr, e);
      }
    }

    return LocalDate.now();
  }

  private List<Song> extractSongs(String text) {
    List<Song> songs = new ArrayList<>();
    String[] lines = text.split("\\r?\\n");

    boolean foundSongLine = false;

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].trim();

      if (line.isEmpty()) {
        continue;
      }

      if (line.matches("(?i).*주제\\s*[:\\s]+.*")) {
        continue;
      }

      if (i == 0) {
        continue;
      }

      Matcher songNumberMatcher = Pattern.compile("^\\s*(\\d+)[.\\s]+").matcher(line);
      if (songNumberMatcher.find()) {
        foundSongLine = true;

        String songLine = line.substring(songNumberMatcher.end()).trim();

        String title = extractSongTitle(songLine);
        String key = extractKey(songLine);
        String artist = extractArtist(songLine);

        StringBuilder additionalInfo = new StringBuilder();
        int j = i + 1;
        while (j < lines.length) {
          String nextLine = lines[j].trim();

          if (nextLine.isEmpty() ||
              nextLine.matches("^\\s*\\d+[.\\s]+.*") ||
              nextLine.matches("(?i).*주제\\s*[:\\s]+.*")) {
            break;
          }

          additionalInfo.append(nextLine).append("\n");
          j++;
        }

        String youtubeUrl = extractYoutubeUrl(additionalInfo.toString());
        if (youtubeUrl == null) {
          youtubeUrl = extractYoutubeUrl(songLine);
        }

        String specialInstructions = extractSpecialInstructions(
            songLine + "\n" + additionalInfo.toString(),
            title, key, artist, youtubeUrl
        );

        if (title != null && !title.isEmpty()) {
          Song song = Song.builder()
              .title(title)
              .originalKey(key)
              .performanceKey(key)
              .artist(artist)
              .youtubeUrl(youtubeUrl)
              .specialInstructions(specialInstructions)
              .build();

          songs.add(song);
        }
      } else if (!foundSongLine) {
        continue;
      }
    }

    return songs;
  }

  private String extractSongTitle(String songLine) {
    String line = songLine.replaceAll(YOUTUBE_URL_PATTERN.pattern(), "").trim();

    int slashIndex = line.indexOf('/');
    if (slashIndex > 0) {
      line = line.substring(0, slashIndex).trim();
    }

    line = line.replaceAll("\\([^)]*\\)", "").replaceAll("\\[[^\\]]*\\]", "").trim();

    String[] words = line.split("\\s+");
    StringBuilder titleBuilder = new StringBuilder();

    for (int i = 0; i < words.length; i++) {
      String word = words[i];

      if (i >= words.length - 2 && isKeyPattern(word)) {
        continue;
      }

      if (titleBuilder.length() > 0) {
        titleBuilder.append(" ");
      }
      titleBuilder.append(word);
    }

    return titleBuilder.toString().trim();
  }

  private boolean isKeyPattern(String text) {
    if (text.matches("[A-Ga-g][#b♯♭]?m?")) {
      return true;
    }

    if (text.matches("[A-Ga-g][#b♯♭]?m?(?:->|[-])[A-Ga-g][#b♯♭]?m?")) {
      return true;
    }

    return false;
  }

  private String extractKey(String songLine) {
    String[] words = songLine.split("\\s+");

    for (int i = words.length - 1; i >= 0; i--) {
      String word = words[i];

      if (isKeyPattern(word)) {
        return word;
      }
    }

    return null;
  }

  private String extractArtist(String songLine) {
    int slashIndex = songLine.indexOf('/');
    if (slashIndex >= 0 && slashIndex + 1 < songLine.length()) {
      String artistPart = songLine.substring(slashIndex + 1).trim();

      artistPart = artistPart.replaceAll("\\([^)]*\\)", "").trim();

      int bracketIndex = artistPart.indexOf('(');
      if (bracketIndex > 0) {
        artistPart = artistPart.substring(0, bracketIndex).trim();
      }

      int nextSlashIndex = artistPart.indexOf('/');
      if (nextSlashIndex > 0) {
        artistPart = artistPart.substring(0, nextSlashIndex).trim();
      }

      return artistPart;
    }
    return null;
  }

  private String extractYoutubeUrl(String songLine) {
    Matcher matcher = YOUTUBE_URL_PATTERN.matcher(songLine);
    if (matcher.find()) {
      String videoId = matcher.group(1);
      return "https://youtube.com/watch?v=" + matcher.group(1);
    }
    return null;
  }

  private String extractSpecialInstructions(String songLine, String title, String key, String artist, String youtubeUrl) {
    String line = songLine;

    if (title != null) {
      line = line.replaceAll(title, "");
    }

    if (key != null) {
      line = line.replaceAll(key, "");
    }

    if (artist != null) {
      line = line.replace("/ " + artist, "").replace("/" + artist, "");
    }

    if (youtubeUrl != null) {
      Matcher matcher = YOUTUBE_URL_PATTERN.matcher(songLine);
      if (matcher.find()) {
        line = line.replace(matcher.group(), "");
      }
    }

    StringBuilder instructions = new StringBuilder();
    Matcher bracketMatcher = Pattern.compile("\\(([^)]*)\\)|\\[([^\\]]*)\\]").matcher(line);

    while (bracketMatcher.find()) {
      String content =
          bracketMatcher.group(1) != null ? bracketMatcher.group(1) : bracketMatcher.group(2);
      if (content != null && !content.isEmpty()) {
        instructions.append(content.trim()).append("\n");
      }
    }

    line = line.replaceAll("\\([^)]*\\)", "").replaceAll("\\[[^\\]]*\\]", "");
    line = line.replaceAll("\\s+", " ").trim();

    if (!line.isEmpty()) {
      instructions.append(line);
    }

    String result = instructions.toString().trim();
    return result.isEmpty() ? null : result;
  }
}
