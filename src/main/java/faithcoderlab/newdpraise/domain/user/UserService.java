package faithcoderlab.newdpraise.domain.user;

import faithcoderlab.newdpraise.domain.user.dto.SignUpRequest;
import faithcoderlab.newdpraise.domain.user.dto.SignUpResponse;
import faithcoderlab.newdpraise.domain.user.dto.UpdateProfileRequest;
import faithcoderlab.newdpraise.domain.user.dto.UserProfileResponse;
import faithcoderlab.newdpraise.global.exception.ResourceAlreadyExistsException;
import faithcoderlab.newdpraise.global.exception.ResourceNotFoundException;
import faithcoderlab.newdpraise.global.service.FileService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final FileService fileService;

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

  @Transactional(readOnly = true)
  public UserProfileResponse getUserProfile(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

    return UserProfileResponse.fromUser(user);
  }

  @Transactional(readOnly = true)
  public UserProfileResponse getCurrentUserProfile(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를  찾을 수 없습니다. Email: " + email));

    return UserProfileResponse.fromUser(user);
  }

  @Transactional
  public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. Email: " + email));

    user.setName(request.getName());
    user.setInstrument(request.getInstrument());
    user.setUpdatedAt(LocalDateTime.now());

    User updatedUser = userRepository.save(user);
    return UserProfileResponse.fromUser(updatedUser);
  }

  @Transactional
  public UserProfileResponse updateProfileImage(String email, MultipartFile file) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. Email: " + email));

    String imagePath = fileService.saveFile(file, "profile-images");
    user.setProfileImage(imagePath);

    User updatedUser = userRepository.save(user);
    return UserProfileResponse.fromUser(updatedUser);
  }
}
