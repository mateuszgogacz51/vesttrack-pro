package com.vesttrack.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

/**
 * Wysylka e-maili transakcyjnych: powitanie po rejestracji oraz link do resetu hasla.
 *
 * Uzywa Spring Mail (JavaMailSender), skonfigurowanego w application.yml przez
 * wlasciwosci spring.mail.* (patrz README.md, sekcja "Konfiguracja e-mail" -
 * tam dokladnie opisano co wpisac dla Gmaila i dla Mailtrap.io do testow).
 *
 * Celowo NIE rzuca wyjatku dalej, gdy wysylka sie nie powiedzie (np. zle dane SMTP) -
 * brak dzialajacej poczty nie powinien blokowac np. rejestracji uzytkownika.
 * Blad jest tylko logowany.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendWelcomeEmail(String toEmail, String firstName) {
        String subject = "Witaj w VestTrack Pro!";
        String body = """
                Cześć %s,

                Twoje konto w VestTrack Pro zostało utworzone pomyślnie.

                Możesz teraz zalogować się i dodać swój pierwszy rachunek inwestycyjny:
                %s/login

                Pozdrawiamy,
                Zespół VestTrack Pro
                """.formatted(firstName, frontendUrl);

        send(toEmail, subject, body);
    }

    public void sendPasswordResetEmail(String toEmail, String rawToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
        String subject = "Reset hasła — VestTrack Pro";
        String body = """
                Otrzymaliśmy prośbę o zresetowanie hasła do Twojego konta VestTrack Pro.

                Kliknij poniższy link, aby ustawić nowe hasło (link ważny 30 minut):
                %s

                Jeśli to nie Ty prosiłeś o reset hasła, zignoruj tę wiadomość —
                Twoje obecne hasło pozostanie bez zmian.

                Pozdrawiamy,
                Zespół VestTrack Pro
                """.formatted(resetLink);

        send(toEmail, subject, body);
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Wyslano e-mail '{}' do {}", subject, to);
        } catch (MailException ex) {
            log.error("Nie udalo sie wyslac e-maila '{}' do {}. Przyczyna: {}", subject, to, ex.getMessage());
        }
    }
}
