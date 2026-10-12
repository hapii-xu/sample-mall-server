package com.samplemall.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 固定测试账号（与 PRD「测试账号」一节一致）；内存库每次启动重建。 */
@Component
class UserSeeder implements ApplicationRunner {

    private final UserRepository users;

    UserSeeder(UserRepository users) {
        this.users = users;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        seed("demo", "tapestry2026", "茶客_demo", "13800000001");
        seed("demo2", "tapestry2026", "茶客_demo2", "13800000002");
    }

    private void seed(String username, String password, String displayName, String phone) {
        String salt = username + "-salt";
        users.save(new User(username, PasswordHasher.hash(salt, password), salt, displayName, phone));
    }
}
