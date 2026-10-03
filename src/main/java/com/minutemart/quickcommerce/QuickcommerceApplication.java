package com.minutemart.quickcommerce;

import java.util.List;
import java.util.UUID;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.modulith.Modulithic;

@SpringBootApplication
@Modulithic
@RegisterReflectionForBinding({ UUID[].class, String[].class, Long[].class })
@ImportRuntimeHints(QuickcommerceApplication.NativeHints.class)
public class QuickcommerceApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuickcommerceApplication.class, args);
	}

	static class NativeHints implements RuntimeHintsRegistrar {

		// Flyway resolves these via Class#getMethod("isFlywaySpecificVersionOf") and
		// Class#getDeclaredConstructor(SQLException, DataSource) when a connection fails.
		private static final List<String> FLYWAY_SQL_EXCEPTION_TYPES = List.of(
				"org.flywaydb.core.internal.exception.sqlExceptions.FlywaySqlServerUntrustedCertificateSqlException",
				"org.flywaydb.core.internal.exception.sqlExceptions.FlywaySqlNoIntegratedAuthException",
				"org.flywaydb.core.internal.exception.sqlExceptions.FlywaySqlNoDriversForInteractiveAuthException");

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.reflection().registerType(UUID[].class, MemberCategory.values());
			hints.reflection().registerType(String[].class, MemberCategory.values());
			hints.reflection().registerType(Long[].class, MemberCategory.values());
			for (String type : FLYWAY_SQL_EXCEPTION_TYPES) {
				hints.reflection().registerType(TypeReference.of(type),
						MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
			}
		}
	}
}
