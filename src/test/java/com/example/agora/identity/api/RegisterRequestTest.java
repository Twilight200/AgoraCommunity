package com.example.agora.identity.api;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RegisterRequestTest {

    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp(){
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    void tearDown(){
        validatorFactory.close();
    }

    @Test
    void acceptsValidRegistrationInput() {
        var request = new RegisterRequest(
                "agora_user",
                "correct horse battery staple"
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsUsernameOutsideLengthRange() {
        var request = new RegisterRequest(
                "abc",
                "correct horse battery staple"
        );

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void rejectsUsernameWithUnsupportedCharacters() {
        var request = new RegisterRequest(
                "agora-user",
                "correct horse battery staple"
        );

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void rejectsPasswordShorterThanTwelveCharacters() {
        var request = new RegisterRequest("agora_user", "short");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void rejectsBlankFields() {
        var request = new RegisterRequest(
                "      ",
                "            "
        );

        assertThat(validator.validate(request)).isNotEmpty();
    }
}
