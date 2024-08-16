package com.ninestar.datapie.datamagic.config;

import com.ninestar.datapie.datamagic.security.*;
import com.ninestar.datapie.datamagic.security.AuthServiceProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import javax.annotation.Resource;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {

	@Value("${spring.security.white.list}")
	private String whiteList;

	@Value("${spring.security.login.page}")
	private String loginPage;

	@Resource
	private RestfulAccessDeniedHandler userAuthDeniedHandler;

	@Resource
	private AuthServiceProvider authServiceProvider;

	/**
	 * Encryption algorithm
	 */
	@Bean
	public PasswordEncoder pswEncoder(){
		return new BCryptPasswordEncoder();
	}


	/**
	 * Custom auth method when login
	 */
	@Override
	protected void configure(AuthenticationManagerBuilder auth){
		// Register custom auth logic
		auth.authenticationProvider(authServiceProvider);
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
		//allow auth and Swagger URL to be accessed without authentication
		web.ignoring().antMatchers(whiteList.split(","));
	}

	/**
	 * Spring security config
	 */
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests()
				// all other requests need to be authenticated
				.anyRequest().authenticated()
				// form login and all related interfaces have permit
				.and().formLogin().loginPage(loginPage).permitAll()
				.and().httpBasic()
				.and().rememberMe().rememberMeParameter("remeber")
				// exception handler
				.and().exceptionHandling().accessDeniedHandler(userAuthDeniedHandler)
				// enable cors
				.and().cors()
				// disable csrf
				.and().csrf().disable().sessionManagement().disable()
				;

		// no cache
		http.headers().cacheControl();

		// filter, used to extract username/password from http request
		http.addFilter(new UsernamePasswordAuthFilter(authenticationManager()));

		//filter, used to verify username/password
		http.addFilter(new JwtAuthenticationTokenFilter(authenticationManager()));
	}
}

