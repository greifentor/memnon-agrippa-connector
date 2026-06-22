package de.ollie.memnon.agrippa.connector;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
class AgrippaJdbcConfiguration {

	@Value("${agrippa.jdbc.datasource.password}")
	private String password;

	@Value("${agrippa.jdbc.datasource.url}")
	private String url;

	@Value("${agrippa.jdbc.datasource.username}")
	private String username;
}
