package top.zjwwiki.blogbackend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import top.zjwwiki.blogbackend.generated.User;
import top.zjwwiki.blogbackend.generated.UserExample;
import top.zjwwiki.mapper.UserMapper;

@Service
public class UserService {

	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
	}

	public User findByUsername(String username) {
		if (!StringUtils.hasText(username)) {
			return null;
		}

		UserExample example = new UserExample();
		example.createCriteria().andUsernameEqualTo(username.trim());
		List<User> users = userMapper.selectByExample(example);
		return users.isEmpty() ? null : users.get(0);
	}

	public boolean existsByUsername(String username) {
		return findByUsername(username) != null;
	}

	public User register(String username, String rawPassword) {
		String normalizedUsername = username == null ? null : username.trim();
		if (!StringUtils.hasText(normalizedUsername)) {
			throw new IllegalArgumentException("Username must not be blank");
		}

		if (existsByUsername(normalizedUsername)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
		}

		User user = new User();
		user.setUsername(normalizedUsername);
		user.setPassword(passwordEncoder.encode(rawPassword));
		user.setRole("USER");
		user.setEnabled((byte) 1);
		userMapper.insertSelective(user);
		return findByUsername(normalizedUsername);
	}
}
