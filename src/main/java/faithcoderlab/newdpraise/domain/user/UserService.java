package faithcoderlab.newdpraise.domain.user;

import faithcoderlab.newdpraise.domain.user.dto.SignUpRequest;
import faithcoderlab.newdpraise.domain.user.dto.SignUpResponse;
import faithcoderlab.newdpraise.global.exception.ResourceAlreadyExistsException;
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
  public SignUpResponse signup(SignUpRequest request) {
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

    return SignUpResponse.fromUser(savedUser);
  }
}
