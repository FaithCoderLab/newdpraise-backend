package faithcoderlab.newdpraise.domain.conti;

import static org.assertj.core.api.Assertions.assertThat;

import faithcoderlab.newdpraise.domain.song.Song;
import faithcoderlab.newdpraise.domain.song.SongRepository;
import faithcoderlab.newdpraise.domain.user.Role;
import faithcoderlab.newdpraise.domain.user.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContiParserServiceTest {

  @Mock
  private SongRepository songRepository;

  @InjectMocks
  private ContiParserService contiParserService;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .id(1L)
        .email("suming@example.com")
        .name("수밍")
        .role(Role.USER)
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Test
  @DisplayName("기본 콘티 텍스트 파싱 테스트")
  void parseBasicContiText() {
    // given
    String contiText = "20250405 찬양집회 콘티\n\n" +
        "1. 물댄 동산 G (교제송) \n\n" +
        "2. 정직한 예배 G-Ab / 제이어스\n" +
        "https://youtu.be/R9tUikvBv5M?si=_Njl-T7VYU7c1hvb\n\n" +
        "3. 아름다우신 Ab / 캠퍼스워십\n\n" +
        "주제 : Alive";

    // when
    Conti result = contiParserService.parseContiText(contiText, testUser);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo("Alive");
    assertThat(result.getScheduledAt()).isEqualTo(LocalDate.of(2025, 4, 5));
    assertThat(result.getCreator()).isEqualTo(testUser);
    assertThat(result.getSongs()).hasSize(3);

    Song firstSong = result.getSongs().get(0);
    assertThat(firstSong.getTitle()).isEqualTo("물댄 동산");
    assertThat(firstSong.getPerformanceKey()).isEqualTo("G");
    assertThat(firstSong.getSpecialInstructions()).contains("교제송");
    assertThat(firstSong.getYoutubeUrl()).isNull();

    Song secondSong = result.getSongs().get(1);
    assertThat(secondSong.getTitle()).isEqualTo("정직한 예배");
    assertThat(secondSong.getPerformanceKey()).isEqualTo("G-Ab");
    assertThat(secondSong.getArtist()).isEqualTo("제이어스");
    assertThat(secondSong.getYoutubeUrl()).isEqualTo("https://youtube.com/watch?v=R9tUikvBv5M");

    Song thirdSong = result.getSongs().get(2);
    assertThat(thirdSong.getTitle()).isEqualTo("아름다우신");
    assertThat(thirdSong.getPerformanceKey()).isEqualTo("Ab");
    assertThat(thirdSong.getArtist()).isEqualTo("캠퍼스워십");
  }

  @Test
  @DisplayName("다양한 날짜 형식 파싱 테스트")
  void parseDifferentDateFormats() {
    // given
    LocalDate expectedDate = LocalDate.of(2025, 4, 5);

    String yyyymmdd = "20250405 콘티\n1. 테스트 곡 E";
    Conti result1 = contiParserService.parseContiText(yyyymmdd, testUser);
    assertThat(result1.getScheduledAt())
        .as("Failed for YYYYMMDD format")
        .isEqualTo(expectedDate);

    String yyyyDashMmDashDd = "2025-04-05 콘티\n1. 테스트 곡 E";
    Conti result2 = contiParserService.parseContiText(yyyyDashMmDashDd, testUser);
    assertThat(result2.getScheduledAt())
        .as("Failed for YYYY-MM-DD format")
        .isEqualTo(expectedDate);

    String yyyySlashMmSlashDd = "2025/04/05 콘티\n1. 테스트 곡 E";
    Conti result3 = contiParserService.parseContiText(yyyySlashMmSlashDd, testUser);
    assertThat(result3.getScheduledAt())
        .as("Failed for YYYY/MM/DD format")
        .isEqualTo(expectedDate);

    String koreanFormat = "2025년 4월 5일 콘티\n1. 테스트 곡 E";
    Conti result4 = contiParserService.parseContiText(koreanFormat, testUser);
    assertThat(result4.getScheduledAt())
        .as("Failed for Korean date format")
        .isEqualTo(expectedDate);

    String mmSlashDd = "04/05 콘티\n1. 테스트 곡 E";
    Conti result5 = contiParserService.parseContiText(mmSlashDd, testUser);
    assertThat(result5.getScheduledAt())
        .as("Failed for MM/DD format")
        .isEqualTo(LocalDate.of(LocalDate.now().getYear(), 4, 5));
  }

  @Test
  @DisplayName("유튜브 URL 다양한 형식 파싱 테스트")
  void parseYoutubeUrls() {
    // given
    String contiText = "테스트 콘티\n\n" +
        "1. 곡1 C\n" +
        "https://youtube.com/watch?v=abcdef12345\n\n" +
        "2. 곡2 D\n" +
        "https://youtu.be/ghijk67890a\n\n" +
        "3. 곡3 E\n" +
        "유튜브: https://youtube.com/watch?v=lmnopq12345&t=120\n\n";

    // when
    Conti result = contiParserService.parseContiText(contiText, testUser);

    // then
    assertThat(result.getSongs()).hasSize(3);
    assertThat(result.getSongs().get(0).getYoutubeUrl()).isEqualTo("https://youtube.com/watch?v=abcdef12345");
    assertThat(result.getSongs().get(1).getYoutubeUrl()).isEqualTo("https://youtube.com/watch?v=ghijk67890a");
    assertThat(result.getSongs().get(2).getYoutubeUrl()).isEqualTo("https://youtube.com/watch?v=lmnopq12345");
  }

  @Test
  @DisplayName("다양한 URL 타입 파싱 테스트")
  void parseNonYoutubeUrls() {
    // given
    String contiText = "20250111 찬양집회\n\n" +
        "1. 주님의 시간에 C->D (76)\n" +
        "일단 레퍼런스대로\n" +
        "http://cafe.naver.com/naehansarang79/140\n\n" +
        "2. 나의 왕 나의 주 A / J-US (이사랑 인도)\n" +
        "레퍼런스대로\n" +
        "3. 주의 자녀로 산다는 것은 G\n" +
        "4. 천성을 향해 A / KCD\n" +
        "5. 온땅은 주의 것 G / 캠퍼스워십\n" +
        "주제 : Children of God";

    // when
    Conti result = contiParserService.parseContiText(contiText, testUser);

    // then
    assertThat(result.getSongs()).hasSize(5);

    Song song1 = result.getSongs().get(0);
    assertThat(song1.getTitle()).isEqualTo("주님의 시간에");
    assertThat(song1.getYoutubeUrl()).isNull();
    assertThat(song1.getReferenceUrl()).isEqualTo("http://cafe.naver.com/naehansarang79/140");
    assertThat(song1.getUrlType()).isEqualTo("other");

    Song song2 = result.getSongs().get(1);
    assertThat(song2.getTitle()).isEqualTo("나의 왕 나의 주");
    assertThat(song2.getYoutubeUrl()).isNull();

    // 3번 곡 - 멜론 링크
    Song song3 = result.getSongs().get(2);
    assertThat(song3.getTitle()).isEqualTo("주의 자녀로 산다는 것은");
    assertThat(song3.getYoutubeUrl()).isNull();

    // 4번 곡 - 사운드클라우드 링크
    Song song4 = result.getSongs().get(3);
    assertThat(song4.getTitle()).isEqualTo("천성을 향해");
    assertThat(song4.getYoutubeUrl()).isNull();

    // 5번 곡 - 다음 뮤직 링크 (기타 URL 분류)
    Song song5 = result.getSongs().get(4);
    assertThat(song5.getTitle()).isEqualTo("온땅은 주의 것");
    assertThat(song5.getYoutubeUrl()).isNull();
  }

  @Test
  @DisplayName("복잡한 콘티 예제 파싱 테스트")
  void parseComplexContiExample() {
    // given
    String contiText = "20250111 찬양집회\n\n" +
        "1. 주님의 시간에 C->D (76)\n" +
        "일단 레퍼런스대로\n" +
        "http://cafe.naver.com/naehansarang79/140\n\n" +
        "2. 나의 왕 나의 주 A / J-US (이사랑 인도)\n" +
        "레퍼런스대로\n" +
        "https://youtu.be/7U6n-xgn8U8?si=VXD2SEgk7QtDCoE7\n\n" +
        "3. winning all A / 캠퍼스워십\n" +
        "템포는 2번곡과 같이\n" +
        "https://youtu.be/_wc0uJTmtSQ?si=sraRjN0R4XWdSTsH 39초부터\n\n" +
        "주제 : Children of God\n\n" +
        "악보는 어디까지나 참고…\n" +
        "수정이 필요하면 수정햐주세요";

    // when
    Conti result = contiParserService.parseContiText(contiText, testUser);

    // then
    assertThat(result.getTitle()).isEqualTo("Children of God");
    assertThat(result.getScheduledAt()).isEqualTo(LocalDate.of(2025, 1, 11));
    assertThat(result.getSongs()).hasSize(3);

    Song firstSong = result.getSongs().get(0);
    assertThat(firstSong.getTitle()).isEqualTo("주님의 시간에");
    assertThat(firstSong.getOriginalKey()).isEqualTo("C->D");
    assertThat(firstSong.getSpecialInstructions()).contains("76");
    assertThat(firstSong.getSpecialInstructions()).contains("일단 레퍼런스대로");

    Song secondSong = result.getSongs().get(1);
    assertThat(secondSong.getTitle()).isEqualTo("나의 왕 나의 주");
    assertThat(secondSong.getOriginalKey()).isEqualTo("A");
    assertThat(secondSong.getArtist()).isEqualTo("J-US");
    assertThat(secondSong.getSpecialInstructions()).contains("이사랑 인도");
    assertThat(secondSong.getSpecialInstructions()).contains("레퍼런스대로");
    assertThat(secondSong.getYoutubeUrl()).isEqualTo("https://youtube.com/watch?v=7U6n-xgn8U8");

    Song thirdSong = result.getSongs().get(2);
    assertThat(thirdSong.getTitle()).isEqualTo("winning all");
    assertThat(thirdSong.getOriginalKey()).isEqualTo("A");
    assertThat(thirdSong.getArtist()).isEqualTo("캠퍼스워십");
    assertThat(thirdSong.getSpecialInstructions()).contains("템포는 2번곡과 같이");
    assertThat(thirdSong.getYoutubeUrl()).isEqualTo("https://youtube.com/watch?v=_wc0uJTmtSQ");
  }

  @Test
  @DisplayName("creator가 null인 경우 테스트")
  void parseContiTextWithNullCreator() {
    // given
    String contiText = "20250405 찬양집회 콘티\n" +
        "1. 물댄 동산 G (교제송)";

    // when
    Conti result = contiParserService.parseContiText(contiText, null);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getCreator()).isNull();
    assertThat(result.getTitle()).isEqualTo("20250405 찬양집회 콘티");
    assertThat(result.getScheduledAt()).isEqualTo(LocalDate.of(2025, 4, 5));
  }

  @Test
  @DisplayName("주제가 없는 경우 첫 줄에서 제목 추출 테스트")
  void parseTitleFromFirstLineWhenNoTheme() {
    // given
    String contiText = "2025년 2월 15일 찬양집회 콘티\n\n" +
        "1. 나 주님이 더욱 필요해 D-E\n" +
        "세션은 따로 카피 안할게요.\n" +
        "연습때 잠깐 맞춰봅시다.";

    // when
    Conti result = contiParserService.parseContiText(contiText, testUser);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo("2025년 2월 15일 찬양집회 콘티");
    assertThat(result.getScheduledAt()).isEqualTo(LocalDate.of(2025, 2, 15));
  }
}
