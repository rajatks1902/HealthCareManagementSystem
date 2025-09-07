package com.rajat.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"jwt.secret=pZfhYe9EJ339HWJ6bHORpAhKyZ9n3FdnxJsH7bPXSNo=",
		"jwt.expiration=86400000"
})
class AuthSecurityApplicationTests {

	@Test
	void contextLoads() {
	}

//	DMD 1
//CREATE TABLE users (
//			id TEXT PRIMARY KEY,
//			username TEXT,
//			email TEXT,
//			password TEXT,
//			roles SET<TEXT>
//	);

//	DMD2
//	CREATE TABLE roles (
//    id TEXT PRIMARY KEY,
//    name TEXT
//);


}
