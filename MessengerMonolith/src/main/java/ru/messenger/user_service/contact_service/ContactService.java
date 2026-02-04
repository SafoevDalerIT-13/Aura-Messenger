package ru.messenger.user_service.contact_service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.messenger.user_service.domain.entity.UserEntity;
import ru.messenger.user_service.domain.repository.UserRepository;
import ru.messenger.user_service.domain.service.exception.UserNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final ContactMapper contactMapper;

    // Получить все контакты пользователя
    public List<ContactResponseDto> getUserContacts(Long userId) {
        List<ContactEntity> contacts = contactRepository.findByOwnerId(userId);
        log.debug("Found {} contacts for user ID: {}", contacts.size(), userId);
        return contacts.stream()
                .map(contactMapper::toResponse)
                .collect(Collectors.toList());
    }

    // Добавить контакт
    public ContactResponseDto addContact(Long userId, ContactRequestDto request) {
        // Находим пользователя
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Если добавляется основной контакт, снимаем флаг с других
        if (request.isPrimary()) {
            contactRepository.unsetAllPrimary(userId);
        }

        // Создаем и сохраняем контакт
        ContactEntity contact = contactMapper.toEntity(request);
        contact.setOwner(user);

        ContactEntity savedContact = contactRepository.save(contact);
        log.info("Added new contact ID: {} for user ID: {}", savedContact.getId(), userId);

        return contactMapper.toResponse(savedContact);
    }

    // Обновить контакт
    public ContactResponseDto updateContact(Long userId, Long contactId, ContactRequestDto request) {
        // Находим контакт, принадлежащий пользователю
        ContactEntity contact = contactRepository.findByIdAndOwnerId(contactId, userId)
                .orElseThrow(() -> new ContactNotFoundException(
                        "Contact not found with id: " + contactId + " for user: " + userId));

        // Если устанавливаем основной контакт, снимаем флаг с других
        if (request.isPrimary() && !contact.isPrimary()) {
            contactRepository.unsetAllPrimary(userId);
        }

        // Обновляем контакт
        contactMapper.updateEntity(contact, request);
        ContactEntity updatedContact = contactRepository.save(contact);
        log.info("Updated contact ID: {} for user ID: {}", contactId, userId);

        return contactMapper.toResponse(updatedContact);
    }

    // Удалить контакт
    public void deleteContact(Long userId, Long contactId) {
        // Проверяем существование контакта
        if (!contactRepository.existsByIdAndOwnerId(contactId, userId)) {
            throw new ContactNotFoundException(
                    "Contact not found with id: " + contactId + " for user: " + userId);
        }

        contactRepository.deleteById(contactId);
        log.info("Deleted contact ID: {} for user ID: {}", contactId, userId);
    }

    // Установить основной контакт
    public void setPrimaryContact(Long userId, Long contactId) {
        // Сначала снимаем флаг со всех контактов
        contactRepository.unsetAllPrimary(userId);

        // Устанавливаем флаг для указанного контакта
        int updated = contactRepository.setPrimary(contactId, userId);

        if (updated == 0) {
            throw new ContactNotFoundException(
                    "Contact not found with id: " + contactId + " for user: " + userId);
        }

        log.info("Set contact ID: {} as primary for user ID: {}", contactId, userId);
    }

    // Получить основной контакт пользователя
    public Optional<ContactResponseDto> getPrimaryContact(Long userId) {
        return contactRepository.findByOwnerIdAndIsPrimaryTrue(userId)
                .map(contactMapper::toResponse);
    }
}
