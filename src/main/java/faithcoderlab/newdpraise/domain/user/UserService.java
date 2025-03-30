package faithcoderlab.newdpraise.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public SignupResponse signup(SignupRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new ResourceAlreadyExistsException("이미 사용 중인 이메일입니다.");
    }

    User user = User.builder()
        .email(request.getEmail())
        .password(passwordEncoder.encode(request.getPassword()))
        .name(request.getName())
        .instrument(request.getInstrument())
        .profileImage(request.getProfileImage())
        .role(Role.USER)
        .enabled(true)
        .build();

    User savedUser = userRepository.save(user);

    return SignupResponse.fromUser(savedUser);
  }
}
