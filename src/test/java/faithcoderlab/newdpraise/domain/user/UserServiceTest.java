package faithcoderlab.newdpraise.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import faithcoderlab.newdpraise.domain.user.dto.SignupRequest;
import faithcoderlab.newdpraise.domain.user.dto.SignupResponse;
import faithcoderlab.newdpraise.global.exception.ResourceAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private UserService userService;

  @Test
  @DisplayName("회원가입 성공")
  void signupSuccess() {
    // given
    SignupRequest request = SignupRequest.builder()
        .email("suming@example.com")
        .password("Password123!")
        .name("수밍")
        .instrument("피아노")
        .build();

    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

    User savedUser = User.builder()
        .id(1L)
        .email(request.getEmail())
        .password("encodedPassword")
        .name(request.getName())
        .instrument(request.getInstrument())
        .role(Role.USER)
        .enabled(true)
        .build();

    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // when
    SignupResponse response = userService.signup(request);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getEmail()).isEqualTo(savedUser.getEmail());
    assertThat(response.getName()).isEqualTo(savedUser.getName());
    assertThat(response.getInstrument()).isEqualTo(savedUser.getInstrument());
    assertThat(response.getRole()).isEqualTo(Role.USER);

    verify(userRepository).existsByEmail(request.getEmail());
    verify(passwordEncoder).encode(request.getPassword());
    verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("회원가입 실패 - 중복 이메일")
  void signupFailWithDuplicateEmail() {
    // given
    SignupRequest request = SignupRequest.builder()
        .email("existing@example.com")
        .password("Password123!")
        .name("테스트유저")
        .instrument("기타")
        .build();

    when(userRepository.existsByEmail(anyString())).thenReturn(true);

    // when & then
    assertThatThrownBy(() -> userService.signup(request))
        .isInstanceOf(ResourceAlreadyExistsException.class)
        .hasMessage("이미 사용 중인 이메일입니다.");

    verify(userRepository).existsByEmail(request.getEmail());
    verify(userRepository, never()).save(any(User.class));
  }
}