package com.samplemall.user;

import com.samplemall.common.ApiResponse;
import com.samplemall.common.BizException;
import com.samplemall.common.JwtSupport;
import com.samplemall.common.UserHeaders;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    /** 登录请求（REQ-030）：空字段的校验文案与前端行内提示一致。 */
    public record LoginRequest(
            @NotBlank(message = "请输入用户名") String username,
            @NotBlank(message = "请输入密码") String password) {
    }

    public record UserView(Long id, String username, String displayName, String phone) {
        static UserView of(User u) {
            return new UserView(u.getId(), u.getUsername(), u.getDisplayName(), u.getPhone());
        }
    }

    public record LoginResult(String token, long expiresIn, UserView user) {
    }

    private final UserRepository users;
    private final JwtSupport jwt;
    private final long ttlSeconds;

    public UserController(UserRepository users, JwtSupport jwt, @Value("${mall.jwt.ttl-seconds}") long ttlSeconds) {
        this.users = users;
        this.jwt = jwt;
        this.ttlSeconds = ttlSeconds;
    }

    @PostMapping("/api/auth/login")
    public ApiResponse<LoginResult> login(@Valid @RequestBody LoginRequest req) {
        User user = users.findByUsername(req.username().trim())
                .filter(u -> PasswordHasher.matches(u.getSalt(), req.password(), u.getPasswordHash()))
                // 账号不存在与口令错误返回同一句提示，避免枚举账号
                .orElseThrow(() -> BizException.unauthorized("BAD_CREDENTIALS", "用户名或密码错误"));
        String token = jwt.issue(user.getId(), user.getUsername(), ttlSeconds);
        return ApiResponse.ok(new LoginResult(token, ttlSeconds, UserView.of(user)));
    }

    @GetMapping("/api/users/me")
    public ApiResponse<UserView> me(@RequestHeader(UserHeaders.USER_ID) long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> BizException.unauthorized("UNAUTHORIZED", "请先登录"));
        return ApiResponse.ok(UserView.of(user));
    }
}
