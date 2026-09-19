package com.minutemart.quickcommerce;

import java.util.UUID;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
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
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.reflection().registerType(UUID[].class, MemberCategory.values());
			hints.reflection().registerType(String[].class, MemberCategory.values());
			hints.reflection().registerType(Long[].class, MemberCategory.values());
		}
	}
}
