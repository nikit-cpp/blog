package com.github.nkonev.controllers;

import com.github.nkonev.Constants;
import com.github.nkonev.converter.UserAccountConverter;
import com.github.nkonev.dto.EditUserDTO;
import com.github.nkonev.entity.jpa.UserAccount;
import com.github.nkonev.entity.redis.UserConfirmationToken;
import com.github.nkonev.exception.UserAlreadyPresentException;
import com.github.nkonev.repo.jpa.UserAccountRepository;
import com.github.nkonev.repo.redis.UserConfirmationTokenRepository;
import com.github.nkonev.services.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Controller
@Transactional
public class RegistrationController {
    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserConfirmationTokenRepository userConfirmationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Value("${custom.confirmation.registration.token.ttl-minutes}")
    private long userConfirmationTokenTtlMinutes;

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationController.class);

    private UserConfirmationToken createUserConfirmationToken(UserAccount userAccount) {
        Assert.isTrue(!userAccount.isEnabled(), "user account mustn't be enabled");

        Duration ttl = Duration.ofMinutes(userConfirmationTokenTtlMinutes);
        long seconds = ttl.getSeconds(); // Redis requires seconds

        UUID tokenUuid = UUID.randomUUID();
        UserConfirmationToken userConfirmationToken = new UserConfirmationToken(tokenUuid.toString(), userAccount.getId(), seconds);
        return userConfirmationTokenRepository.save(userConfirmationToken);
    }

    @PostMapping(value = Constants.Uls.API+Constants.Uls.REGISTER)
    @ResponseBody
    public void register(@RequestBody @Valid EditUserDTO userAccountDTO) {
        if(userAccountRepository.findByUsername(userAccountDTO.getLogin()).isPresent()){
            throw new UserAlreadyPresentException("User with login '" + userAccountDTO.getLogin() + "' is already present");
        }
        if(userAccountRepository.findByEmail(userAccountDTO.getEmail()).isPresent()){
            LOGGER.warn("Skipping sent registration email {} because this user already present", userAccountDTO.getEmail());
            return; // we care for user email leak
        }

        UserAccount userAccount = UserAccountConverter.buildUserAccountEntityForInsert(userAccountDTO, passwordEncoder);

        userAccount = userAccountRepository.save(userAccount);
        UserConfirmationToken userConfirmationToken = createUserConfirmationToken(userAccount);
        emailService.sendUserConfirmationToken(userAccount.getEmail(), userConfirmationToken, userAccount.getUsername());
    }

    /**
     * Handles confirmations.
     * In frontend router also should implement follows pages
     * /confirm -- success confirmation
     * /confirm/registration/token-not-found
     * /confirm/registration/user-not-found
     * @param uuid
     * @return
     */
    @GetMapping(value = Constants.Uls.CONFIRM)
    public String confirm(@RequestParam(Constants.Uls.UUID) UUID uuid) {
        String stringUuid = uuid.toString();
        Optional<UserConfirmationToken> userConfirmationTokenOptional = userConfirmationTokenRepository.findById(stringUuid);
        if (!userConfirmationTokenOptional.isPresent()) {
            return "redirect:/confirm/registration/token-not-found";
        }
        UserConfirmationToken userConfirmationToken = userConfirmationTokenOptional.get();
        Optional<UserAccount> userAccountOptional = userAccountRepository.findById(userConfirmationToken.getUserId());
        if (!userAccountOptional.isPresent()) {
            return "redirect:/confirm/registration/user-not-found";
        }
        UserAccount userAccount = userAccountOptional.get();
        if (userAccount.isEnabled()) {
            LOGGER.warn("Somebody attempts secondary confirm already confirmed user account with email='{}'", userAccount);
            return Constants.Uls.ROOT;  // respond static
        }

        userAccount.setEnabled(true);

        userConfirmationTokenRepository.deleteById(stringUuid);

        return Constants.Uls.ROOT; // respond static
    }

    @PostMapping(value = Constants.Uls.API+Constants.Uls.RESEND_CONFIRMATION_EMAIL)
    @ResponseBody
    public void resendConfirmationToken(@RequestParam(value = "email") String email) {
        Optional<UserAccount> userAccountOptional = userAccountRepository.findByEmail(email);
        if(!userAccountOptional.isPresent()){
            LOGGER.warn("Skipping sent subsequent confirmation email {} because this email is not found", email);
            return; // we care for for email leak
        }
        UserAccount userAccount = userAccountOptional.get();
        if (userAccount.isEnabled()) {
            // this account already confirmed
            LOGGER.warn("Skipping sent subsequent confirmation email {} because this user account already enabled", email);
            return; // we care for for email leak
        }

        UserConfirmationToken userConfirmationToken = createUserConfirmationToken(userAccount);
        emailService.sendUserConfirmationToken(email, userConfirmationToken, userAccount.getUsername());
    }

}
