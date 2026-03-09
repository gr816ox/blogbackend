package top.zjwwiki.blogbackend.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
		UserExample example = new UserExample();
		example.createCriteria().andUsernameEqualTo(username);
		List<User> users = userMapper.selectByExample(example);
		return users.isEmpty() ? null : users.get(0);
	}

	public boolean existsByUsername(String username) {
		return findByUsername(username) != null;
	}

	public User register(String username, String rawPassword) {
		if (existsByUsername(username)) {
			throw new IllegalArgumentException("Username already exists: " + username);
		}

		User user = new User();
		user.setUsername(username);
		user.setPassword(passwordEncoder.encode(rawPassword));
		user.setRole("USER");
		user.setEnabled((byte) 1);
		userMapper.insertSelective(user);
		return findByUsername(username);
	}
}
