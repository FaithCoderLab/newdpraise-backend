package faithcoderlab.newdpraise.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import faithcoderlab.newdpraise.domain.user.dto.SignUpRequest;
import faithcoderlab.newdpraise.domain.user.dto.SignUpResponse;
import faithcoderlab.newdpraise.domain.user.dto.UpdateProfileRequest;
import faithcoderlab.newdpraise.domain.user.dto.UserProfileResponse;
import faithcoderlab.newdpraise.global.exception.ResourceAlreadyExistsException;
import faithcoderlab.newdpraise.global.exception.ResourceNotFoundException;
import faithcoderlab.newdpraise.global.service.FileService;
import java.time.LocalDateTime;
import java.util.Optional;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private FileService fileService;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private UserService userService;

  private Faker faker;

  @BeforeEach
  void setUp() {
    faker = new Faker();
  }

  @Test
  @DisplayName("회원가입 성공")
  void signupSuccess() {
    // given
    String email = faker.internet().emailAddress();
    String password = faker.internet().password(8, 16, true, true);
    String name = faker.name().fullName();
    String instrument = faker.music().instrument();

    SignUpRequest request = SignUpRequest.builder()
        .email(email)
        .password(password)
        .name(name)
        .instrument(instrument)
        .build();

    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

    User savedUser = createTestUser(1L, email, name, instrument);
    savedUser.setPassword("encodedPassword");

    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // when
    SignUpResponse response = userService.signup(request);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getEmail()).isEqualTo(email);
    assertThat(response.getName()).isEqualTo(name);
    assertThat(response.getInstrument()).isEqualTo(instrument);
    assertThat(response.getRole()).isEqualTo(Role.USER);

    verify(userRepository).existsByEmail(request.getEmail());
    verify(passwordEncoder).encode(request.getPassword());
    verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("회원가입 실패 - 중복 이메일")
  void signupFailWithDuplicateEmail() {
    // given
    String duplicateEmail = faker.internet().emailAddress();
    SignUpRequest request = SignUpRequest.builder()
        .email(duplicateEmail)
        .password(faker.internet().password(8, 16, true, true))
        .name(faker.name().fullName())
        .instrument(faker.music().instrument())
        .build();

    when(userRepository.existsByEmail(duplicateEmail)).thenReturn(true);

    // when & then
    assertThatThrownBy(() -> userService.signup(request))
        .isInstanceOf(ResourceAlreadyExistsException.class)
        .hasMessage("이미 사용 중인 이메일입니다.");

    verify(userRepository).existsByEmail(duplicateEmail);
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("사용자 프로필 조회 - 성공")
  void getUserProfileSuccess() {
    // given
    Long userId = faker.number().randomNumber();
    String email = faker.internet().emailAddress();
    String name = faker.name().fullName();
    String instrument = faker.music().instrument();

    User user = createTestUser(userId, email, name, instrument);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // when
    UserProfileResponse response = userService.getUserProfile(userId);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(userId);
    assertThat(response.getEmail()).isEqualTo(email);
    assertThat(response.getName()).isEqualTo(name);
    assertThat(response.getInstrument()).isEqualTo(instrument);
  }

  @Test
  @DisplayName("사용자 프로필 조회 - 실패 (사용자 없음)")
  void getUserProfileNotFound() {
    // given
    Long userId = faker.number().randomNumber();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.getUserProfile(userId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("사용자를 찾을 수 없습니다.");
  }

  @Test
  @DisplayName("현재 사용자 프로필 조회 - 성공")
  void getCurrentUserProfileSuccess() {
    // given
    String email = faker.internet().emailAddress();
    String name = faker.name().fullName();
    String instrument = faker.music().instrument();

    User user = createTestUser(1L, email, name, instrument);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

    // when
    UserProfileResponse response = userService.getCurrentUserProfile(email);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getEmail()).isEqualTo(email);
    assertThat(response.getName()).isEqualTo(name);
  }

  @Test
  @DisplayName("프로필 업데이트 - 성공")
  void updateProfileSuccess() {
    // given
    String email = faker.internet().emailAddress();
    String originalName = faker.name().fullName();
    String originalInstrument = faker.music().instrument();

    User user = createTestUser(1L, email, originalName, originalInstrument);

    String newName = faker.name().fullName();
    String newInstrument = faker.music().instrument();

    UpdateProfileRequest request = new UpdateProfileRequest();
    request.setName(newName);
    request.setInstrument(newInstrument);

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenReturn(user);

    // when
    UserProfileResponse response = userService.updateProfile(email, request);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getName()).isEqualTo(newName);
    assertThat(response.getInstrument()).isEqualTo(newInstrument);
    verify(userRepository).save(user);
  }

  @Test
  @DisplayName("프로필 이미지 업데이트 - 성공")
  void updateProfileImageSuccess() {
    // given
    String email = faker.internet().emailAddress();
    String name = faker.name().fullName();
    String instrument = faker.music().instrument();

    User user = createTestUser(1L, email, name, instrument);

    String filename = "test_" + faker.internet().uuid() + ".jpg";
    byte[] content = faker.lorem().paragraph().getBytes();

    MultipartFile file = new MockMultipartFile(
        "file", filename, "image/jpeg", content);
    String imagePath = "profile-images/" + faker.internet().uuid() + ".jpg";

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(fileService.saveFile(any(MultipartFile.class), anyString())).thenReturn(imagePath);
    when(userRepository.save(any(User.class))).thenReturn(user);

    // when
    UserProfileResponse response = userService.updateProfileImage(email, file);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getProfileImage()).isEqualTo(imagePath);
    verify(fileService).saveFile(file, "profile-images");
    verify(userRepository).save(user);
  }

  private User createTestUser(Long id, String email, String name, String instrument) {
    return User.builder()
        .id(id)
        .email(email)
        .name(name)
        .password(faker.internet().password(8, 16, true, true))
        .instrument(instrument)
        .profileImage(faker.internet().uuid() + ".jpg")
        .role(Role.USER)
        .isActive(true)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }
}
