package everyide.webide.config.auth.api;

import everyide.webide.config.auth.dto.request.PasswordChangeRequest;
import everyide.webide.config.auth.dto.request.SignRequestDto;
import everyide.webide.config.auth.dto.request.UserResponse;
import everyide.webide.user.UserService;
import everyide.webide.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> signUpUser(@RequestBody SignRequestDto signRequestDto) {
        userService.signUpUser(signRequestDto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/hello")
    public String hello() {
        return "hello";
    }

    @GetMapping("refresh")
    public String refresh() {
        return "API End-point for Refresh Token";
    }

    @GetMapping("user/info")
    public UserResponse getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName(); // JWT에서 사용자의 이메일 가져오기

        // 이메일을 사용하여 사용자 정보 조회
        User user = userService.findByEmail(email);

        // 필요한 정보만 UserResponse 객체에 담아 반환
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }

    @PatchMapping("user/info")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest passwordChangeRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean passwordChanged = userService.changePassword(passwordChangeRequest.getEmail(), passwordChangeRequest.getOldPassword(), passwordChangeRequest.getNewPassword());
            if (!passwordChanged) {
                // 비밀번호 변경 시도 실패 (예: 기존 비밀번호 불일치)
                User user = userService.getUserByEmail(passwordChangeRequest.getEmail());
                response.put("status", true); // 비밀번호 변경 실패 상태
                response.put("message", "Current password is incorrect.");
                // 사용자 정보 포함
                response.put("email", user.getEmail());
                response.put("name", user.getName());
                // 로그인 상태 유지를 위해 BadRequest 응답
                return ResponseEntity.badRequest().body(response);
            }
            // 비밀번호 변경 성공
            User user = userService.getUserByEmail(passwordChangeRequest.getEmail());
            response.put("status", false); // 비밀번호 변경 성공 상태
            response.put("message", "Password changed successfully.");
            // 사용자 정보 포함
            response.put("email", user.getEmail());
            response.put("name", user.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 예외 처리
            response.put("status", true); // 서버 에러 상태
            response.put("message", e.getMessage());
            // 서버 에러 상황에서도 클라이언트에 적절한 응답을 제공
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/api/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userService.findAllUsers();

        // 사용자 비밀번호를 제외한 정보를 UserResponse 리스트로 변환
        List<UserResponse> userResponses = users.stream()
                .map(user -> new UserResponse(user.getId(), user.getName(), user.getEmail()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(userResponses);
    }



}
