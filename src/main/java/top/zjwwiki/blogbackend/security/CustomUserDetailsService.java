package top.zjwwiki.blogbackend.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import top.zjwwiki.blogbackend.generated.User;
import top.zjwwiki.blogbackend.service.UserService;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserService userService;

	public CustomUserDetailsService(UserService userService) {
		this.userService = userService;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User dbUser = userService.findByUsername(username);
		if (dbUser == null) {
			throw new UsernameNotFoundException("User not found: " + username);
		}

		if (dbUser.getEnabled() != null && dbUser.getEnabled() == 0) {
			throw new UsernameNotFoundException("User is disabled: " + username);
		}

		String dbRole = dbUser.getRole();
		String role = (dbRole == null || dbRole.isBlank()) ? "USER" : dbRole;
		if (role.startsWith("ROLE_")) {
			role = role.substring("ROLE_".length());
		}

		return org.springframework.security.core.userdetails.User.withUsername(dbUser.getUsername())
				.password(dbUser.getPassword())
				.roles(role)
				.disabled(dbUser.getEnabled() != null && dbUser.getEnabled() == 0)
				.build();
	}

	public void register(String username, String rawPassword) {
		userService.register(username, rawPassword);
	}
}
